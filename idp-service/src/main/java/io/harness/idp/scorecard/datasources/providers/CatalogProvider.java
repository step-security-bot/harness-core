/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasources.providers;

import static io.harness.idp.common.Constants.CATALOG_IDENTIFIER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.scorecard.common.beans.DataSourceConfig;
import io.harness.idp.scorecard.datapoints.parser.factory.DataPointParserFactory;
import io.harness.idp.scorecard.datapoints.service.DataPointService;
import io.harness.idp.scorecard.datasourcelocations.locations.DataSourceLocationFactory;
import io.harness.idp.scorecard.datasourcelocations.repositories.DataSourceLocationRepository;
import io.harness.idp.scorecard.datasources.entity.DataSourceEntity;
import io.harness.idp.scorecard.datasources.repositories.DataSourceRepository;
import io.harness.idp.scorecard.scores.beans.DataFetchDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.IDP)
public class CatalogProvider extends NoopDataSourceProvider {
  protected CatalogProvider(DataPointService dataPointService, DataSourceLocationFactory dataSourceLocationFactory,
      DataSourceLocationRepository dataSourceLocationRepository, DataPointParserFactory dataPointParserFactory,
      DataSourceRepository dataSourceRepository) {
    super(CATALOG_IDENTIFIER, dataPointService, dataSourceLocationFactory, dataSourceLocationRepository,
        dataPointParserFactory, dataSourceRepository);
  }

  @Override
  public Map<String, Map<String, Object>> fetchData(String accountIdentifier, BackstageCatalogEntity entity,
      List<DataFetchDTO> dataPointsAndInputValues, String configs) {
    return processOut(accountIdentifier, CATALOG_IDENTIFIER, entity, getAuthHeaders(accountIdentifier, null),
        Collections.emptyMap(), Collections.emptyMap(), dataPointsAndInputValues);
  }

  @Override
  protected Map<String, String> prepareUrlReplaceablePairs(String... keysValues) {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, String> getAuthHeaders(String accountIdentifier, String configs) {
    return Collections.emptyMap();
  }

  @Override
  protected DataSourceConfig getDataSourceConfig(DataSourceEntity dataSourceEntity,
      Map<String, String> possibleReplaceableUrlPairs, Map<String, String> replaceableHeaders) {
    return null;
  }
}
