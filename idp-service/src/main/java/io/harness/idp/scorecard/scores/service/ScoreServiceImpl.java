/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scores.service;

import static io.harness.idp.common.JacksonUtils.readValue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;
import io.harness.idp.scorecard.datapoints.repositories.DataPointsRepository;
import io.harness.idp.scorecard.datasourcelocations.entity.DataSourceLocationEntity;
import io.harness.idp.scorecard.datasourcelocations.repositories.DataSourceLocationRepository;
import io.harness.idp.scorecard.datasources.beans.entity.DataSourceEntity;
import io.harness.idp.scorecard.datasources.repositories.DataSourceRepository;
import io.harness.idp.scorecard.scorecardchecks.entity.CheckEntity;
import io.harness.idp.scorecard.scorecardchecks.repositories.CheckRepository;
import io.harness.idp.scorecard.scorecardchecks.service.ScorecardService;
import io.harness.idp.scorecard.scores.entities.ScoreEntity;
import io.harness.idp.scorecard.scores.mappers.ScorecardGraphSummaryInfoMapper;
import io.harness.idp.scorecard.scores.mappers.ScorecardScoreMapper;
import io.harness.idp.scorecard.scores.mappers.ScorecardSummaryInfoMapper;
import io.harness.idp.scorecard.scores.repositories.ScoreEntityByScorecardIdentifier;
import io.harness.idp.scorecard.scores.repositories.ScoreRepository;
import io.harness.spec.server.idp.v1.model.Scorecard;
import io.harness.spec.server.idp.v1.model.ScorecardDetails;
import io.harness.spec.server.idp.v1.model.ScorecardGraphSummaryInfo;
import io.harness.spec.server.idp.v1.model.ScorecardScore;
import io.harness.spec.server.idp.v1.model.ScorecardSummaryInfo;
import io.harness.springdata.TransactionHelper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @com.google.inject.Inject }))
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class ScoreServiceImpl implements ScoreService {
  @Inject TransactionHelper transactionHelper;
  @Inject CheckRepository checkRepository;
  @Inject DataPointsRepository datapointRepository;
  @Inject DataSourceRepository datasourceRepository;
  @Inject DataSourceLocationRepository datasourceLocationRepository;
  @Inject ScoreComputerService scoreComputerService;
  ScorecardService scorecardService;
  ScoreRepository scoreRepository;

  @Override
  public void populateData(
      String checkEntities, String datapointEntities, String datasourceEntities, String datasourceLocationEntities) {
    List<CheckEntity> checks = readValue(checkEntities, CheckEntity.class);
    List<DataPointEntity> dataPoints = readValue(datapointEntities, DataPointEntity.class);
    List<DataSourceEntity> dataSources = readValue(datasourceEntities, DataSourceEntity.class);
    List<DataSourceLocationEntity> dataSourceLocations =
        readValue(datasourceLocationEntities, DataSourceLocationEntity.class);
    log.info("Converted entities json string to corresponding list<> pojo's");
    saveAll(checks, dataPoints, dataSources, dataSourceLocations);
    log.info("Populated data into checks, dataPoints, dataSources, dataSourceLocations");
  }

  @Override
  public List<ScorecardSummaryInfo> getScoresSummaryForAnEntity(String accountIdentifier, String entityIdentifier) {
    Map<String, Scorecard> scorecardIdentifierEntityMapping =
        scorecardService.getAllScorecardsAndChecksDetails(accountIdentifier)
            .stream()
            .collect(Collectors.toMap(Scorecard::getIdentifier, Function.identity()));

    Map<String, ScoreEntity> lastComputedScoresForScorecards = getScoreEntityAndScoreCardIdentifierMapping(
        scoreRepository.getAllLatestScoresByScorecardsForAnEntity(accountIdentifier, entityIdentifier)
            .getMappedResults());

    // deleting scores for deleted scorecards
    deleteScoresForDeletedScoreCards(
        accountIdentifier, scorecardIdentifierEntityMapping, lastComputedScoresForScorecards);

    return lastComputedScoresForScorecards.keySet()
        .stream()
        .filter(scoreCardIdentifier -> scorecardIdentifierEntityMapping.get(scoreCardIdentifier).isPublished())
        .map(scoreCardIdentifier
            -> ScorecardSummaryInfoMapper.toDTO(lastComputedScoresForScorecards.get(scoreCardIdentifier),
                scorecardIdentifierEntityMapping.get(scoreCardIdentifier).getName(),
                scorecardIdentifierEntityMapping.get(scoreCardIdentifier).getDescription()))
        .collect(Collectors.toList());
  }

  @Override
  public List<ScorecardGraphSummaryInfo> getScoresGraphSummaryForAnEntityAndScorecard(
      String accountIdentifier, String entityIdentifier, String scorecardIdentifier) {
    List<ScoreEntity> scoreEntities =
        scoreRepository.findAllByAccountIdentifierAndEntityIdentifierAndScorecardIdentifier(
            accountIdentifier, entityIdentifier, scorecardIdentifier);
    return scoreEntities.stream().map(ScorecardGraphSummaryInfoMapper::toDTO).collect(Collectors.toList());
  }

  @Override
  public List<ScorecardScore> getScorecardScoreOverviewForAnEntity(String accountIdentifier, String entityIdentifier) {
    Map<String, Scorecard> scorecardIdentifierEntityMapping =
        scorecardService.getAllScorecardsAndChecksDetails(accountIdentifier)
            .stream()
            .collect(Collectors.toMap(Scorecard::getIdentifier, Function.identity()));

    Map<String, ScoreEntity> lastComputedScoresForScorecards = getScoreEntityAndScoreCardIdentifierMapping(
        scoreRepository.getAllLatestScoresByScorecardsForAnEntity(accountIdentifier, entityIdentifier)
            .getMappedResults());

    // deleting scores for deleted scorecards
    deleteScoresForDeletedScoreCards(
        accountIdentifier, scorecardIdentifierEntityMapping, lastComputedScoresForScorecards);

    return lastComputedScoresForScorecards.keySet()
        .stream()
        .filter(scorecardIdentifier -> scorecardIdentifierEntityMapping.get(scorecardIdentifier).isPublished())
        .map(scorecardIdentifier
            -> ScorecardScoreMapper.toDTO(lastComputedScoresForScorecards.get(scorecardIdentifier),
                scorecardIdentifierEntityMapping.get(scorecardIdentifier).getName(),
                scorecardIdentifierEntityMapping.get(scorecardIdentifier).getDescription()))
        .collect(Collectors.toList());
  }

  @Override
  public ScorecardSummaryInfo getScorecardRecalibratedScoreInfoForAnEntityAndScorecard(
      String accountIdentifier, String entityIdentifier, String scorecardIdentifier) {
    ScorecardDetails scorecardDetails = null;
    if (scorecardIdentifier != null) {
      scorecardDetails = scorecardService.getScorecardDetails(accountIdentifier, scorecardIdentifier).getScorecard();
      if (!scorecardDetails.isPublished()) {
        throw new UnsupportedOperationException(String.format(
            "Recalibrated scores will not be calculated for unpublished scorecard - %s for entity - %s in account - %s ",
            scorecardIdentifier, entityIdentifier, accountIdentifier));
      }
    }

    scoreComputerService.computeScores(accountIdentifier,
        scorecardIdentifier == null ? Collections.emptyList() : Collections.singletonList(scorecardIdentifier),
        entityIdentifier == null ? Collections.emptyList() : Collections.singletonList(entityIdentifier));

    if (scorecardIdentifier != null) {
      ScoreEntity latestComputedScoreForScorecard = null;
      if (entityIdentifier != null) {
        latestComputedScoreForScorecard = scoreRepository.getLatestComputedScoreForEntityAndScorecard(
            accountIdentifier, entityIdentifier, scorecardIdentifier);
      }
      return ScorecardSummaryInfoMapper.toDTO(
          latestComputedScoreForScorecard, scorecardDetails.getName(), scorecardDetails.getDescription());
    }
    return null;
  }

  private void saveAll(List<CheckEntity> checks, List<DataPointEntity> dataPoints, List<DataSourceEntity> dataSources,
      List<DataSourceLocationEntity> dataSourceLocations) {
    transactionHelper.performTransaction(() -> {
      checkRepository.saveAll(checks);
      datapointRepository.saveAll(dataPoints);
      datasourceRepository.saveAll(dataSources);
      datasourceLocationRepository.saveAll(dataSourceLocations);
      return null;
    });
  }

  private Map<String, ScoreEntity> getScoreEntityAndScoreCardIdentifierMapping(
      List<ScoreEntityByScorecardIdentifier> scoreEntityByScorecardIdentifierList) {
    return scoreEntityByScorecardIdentifierList.stream().collect(Collectors.toMap(
        ScoreEntityByScorecardIdentifier::getScorecardIdentifier, ScoreEntityByScorecardIdentifier::getScoreEntity));
  }

  private void deleteScoresForDeletedScoreCards(String accountIdentifier,
      Map<String, Scorecard> scorecardIdentifierMapping, Map<String, ScoreEntity> lastComputedScores) {
    List<String> scoreIdsToBeDeleted = new ArrayList<>();

    for (Map.Entry<String, ScoreEntity> lastComputedScore : lastComputedScores.entrySet()) {
      if (!scorecardIdentifierMapping.containsKey(lastComputedScore.getKey())) {
        scoreIdsToBeDeleted.add(lastComputedScore.getValue().getId());
      }
    }
    scoreRepository.deleteAllByAccountIdentifierAndIdIn(accountIdentifier, scoreIdsToBeDeleted);
  }
}
