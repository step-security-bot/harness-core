package io.harness.ccm.anomaly.graphql;

import com.google.cloud.Timestamp;
import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.anomaly.entities.AnomalyEntity.AnomaliesDataTableSchema;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingTimeFilter;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;

import java.util.Calendar;
import java.util.Date;

@Data
@Builder
@Slf4j
public class AnomaliesTimeFilter implements AnomaliesFilter {
  private QLTimeOperator operator;
  private AnomaliesDataTableSchema.fields variable;
  private Number value;

  @Override
  public Object getOperator() {
    return null;
  }

  @Override
  public Object[] getValues() {
    return new Object[0];
  }

  @Override
  public Condition toCondition() {
    Preconditions.checkNotNull(value, "The billing time filter is missing value.");
    Preconditions.checkNotNull(operator, "The billing time filter is missing operator");
    Timestamp timestamp = Timestamp.of(DateUtils.round(new Date(value.longValue()), Calendar.SECOND));

    DbColumn dbColumn = null;

    switch (variable) {
      case ANOMALY_TIME:
        dbColumn = AnomaliesDataTableSchema.anomalyTime;
        break;
      default:
        log.error("unsupported variable in anomaly time filterq");
        // throw new InvalidRequestException(invalidTimeFilter);
    }
    return getCondition(dbColumn, timestamp);
  }

  private Condition getCondition(DbColumn dbColumn, Timestamp timestamp) {
    switch (operator) {
      case AFTER:
        return BinaryCondition.greaterThanOrEq(dbColumn, timestamp);
      case BEFORE:
        return BinaryCondition.lessThanOrEq(dbColumn, timestamp);
      default:
        return null;
    }
  }

  public static AnomaliesTimeFilter convertFromCloudBillingFilter(CloudBillingTimeFilter filter) {
    AnomaliesTimeFilterBuilder filterBuilder = AnomaliesTimeFilter.builder();

    filterBuilder.operator(filter.getOperator());
    filterBuilder.value(filter.getValues()[0]);

    switch (filter.getVariable()) {
      case CloudBillingFilter.BILLING_GCP_PRODUCT:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.GCP_PRODUCT);
        break;
      case CloudBillingFilter.BILLING_GCP_PROJECT:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.GCP_PROJECT);
        break;
      case CloudBillingFilter.BILLING_GCP_SKU:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.GCP_SKU_ID);
        break;
      case CloudBillingFilter.BILLING_AWS_LINKED_ACCOUNT:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.AWS_ACCOUNT);
        break;
      case CloudBillingFilter.BILLING_AWS_SERVICE:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.AWS_SERVICE);
        break;
      default:
        log.error("CloudBillingFilter : {} not supported in AnomaliesIdFilter", filter.getVariable());
    }
    return filterBuilder.build();
  }
}
