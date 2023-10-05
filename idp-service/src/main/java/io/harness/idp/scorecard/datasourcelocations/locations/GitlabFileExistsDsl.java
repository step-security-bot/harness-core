/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasourcelocations.locations;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.common.Constants.DSL_RESPONSE;
import static io.harness.idp.common.Constants.ERRORS;
import static io.harness.idp.common.Constants.ERROR_MESSAGE_KEY;
import static io.harness.idp.common.Constants.MESSAGE_KEY;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.IS_FILE_EXISTS;
import static io.harness.idp.scorecard.datapoints.constants.DataPoints.SOURCE_LOCATION_ANNOTATION_ERROR;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.PROJECT_PATH;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPO_SCM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.common.GsonUtils;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;
import io.harness.idp.scorecard.datasourcelocations.beans.ApiRequestDetails;
import io.harness.idp.scorecard.datasourcelocations.client.DslClient;
import io.harness.idp.scorecard.datasourcelocations.client.DslClientFactory;
import io.harness.idp.scorecard.datasourcelocations.entity.DataSourceLocationEntity;

import com.google.inject.Inject;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.IDP)
public class GitlabFileExistsDsl implements DataSourceLocation {
  DslClientFactory dslClientFactory;
  private static final String FILE_PATH_REPLACER = "{FILE_PATH_REPLACER}";
  @Override
  public Map<String, Object> fetchData(String accountIdentifier, BackstageCatalogEntity backstageCatalogEntity,
      DataSourceLocationEntity dataSourceLocationEntity, Map<DataPointEntity, Set<String>> dataPointsAndInputValues,
      Map<String, String> replaceableHeaders, Map<String, String> possibleReplaceableRequestBodyPairs,
      Map<String, String> possibleReplaceableUrlPairs) throws NoSuchAlgorithmException, KeyManagementException {
    ApiRequestDetails apiRequestDetails = fetchApiRequestDetails(dataSourceLocationEntity);
    matchAndReplaceHeaders(apiRequestDetails.getHeaders(), replaceableHeaders);
    apiRequestDetails.setUrl(replaceUrlsPlaceholdersIfAny(apiRequestDetails.getUrl(), possibleReplaceableUrlPairs));
    Map<String, Object> data = new HashMap<>();

    Optional<Map.Entry<DataPointEntity, Set<String>>> dataPointAndInputValuesOpt =
        dataPointsAndInputValues.entrySet()
            .stream()
            .filter(entry -> entry.getKey().getIdentifier().equals(IS_FILE_EXISTS))
            .findFirst();

    if (dataPointAndInputValuesOpt.isEmpty()) {
      return data;
    }
    DataPointEntity dataPoint = dataPointAndInputValuesOpt.get().getKey();
    Set<String> inputValues = dataPointAndInputValuesOpt.get().getValue();
    String tempRequestBody = apiRequestDetails.getRequestBody(); // using temp variable to store unchanged requestBody

    for (String inputValue : inputValues) {
      if (isEmpty(possibleReplaceableRequestBodyPairs.get(REPO_SCM))
          || isEmpty(possibleReplaceableRequestBodyPairs.get(PROJECT_PATH))) {
        data.put(inputValue, Map.of(ERROR_MESSAGE_KEY, SOURCE_LOCATION_ANNOTATION_ERROR));
        continue;
      }
      apiRequestDetails.setRequestBody(tempRequestBody);
      Map<DataPointEntity, String> dataPointAndInputValueToFetch = Map.of(dataPoint, inputValue);
      String requestBody =
          constructRequestBody(apiRequestDetails, possibleReplaceableRequestBodyPairs, dataPointAndInputValueToFetch);
      apiRequestDetails.setRequestBody(requestBody);
      DslClient dslClient =
          dslClientFactory.getClient(accountIdentifier, possibleReplaceableRequestBodyPairs.get(REPO_SCM));
      Response response = getResponse(apiRequestDetails, dslClient, accountIdentifier);
      Map<String, Object> inputValueData = new HashMap<>();
      if (response.getStatus() == 200) {
        inputValueData.put(
            DSL_RESPONSE, GsonUtils.convertJsonStringToObject(response.getEntity().toString(), Map.class));
      } else if (response.getStatus() == 500) {
        inputValueData.put(ERROR_MESSAGE_KEY, ((ResponseMessage) response.getEntity()).getMessage());
      } else {
        List<Map<String, Object>> errors =
            (List<Map<String, Object>>) GsonUtils.convertJsonStringToObject(response.getEntity().toString(), Map.class)
                .get(ERRORS);
        inputValueData.put(ERROR_MESSAGE_KEY, errors.get(0).get(MESSAGE_KEY));
      }
      data.put(inputValue, inputValueData);
    }
    return data;
  }

  @Override
  public String replaceInputValuePlaceholdersIfAny(Map<String, String> dataPointsAndInputValue, String requestBody) {
    if (!isEmpty(dataPointsAndInputValue.get(IS_FILE_EXISTS))) {
      String inputValue = dataPointsAndInputValue.get(IS_FILE_EXISTS);
      inputValue = inputValue.replace("\"", "");
      int lastSlash = inputValue.lastIndexOf("/");
      if (lastSlash != -1) {
        String path = inputValue.substring(0, lastSlash);
        requestBody = requestBody.replace(FILE_PATH_REPLACER, path);
      } else {
        requestBody = requestBody.replace(FILE_PATH_REPLACER, "");
      }
    }
    return requestBody;
  }
}
