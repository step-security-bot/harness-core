package io.harness.cvng.beans;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CV)
public enum DataCollectionRequestType {
  SPLUNK_SAVED_SEARCHES,
  STACKDRIVER_DASHBOARD_LIST,
  STACKDRIVER_DASHBOARD_GET,
  STACKDRIVER_SAMPLE_DATA,
  APPDYNAMICS_FETCH_APPS,
  APPDYNAMICS_FETCH_TIERS,
  APPDYNAMICS_GET_METRIC_DATA,
  NEWRELIC_APPS_REQUEST,
  NEWRELIC_VALIDATION_REQUEST,
}
