/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasourcelocations.locations;

import static io.harness.idp.common.Constants.DSL_RESPONSE;
import static io.harness.idp.common.Constants.ERROR_MESSAGE_KEY;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.PAGERDUTY_ANNOTATION_MISSING_ERROR;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.PAGERDUTY_SERVICE_ID;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.common.GsonUtils;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;
import io.harness.idp.scorecard.datasourcelocations.beans.ApiRequestDetails;
import io.harness.idp.scorecard.datasourcelocations.client.DslClient;
import io.harness.idp.scorecard.datasourcelocations.client.DslClientFactory;
import io.harness.idp.scorecard.datasourcelocations.entity.DataSourceLocationEntity;
import io.harness.idp.scorecard.datasourcelocations.entity.HttpDataSourceLocationEntity;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@Slf4j
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class PagerDutyServiceDirectory implements DataSourceLocation {
  DslClientFactory dslClientFactory;
  private static final String ERROR_MESSAGE_FOR_INVALID_SERVICE_ID = "PagerDuty service id provided is invalid";
  @Override
  public Map<String, Object> fetchData(String accountIdentifier, BackstageCatalogEntity backstageCatalogEntity,
      DataSourceLocationEntity dataSourceLocationEntity, Map<DataPointEntity, Set<String>> dataPointsAndInputValues,
      Map<String, String> replaceableHeaders, Map<String, String> possibleReplaceableRequestBodyPairs,
      Map<String, String> possibleReplaceableUrlPairs) {
    ApiRequestDetails apiRequestDetails =
        ((HttpDataSourceLocationEntity) dataSourceLocationEntity).getApiRequestDetails();

    String apiUrl = apiRequestDetails.getUrl();
    log.info("PD Service Directory DSL -->  URL before replacements - {}", apiUrl);

    matchAndReplaceHeaders(apiRequestDetails.getHeaders(), replaceableHeaders);

    Map<String, Object> inputValueData = new HashMap<>();

    String serviceId = possibleReplaceableUrlPairs.get(PAGERDUTY_SERVICE_ID);
    if (serviceId == null) {
      log.info("PagerDutyServiceDirectory - pager duty annotation is missing");
      inputValueData.put(ERROR_MESSAGE_KEY, PAGERDUTY_ANNOTATION_MISSING_ERROR);
      return inputValueData;
    }

    apiUrl = replaceUrlsPlaceholdersIfAny(apiUrl, possibleReplaceableUrlPairs);

    log.info("PagerDutyServiceDirectory DSL, Replaced API URL - {} ", apiUrl);

    apiRequestDetails.setUrl(apiUrl);

    DslClient dslClient = dslClientFactory.getClient(accountIdentifier, null);
    Response response = getResponse(apiRequestDetails, dslClient, accountIdentifier);

    log.info("Response Status - {}", response.getStatus());
    log.info("Response Entity - {}", response.getEntity().toString());

    Map<String, Object> convertedResponse =
        GsonUtils.convertJsonStringToObject(response.getEntity().toString(), Map.class);
    if (response.getStatus() == 200) {
      inputValueData.put(DSL_RESPONSE, convertedResponse);
    } else if (response.getStatus() == 404) {
      inputValueData.put(ERROR_MESSAGE_KEY, ERROR_MESSAGE_FOR_INVALID_SERVICE_ID);
    } else {
      inputValueData.put(ERROR_MESSAGE_KEY, convertedResponse.get("message"));
    }
    log.info("PagerDutyServiceDirectory - Response status code - {} and returned response -{}", response.getStatus(),
        inputValueData);
    return inputValueData;
  }

  @Override
  public String replaceRequestBodyInputValuePlaceholdersIfAny(
      Map<String, String> dataPointIdsAndInputValues, String requestBody) {
    return null;
  }
}
