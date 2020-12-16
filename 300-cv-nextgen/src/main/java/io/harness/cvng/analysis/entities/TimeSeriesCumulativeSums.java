package io.harness.cvng.analysis.entities;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@CdIndex(name = "service_gd_idx",
    fields = { @Field("cvConfigId")
               , @Field(value = "analysisEndTime", type = IndexType.DESC) })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TimeSeriesCumulativeSumsKeys")
@EqualsAndHashCode(callSuper = false, exclude = {"transactionMetricSums"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeseriesCumulativeSums", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesCumulativeSums implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @NotEmpty @FdIndex private String verificationTaskId;
  @NotEmpty @FdIndex private Instant analysisStartTime;
  @NotEmpty @FdIndex private Instant analysisEndTime;

  private List<TransactionMetricSums> transactionMetricSums;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TransactionMetricSumsKeys")
  public static class TransactionMetricSums {
    private String transactionName;
    private List<MetricSum> metricSums;
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "MetricSumsKeys")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MetricSum {
    private String metricName;
    private double risk;
    private double data;
  }

  public static List<TransactionMetricSums> convertMapToTransactionMetricSums(
      Map<String, Map<String, MetricSum>> txnMetricMap) {
    List<TransactionMetricSums> txnMetricSumList = new ArrayList<>();
    if (txnMetricMap == null) {
      return null;
    }
    txnMetricMap.forEach((txnName, metricMap) -> {
      if (isNotEmpty(metricMap)) {
        TransactionMetricSums txnMetricSums = TransactionMetricSums.builder().transactionName(txnName).build();
        List<MetricSum> metricSumsList = new ArrayList<>();
        metricMap.forEach((metricName, metricSums) -> {
          metricSums.setMetricName(metricName);
          metricSumsList.add(metricSums);
        });
        txnMetricSums.setMetricSums(metricSumsList);
        txnMetricSumList.add(txnMetricSums);
      }
    });
    return txnMetricSumList;
  }

  public static Map<String, Map<String, List<MetricSum>>> convertToMap(
      List<TimeSeriesCumulativeSums> timeSeriesCumulativeSumsList) {
    if (isEmpty(timeSeriesCumulativeSumsList)) {
      return new HashMap<>();
    }
    timeSeriesCumulativeSumsList.sort(Comparator.comparing(TimeSeriesCumulativeSums::getAnalysisStartTime));
    Map<String, Map<String, List<MetricSum>>> txnMetricMap = new HashMap<>();

    for (TimeSeriesCumulativeSums timeSeriesCumulativeSums : timeSeriesCumulativeSumsList) {
      if (isEmpty(timeSeriesCumulativeSums.getTransactionMetricSums())) {
        continue;
      }
      for (TransactionMetricSums transactionSum : timeSeriesCumulativeSums.getTransactionMetricSums()) {
        String transactionName = transactionSum.getTransactionName();
        if (!txnMetricMap.containsKey(transactionName)) {
          txnMetricMap.put(transactionName, new HashMap<>());
        }
        transactionSum.getMetricSums().forEach(metricSum -> {
          String metricName = metricSum.getMetricName();
          if (!txnMetricMap.get(transactionName).containsKey(metricName)) {
            txnMetricMap.get(transactionName).put(metricName, new ArrayList<>());
          }
          txnMetricMap.get(transactionName).get(metricName).add(metricSum);
        });
      }
    }
    return txnMetricMap;
  }
}
