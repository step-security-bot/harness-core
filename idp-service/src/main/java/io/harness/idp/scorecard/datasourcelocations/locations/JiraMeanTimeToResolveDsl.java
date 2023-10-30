/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasourcelocations.locations;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.common.Constants.DSL_RESPONSE;
import static io.harness.idp.common.Constants.ERROR_MESSAGES_KEY;
import static io.harness.idp.common.Constants.ERROR_MESSAGE_KEY;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.MEAN_TIME_TO_RESOLVE;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.PROJECT_KEY_ANNOTATION_ERROR;
import static io.harness.idp.scorecard.datapoints.constants.Inputs.JQL;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.API_BASE_URL;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.PROJECT_COMPONENT_REPLACER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.common.GsonUtils;
import io.harness.idp.scorecard.common.beans.DataSourceConfig;
import io.harness.idp.scorecard.common.beans.HttpConfig;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;
import io.harness.idp.scorecard.datasourcelocations.beans.ApiRequestDetails;
import io.harness.idp.scorecard.datasourcelocations.client.DslClient;
import io.harness.idp.scorecard.datasourcelocations.client.DslClientFactory;
import io.harness.idp.scorecard.datasourcelocations.entity.DataSourceLocationEntity;
import io.harness.spec.server.idp.v1.model.InputValue;

import com.google.inject.Inject;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.apache.xerces.util.URI;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class JiraMeanTimeToResolveDsl implements DataSourceLocation {
  DslClientFactory dslClientFactory;
  @Override
  public Map<String, Object> fetchData(String accountIdentifier, BackstageCatalogEntity backstageCatalogEntity,
      DataSourceLocationEntity dataSourceLocationEntity,
      List<Pair<DataPointEntity, List<InputValue>>> dataPointsAndInputValues, Map<String, String> replaceableHeaders,
      Map<String, String> possibleReplaceableRequestBodyPairs, Map<String, String> possibleReplaceableUrlPairs,
      DataSourceConfig dataSourceConfig) throws NoSuchAlgorithmException, KeyManagementException {
    ApiRequestDetails apiRequestDetails = fetchApiRequestDetails(dataSourceLocationEntity);
    matchAndReplaceHeaders(apiRequestDetails.getHeaders(), replaceableHeaders);
    HttpConfig httpConfig = (HttpConfig) dataSourceConfig;
    apiRequestDetails.getHeaders().putAll(httpConfig.getHeaders());
    apiRequestDetails.setUrl(
        constructUrl(httpConfig.getTarget(), apiRequestDetails.getUrl(), possibleReplaceableUrlPairs));
    Map<String, Object> data = new HashMap<>();
    String tempRequestBody = apiRequestDetails.getRequestBody(); // using temp variable to store unchanged requestBody

    for (Pair<DataPointEntity, List<InputValue>> dataPointAndInputValues : dataPointsAndInputValues) {
      DataPointEntity dataPoint = dataPointAndInputValues.getFirst();
      List<InputValue> inputValues = dataPointAndInputValues.getSecond();

      if (isEmpty(possibleReplaceableRequestBodyPairs.get(PROJECT_COMPONENT_REPLACER))) {
        addInputValueResponse(data, inputValues, Map.of(ERROR_MESSAGE_KEY, PROJECT_KEY_ANNOTATION_ERROR));
        continue;
      }

      apiRequestDetails.setRequestBody(tempRequestBody);
      String requestBody =
          constructRequestBody(apiRequestDetails, possibleReplaceableRequestBodyPairs, dataPoint, inputValues);
      apiRequestDetails.setRequestBody(requestBody);
      URI uri;
      try {
        uri = new URI(possibleReplaceableUrlPairs.get(API_BASE_URL));
      } catch (URI.MalformedURIException e) {
        log.warn("Url is malformed: {}", possibleReplaceableUrlPairs.get(API_BASE_URL), e);
        continue;
      }
      DslClient dslClient = dslClientFactory.getClient(accountIdentifier, uri.getHost());
      Response response = getResponse(apiRequestDetails, dslClient, accountIdentifier);
      Map<String, Object> inputValueData = new HashMap<>();
      if (response.getStatus() == 200) {
        inputValueData.put(
            DSL_RESPONSE, GsonUtils.convertJsonStringToObject(response.getEntity().toString(), Map.class));
      } else if (response.getStatus() == 500) {
        inputValueData.put(ERROR_MESSAGE_KEY, ((ResponseMessage) response.getEntity()).getMessage());
      } else {
        inputValueData.put(ERROR_MESSAGE_KEY,
            GsonUtils.convertJsonStringToObject(response.getEntity().toString(), Map.class)
                .get(ERROR_MESSAGES_KEY)
                .toString());
      }
      addInputValueResponse(data, inputValues, inputValueData);
    }

    return data;
  }

  @Override
  public String replaceInputValuePlaceholdersIfAny(
      String requestBody, DataPointEntity dataPoint, List<InputValue> inputValues) {
    if (dataPoint.getIdentifier().equals(MEAN_TIME_TO_RESOLVE)) {
      Optional<InputValue> inputValueOpt =
          inputValues.stream().filter(inputValue -> inputValue.getKey().equals(JQL)).findFirst();
      if (inputValueOpt.isPresent()) {
        String inputValue = inputValueOpt.get().getValue();
        if (!inputValue.isEmpty()) {
          inputValue = inputValue.replaceFirst("\"", "");
          inputValue = inputValue.substring(0, inputValue.length() - 1);
          requestBody = requestBody.replace("{JQL_EXPRESSION}", inputValue);
        }
      }
    }
    return requestBody;
  }
}
