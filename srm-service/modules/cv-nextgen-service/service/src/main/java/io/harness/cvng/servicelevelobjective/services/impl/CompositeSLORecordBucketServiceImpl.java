/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.SLI_RECORD_BUCKET_SIZE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthorityCount;

import io.harness.SRMPersistence;
import io.harness.annotations.retry.RetryOnException;
import io.harness.cvng.servicelevelobjective.beans.CompositeSLOFormulaType;
import io.harness.cvng.servicelevelobjective.beans.SLIEvaluationType;
import io.harness.cvng.servicelevelobjective.beans.SLIMissingDataType;
import io.harness.cvng.servicelevelobjective.beans.slospec.CompositeSLOEvaluator;
import io.harness.cvng.servicelevelobjective.entities.CompositeSLORecordBucket;
import io.harness.cvng.servicelevelobjective.entities.CompositeServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.SLIRecordBucket;
import io.harness.cvng.servicelevelobjective.entities.SLIState;
import io.harness.cvng.servicelevelobjective.services.api.CompositeSLORecordBucketService;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordBucketService;
import io.harness.cvng.servicelevelobjective.services.api.SLOHealthIndicatorService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveV2Service;
import io.harness.exception.InvalidArgumentsException;

import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Inject;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;

@Slf4j
public class CompositeSLORecordBucketServiceImpl implements CompositeSLORecordBucketService {
  private static final int RETRY_COUNT = 3;
  @Inject private SRMPersistence hPersistence;
  @Inject ServiceLevelObjectiveV2Service serviceLevelObjectiveV2Service;
  @Inject SLIRecordBucketService sliRecordBucketService;
  @Inject SLOHealthIndicatorService sloHealthIndicatorService;

  @Inject Map<CompositeSLOFormulaType, CompositeSLOEvaluator> formulaTypeCompositeSLOEvaluatorMap;
  @Override
  public void create(CompositeServiceLevelObjective compositeServiceLevelObjective, Instant startTime, Instant endTime,
      String verificationTaskId) {
    Pair<Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>,
        Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>>
        sloDetailsSLIRecordsAndSLIMissingDataType = sliRecordBucketService.getSLODetailsSLIRecordsAndSLIMissingDataType(
            compositeServiceLevelObjective.getServiceLevelObjectivesDetails(), startTime, endTime);
    if (sloDetailsSLIRecordsAndSLIMissingDataType.getKey().size()
        == compositeServiceLevelObjective.getServiceLevelObjectivesDetails()
               .size()) { // count of simple slo's in the composite slo
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap = sloDetailsSLIRecordsAndSLIMissingDataType.getKey();
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap =
              sloDetailsSLIRecordsAndSLIMissingDataType
                  .getValue(); // this map holds config for how to treat missing data for any simple slo
      String compositeSLOId = compositeServiceLevelObjective.getUuid();
      int sloVersion = compositeServiceLevelObjective.getVersion();
      SLIEvaluationType sliEvaluationType = compositeServiceLevelObjective.getSliEvaluationType();
      if (isEmpty(
              serviceLevelObjectivesDetailCompositeSLORecordMap)) { // we dont have any actual records , SLIRecordBucket
        return;
      }
      double runningGoodCount = 0;
      double runningBadCount = 0;
      // check if its a version upgrade/new and we create from beginning or its next iteration
      CompositeSLORecordBucket lastCompositeSLORecord = getLastCompositeSLORecordBucket(compositeSLOId, startTime);
      CompositeSLORecordBucket latestCompositeSLORecord =
          getLatestCompositeSLORecordBucket(compositeSLOId); // only date of the latest is used to check.
      if (Objects.nonNull(lastCompositeSLORecord)) {
        runningGoodCount = lastCompositeSLORecord.getRunningGoodCount();
        runningBadCount = lastCompositeSLORecord.getRunningBadCount();
      }
      // new data has arrived for this composite SLO
      if (Objects.nonNull(latestCompositeSLORecord)
          && latestCompositeSLORecord.getStartTimestamp()
                 .plus(SLI_RECORD_BUCKET_SIZE, ChronoUnit.MINUTES)
                 .isAfter(startTime)) { // TODO recheck this condition
        // Update flow: fetch CompositeSLO Records to be updated
        updateCompositeSLORecords(serviceLevelObjectivesDetailCompositeSLORecordMap,
            objectivesDetailSLIMissingDataTypeMap, compositeServiceLevelObjective, runningGoodCount, runningBadCount,
            compositeSLOId, startTime, endTime, sliEvaluationType);
      } else {
        List<CompositeSLORecordBucket> compositeSLORecords =
            getCompositeSLORecordsFromSLIsDetails(serviceLevelObjectivesDetailCompositeSLORecordMap,
                objectivesDetailSLIMissingDataTypeMap, sloVersion, runningGoodCount, runningBadCount, compositeSLOId,
                sliEvaluationType, compositeServiceLevelObjective.getCompositeSLOFormulaType());
        hPersistence.saveBatch(compositeSLORecords);
      }
      sloHealthIndicatorService.upsert(compositeServiceLevelObjective);
    }
  }

  @Override
  public CompositeSLORecordBucket getLatestCompositeSLORecordBucket(String sloId) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloId, sloId)
        .order(Sort.descending(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp))
        .get();
  }

  @Override
  public CompositeSLORecordBucket getLatestCompositeSLORecordBucketWithVersion(
      String sloId, Instant startTimeForCurrentRange, int sloVersion) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloId, sloId)
        .field(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp)
        .greaterThanOrEq(startTimeForCurrentRange)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloVersion, sloVersion)
        .order(Sort.descending(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp))
        .get();
  }

  @Override
  public CompositeSLORecordBucket getFirstCompositeSLORecordBucket(String sloId, Instant startTimeStamp) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloId, sloId)
        .field(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp)
        .greaterThanOrEq(startTimeStamp)
        .order(Sort.ascending(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp))
        .get();
  }

  @Override
  public CompositeSLORecordBucket getLastCompositeSLORecordBucket(String sloId, Instant startTimeStamp) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloId, sloId)
        .field(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp)
        .lessThan(startTimeStamp)
        .order(Sort.descending(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp))
        .get();
  }

  @Override
  public List<CompositeSLORecordBucket> getSLORecordBuckets(
      String sloId, Instant startTimeStamp, Instant endTimeStamp) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloId, sloId)
        .field(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp)
        .greaterThanOrEq(startTimeStamp)
        .field(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp)
        .lessThan(endTimeStamp)
        .order(Sort.ascending(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp))
        .asList();
  }

  @Override
  public List<CompositeSLORecordBucket> getLatestCountSLORecords(String sloId, int count) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloId, sloId)
        .order(Sort.descending(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp))
        .asList(new FindOptions().limit(count));
  }

  @Override
  public List<CompositeSLORecordBucket> getSLORecordsOfMinutes(String sloId, List<Instant> minutes) {
    return hPersistence.createQuery(CompositeSLORecordBucket.class, excludeAuthorityCount)
        .filter(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.sloId, sloId)
        .field(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp)
        .in(minutes)
        .order(Sort.ascending(CompositeSLORecordBucket.CompositeSLORecordBucketKeys.startTimestamp))
        .asList();
  }

  @RetryOnException(retryCount = RETRY_COUNT, retryOn = ConcurrentModificationException.class)
  public void updateCompositeSLORecords(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap,
      CompositeServiceLevelObjective serviceLevelObjective, double runningGoodCount, double runningBadCount,
      String verificationTaskId, Instant startTime, Instant endTime, SLIEvaluationType sliEvaluationType) {
    // Put check as mutiple of BucketSize TODO
    List<CompositeSLORecordBucket> toBeUpdatedSLORecordBuckets =
        getSLORecordBuckets(verificationTaskId, startTime, endTime.plus(1, ChronoUnit.MINUTES));
    Map<Instant, CompositeSLORecordBucket> sloRecordBucketMap =
        toBeUpdatedSLORecordBuckets.stream().collect(Collectors.toMap(
            CompositeSLORecordBucket::getStartTimestamp, Function.identity(), (sloRecordBucket1, sloRecordBucket2) -> {
              log.info("Duplicate SLO Key detected sloId: {}, timeStamp: {}", serviceLevelObjective.getUuid(),
                  sloRecordBucket1.getStartTimestamp());
              return sloRecordBucket1.getLastUpdatedAt() > sloRecordBucket2.getLastUpdatedAt() ? sloRecordBucket1
                                                                                               : sloRecordBucket2;
            }));
    List<CompositeSLORecordBucket> updateOrCreateSLORecordBuckets;
    if (sliEvaluationType == SLIEvaluationType.WINDOW) {
      updateOrCreateSLORecordBuckets = updateWindowCompositeSLORecordBuckets(
          serviceLevelObjectivesDetailCompositeSLORecordMap, objectivesDetailSLIMissingDataTypeMap,
          serviceLevelObjective.getVersion(), runningGoodCount, runningBadCount, verificationTaskId, sloRecordBucketMap,
          serviceLevelObjective.getCompositeSLOFormulaType());
    } else if (sliEvaluationType == SLIEvaluationType.REQUEST) {
      updateOrCreateSLORecordBuckets = updateRequestCompositeSLORecordBuckets(
          serviceLevelObjectivesDetailCompositeSLORecordMap, serviceLevelObjective.getVersion(), runningGoodCount,
          runningBadCount, verificationTaskId, sloRecordBucketMap);
    } else {
      throw new InvalidArgumentsException("Invalid Evaluation Type");
    }

    try {
      hPersistence.upsertBatch(CompositeSLORecordBucket.class, updateOrCreateSLORecordBuckets, new ArrayList<>());
    } catch (IllegalAccessException exception) {
      log.error("SLO Records update failed through Bulk update {}", exception.getLocalizedMessage());
      hPersistence.save(updateOrCreateSLORecordBuckets);
    }
  }

  public List<CompositeSLORecordBucket> getCompositeSLORecordsFromSLIsDetails(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap,
      int sloVersion, double runningGoodCount, double runningBadCount, String verificationTaskId,
      SLIEvaluationType sliEvaluationType, CompositeSLOFormulaType compositeSLOFormulaType) {
    if (sliEvaluationType == SLIEvaluationType.REQUEST) {
      return getRequestCompositeSLORecordsFromSLIsDetails(serviceLevelObjectivesDetailCompositeSLORecordMap, sloVersion,
          runningGoodCount, runningBadCount, verificationTaskId);
    } else {
      return getWindowCompositeSLORecordsFromSLIsDetails(serviceLevelObjectivesDetailCompositeSLORecordMap,
          objectivesDetailSLIMissingDataTypeMap, sloVersion, runningGoodCount, runningBadCount, verificationTaskId,
          compositeSLOFormulaType);
    }
  }

  private List<CompositeSLORecordBucket> getWindowCompositeSLORecordsFromSLIsDetails(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap,
      int sloVersion, double runningGoodCount, double runningBadCount, String verificationTaskId,
      CompositeSLOFormulaType sloFormulaType) {
    Map<Instant, Pair<List<Double>, List<Integer>>> timeStampToBadValue =
        new HashMap<>(); // the list contains for each of the simple SLOs
    Map<Instant, Pair<List<Double>, List<Integer>>> timeStampToGoodValue = new HashMap<>();
    Map<Instant, Integer> timeStampTotalWeightage = new HashMap<>();
    getTimeStampMapsForGoodBadTotal(serviceLevelObjectivesDetailCompositeSLORecordMap,
        objectivesDetailSLIMissingDataTypeMap, timeStampToBadValue, timeStampToGoodValue, timeStampTotalWeightage);
    List<CompositeSLORecordBucket> sloRecordList = new ArrayList<>();
    int idx = 0;
    for (Instant instant : ImmutableSortedSet.copyOf(timeStampTotalWeightage.keySet())) {
      // we are checking, if we have all the simple slo data
      // TODO we need to check its always for BUCKET_SIZE all edge cases
      if (timeStampTotalWeightage.get(instant).equals(
              serviceLevelObjectivesDetailCompositeSLORecordMap.size())) { // Check if this is correct TODO
        Pair<Double, Double> currentCount =
            formulaTypeCompositeSLOEvaluatorMap.get(sloFormulaType)
                .evaluate(timeStampToGoodValue.get(instant).getFirst(), timeStampToGoodValue.get(instant).getSecond(),
                    timeStampToBadValue.get(instant).getSecond());
        double currentGoodCount = currentCount.getFirst();
        double currentBadCount = currentCount.getSecond();
        runningGoodCount += currentGoodCount;
        runningBadCount += currentBadCount;
        if ((idx + 1) % SLI_RECORD_BUCKET_SIZE == 0) {
          CompositeSLORecordBucket sloRecordBucket =
              CompositeSLORecordBucket.builder()
                  .runningBadCount(runningBadCount)
                  .runningGoodCount(runningGoodCount)
                  .sloId(verificationTaskId)
                  .sloVersion(sloVersion)
                  .verificationTaskId(verificationTaskId)
                  .startTimestamp(instant.minus(4, ChronoUnit.MINUTES)) // TODO date check
                  .build();
          sloRecordList.add(sloRecordBucket);
        }
      }
      idx++;
    }
    return sloRecordList;
  }

  private List<CompositeSLORecordBucket> getRequestCompositeSLORecordsFromSLIsDetails(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      int sloVersion, double runningGoodCount, double runningBadCount, String verificationTaskId) {
    // TODO need condition here too
    List<CompositeSLORecordBucket> sloRecordList = new ArrayList<>();
    Map<Instant, Map<String, SLIRecordBucket>> timeStampToSLIRecordBucketMap =
        getTimeStampToSLIRecordBucketMap(serviceLevelObjectivesDetailCompositeSLORecordMap);
    for (Instant instant : timeStampToSLIRecordBucketMap.keySet()) {
      if (timeStampToSLIRecordBucketMap.get(instant).size()
          == serviceLevelObjectivesDetailCompositeSLORecordMap.size()) {
        CompositeSLORecordBucket sloRecord =
            CompositeSLORecordBucket.builder()
                .runningBadCount(runningBadCount)
                .runningGoodCount(runningGoodCount)
                .sloId(verificationTaskId)
                .sloVersion(sloVersion)
                .verificationTaskId(verificationTaskId)
                .startTimestamp(instant)
                .scopedIdentifierSLIRecordBucketMap(timeStampToSLIRecordBucketMap.get(instant))
                .build();
        sloRecordList.add(sloRecord);
      }
    }
    return sloRecordList;
  }

  private void getTimeStampMapsForGoodBadTotal(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap, // we are updating the three maps
      Map<Instant, Pair<List<Double>, List<Integer>>> timeStampToBadValue, // status of SLO in that min
      Map<Instant, Pair<List<Double>, List<Integer>>> timeStampToGoodValue, // status of SLO in that min
      Map<Instant, Integer> timeStampToTotalValue) {
    for (Map.Entry<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
             objectivesDetailListEntry : serviceLevelObjectivesDetailCompositeSLORecordMap.entrySet()) {
      CompositeServiceLevelObjective.ServiceLevelObjectivesDetail objectivesDetail = objectivesDetailListEntry.getKey();
      for (SLIRecordBucket sliRecordBucket :
          objectivesDetailListEntry.getValue()) { // Iterate for the particular simple SLO
        for (int i = 0; i < sliRecordBucket.getSliStates().size(); i++) {
          SLIState sliState = sliRecordBucket.getSliStates().get(i);
          Instant currentTime = sliRecordBucket.getBucketStartTime().plus(i, ChronoUnit.MINUTES);
          Pair<List<Double>, List<Integer>> badCountPair = timeStampToBadValue.getOrDefault(currentTime,
              new Pair<>(new ArrayList<>(), new ArrayList<>())); // list of weights + is it of good/bad for simple SLO
          Pair<List<Double>, List<Integer>> goodCountPair =
              timeStampToGoodValue.getOrDefault(currentTime, new Pair<>(new ArrayList<>(), new ArrayList<>()));
          timeStampToBadValue.put(currentTime, badCountPair);
          timeStampToGoodValue.put(currentTime, goodCountPair);
          timeStampToTotalValue.put(currentTime, timeStampToTotalValue.getOrDefault(currentTime, 0) + 1);
          if (SLIState.GOOD.equals(sliState)
              || (SLIState.NO_DATA.equals(sliState)
                  && objectivesDetailSLIMissingDataTypeMap.get(objectivesDetail).equals(SLIMissingDataType.GOOD))) {
            badCountPair.getFirst().add(objectivesDetail.getWeightagePercentage());
            badCountPair.getSecond().add(0);
            goodCountPair.getFirst().add(objectivesDetail.getWeightagePercentage());
            goodCountPair.getSecond().add(1);
          } else if (SLIState.BAD.equals(sliState)
              || (SLIState.NO_DATA.equals(sliState)
                  && objectivesDetailSLIMissingDataTypeMap.get(objectivesDetail).equals(SLIMissingDataType.BAD))) {
            badCountPair.getFirst().add(objectivesDetail.getWeightagePercentage());
            badCountPair.getSecond().add(1);
            goodCountPair.getFirst().add(objectivesDetail.getWeightagePercentage());
            goodCountPair.getSecond().add(0);
          } else {
            badCountPair.getFirst().add(objectivesDetail.getWeightagePercentage());
            badCountPair.getSecond().add(-1);
            goodCountPair.getFirst().add(objectivesDetail.getWeightagePercentage());
            goodCountPair.getSecond().add(-1);
          }
        }
      }
    }
  }

  private Map<Instant, Map<String, SLIRecordBucket>> getTimeStampToSLIRecordBucketMap(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap) {
    Map<Instant, Map<String, SLIRecordBucket>> timeStampToSLIRecordsMap = new HashMap<>();
    for (CompositeServiceLevelObjective.ServiceLevelObjectivesDetail objectivesDetail :
        serviceLevelObjectivesDetailCompositeSLORecordMap.keySet()) { // iterate for each of simple SLO
      for (SLIRecordBucket sliRecordBucket :
          serviceLevelObjectivesDetailCompositeSLORecordMap.get(objectivesDetail)) { // iterate for each of the buckets
        Map<String, SLIRecordBucket> serviceLevelObjectivesDetailSLIRecordMap = timeStampToSLIRecordsMap.getOrDefault(
            sliRecordBucket.getBucketStartTime().plus(4, ChronoUnit.MINUTES), new HashMap<>());
        // fix with contains etc TODO
        // TODO this needs fixing
        /*        for (SLIState sliState : sliRecordBucket.getSliStates()) {
                  if (sliState != SLIState.SKIP_DATA) {*/
        serviceLevelObjectivesDetailSLIRecordMap.put(
            serviceLevelObjectiveV2Service.getScopedIdentifier(objectivesDetail), sliRecordBucket);
        //          }
        timeStampToSLIRecordsMap.put(
            sliRecordBucket.getBucketStartTime().plus(4, ChronoUnit.MINUTES), serviceLevelObjectivesDetailSLIRecordMap);
        //        }
      }
    }
    return timeStampToSLIRecordsMap;
  }

  private List<CompositeSLORecordBucket> updateWindowCompositeSLORecordBuckets(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, SLIMissingDataType>
          objectivesDetailSLIMissingDataTypeMap,
      int sloVersion, double previousRunningGoodCount, double previousRunningBadCount, String verificationTaskId,
      Map<Instant, CompositeSLORecordBucket> sloRecordBucketMap, CompositeSLOFormulaType sloFormulaType) {
    List<CompositeSLORecordBucket> updateOrCreateSLORecords = new ArrayList<>();
    Map<Instant, Pair<List<Double>, List<Integer>>> timeStampToBadValue = new HashMap<>(); // weight with current total
    Map<Instant, Pair<List<Double>, List<Integer>>> timeStampToGoodValue = new HashMap<>();
    Map<Instant, Integer> timeStampToTotalValue = new HashMap<>();
    getTimeStampMapsForGoodBadTotal(serviceLevelObjectivesDetailCompositeSLORecordMap,
        objectivesDetailSLIMissingDataTypeMap, timeStampToBadValue, timeStampToGoodValue, timeStampToTotalValue);
    int minute = 0;
    for (Instant instant :
        ImmutableSortedSet.copyOf(timeStampToTotalValue.keySet())) { // maybe we need a better iteration
      if (timeStampToTotalValue.get(instant).equals(serviceLevelObjectivesDetailCompositeSLORecordMap.size())) {
        Pair<Double, Double> currentCount =
            formulaTypeCompositeSLOEvaluatorMap.get(sloFormulaType)
                .evaluate(timeStampToGoodValue.get(instant).getFirst(), timeStampToGoodValue.get(instant).getSecond(),
                    timeStampToBadValue.get(instant).getSecond());
        double currentGoodCount = currentCount.getFirst();
        double currentBadCount = currentCount.getSecond();
        previousRunningGoodCount += currentGoodCount;
        previousRunningBadCount += currentBadCount;
      }
      if ((minute + 1) % SLI_RECORD_BUCKET_SIZE == 0) {
        CompositeSLORecordBucket sloRecordBucket =
            sloRecordBucketMap.get(instant.minus(4, ChronoUnit.MINUTES)); // map is of bucket startTime
        if (Objects.nonNull(sloRecordBucket)) {
          sloRecordBucket.setRunningGoodCount(previousRunningGoodCount);
          sloRecordBucket.setRunningBadCount(previousRunningBadCount);
          sloRecordBucket.setSloVersion(sloVersion);
        } else {
          sloRecordBucket = CompositeSLORecordBucket.builder()
                                .runningBadCount(previousRunningBadCount)
                                .runningGoodCount(previousRunningGoodCount)
                                .sloId(verificationTaskId)
                                .sloVersion(sloVersion)
                                .verificationTaskId(verificationTaskId)
                                .startTimestamp(instant)
                                .build();
        }
        updateOrCreateSLORecords.add(sloRecordBucket);
      }
      minute++;
    }
    return updateOrCreateSLORecords;
  }

  private List<CompositeSLORecordBucket> updateRequestCompositeSLORecordBuckets(
      Map<CompositeServiceLevelObjective.ServiceLevelObjectivesDetail, List<SLIRecordBucket>>
          serviceLevelObjectivesDetailCompositeSLORecordMap,
      int sloVersion, double runningGoodCount, double runningBadCount, String verificationTaskId,
      Map<Instant, CompositeSLORecordBucket> sloRecordMap) { // sloRecordMap is the current value and we are updating
                                                             // based on new entries, it can be a version upgrade
    List<CompositeSLORecordBucket> updateOrCreateSLORecords = new ArrayList<>();
    // A Map of  instant and simple slo id  and its bucket
    Map<Instant, Map<String, SLIRecordBucket>> timeStampToSLIRecordBucketsMap =
        getTimeStampToSLIRecordBucketMap(serviceLevelObjectivesDetailCompositeSLORecordMap);
    CompositeSLORecordBucket compositeSLORecordBucket = null;
    for (Instant instant : timeStampToSLIRecordBucketsMap.keySet()) {
      if (timeStampToSLIRecordBucketsMap.get(instant).size()
          == serviceLevelObjectivesDetailCompositeSLORecordMap.size()) {
        compositeSLORecordBucket = sloRecordMap.get(instant);
      }
      if (Objects.nonNull(compositeSLORecordBucket)) {
        compositeSLORecordBucket.setRunningGoodCount(runningGoodCount);
        compositeSLORecordBucket.setRunningBadCount(runningBadCount);
        compositeSLORecordBucket.setSloVersion(sloVersion);
        compositeSLORecordBucket.setScopedIdentifierSLIRecordBucketMap(timeStampToSLIRecordBucketsMap.get(instant));
      } else {
        compositeSLORecordBucket = CompositeSLORecordBucket.builder()
                                       .runningBadCount(runningBadCount)
                                       .runningGoodCount(runningGoodCount)
                                       .sloId(verificationTaskId)
                                       .sloVersion(sloVersion)
                                       .verificationTaskId(verificationTaskId)
                                       .startTimestamp(instant)
                                       .scopedIdentifierSLIRecordBucketMap(timeStampToSLIRecordBucketsMap.get(instant))
                                       .build();
      }
      updateOrCreateSLORecords.add(compositeSLORecordBucket);
    }
    return updateOrCreateSLORecords;
  }
}
