/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.resources;

import io.harness.cvng.core.beans.healthsource.HealthSourceRecordsRequest;
import io.harness.cvng.core.beans.healthsource.HealthSourceRecordsResponse;
import io.harness.cvng.core.beans.healthsource.LogRecordsResponse;
import io.harness.cvng.core.beans.healthsource.MetricRecordsResponse;
import io.harness.cvng.core.beans.healthsource.QueryRecordsRequest;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.services.api.HealthSourceRawRecordService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.NonNull;
import retrofit2.http.Body;

@Api("health-source-records")
@Path(CVNextGenConstants.PROJECT_PATH)
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
public class HealthSourceRawRecordResource {
  @Inject private HealthSourceRawRecordService healthSourceService;

  @POST
  @Path("/healthSourceRecords")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Fetch health source raw records by submitting a query to the health source provider.",
      nickname = "getSampleRawRecord")
  public RestResponse<HealthSourceRecordsResponse>
  getSampleRawRecord(@PathParam(CVNextGenConstants.ACCOUNT_IDENTIFIER_KEY) @NonNull String accountIdentifier,
      @PathParam(CVNextGenConstants.ORG_IDENTIFIER_KEY) @NonNull String orgIdentifier,
      @PathParam(CVNextGenConstants.PROJECT_IDENTIFIER_KEY) @NonNull String projectIdentifier,
      @NotNull @Valid @Body HealthSourceRecordsRequest healthSourceRecordsRequest) {
    return new RestResponse<>(healthSourceService.fetchSampleRawRecordsForHealthSource(
        healthSourceRecordsRequest, accountIdentifier, orgIdentifier, projectIdentifier));
  }

  @POST
  @Path("/metricRecords")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Fetch metric records by submitting a query to the health source provider.",
      nickname = "getSampleMetricData")
  public RestResponse<MetricRecordsResponse>
  getSampleMetricData(@PathParam(CVNextGenConstants.ACCOUNT_IDENTIFIER_KEY) @NonNull String accountIdentifier,
      @PathParam(CVNextGenConstants.ORG_IDENTIFIER_KEY) @NonNull String orgIdentifier,
      @PathParam(CVNextGenConstants.PROJECT_IDENTIFIER_KEY) @NonNull String projectIdentifier,
      @NotNull @Valid @Body QueryRecordsRequest queryRecordsRequest) {
    return new RestResponse<>(
        healthSourceService.fetchMetricData(queryRecordsRequest, accountIdentifier, orgIdentifier, projectIdentifier));
  }

  @POST
  @Path("/logRecords")
  @Timed
  @ExceptionMetered
  @ApiOperation(
      value = "Fetch log records by submitting a query to the health source provider.", nickname = "getSampleLogData")
  public RestResponse<LogRecordsResponse>
  getSampleLogData(@PathParam(CVNextGenConstants.ACCOUNT_IDENTIFIER_KEY) @NonNull String accountIdentifier,
      @PathParam(CVNextGenConstants.ORG_IDENTIFIER_KEY) @NonNull String orgIdentifier,
      @PathParam(CVNextGenConstants.PROJECT_IDENTIFIER_KEY) @NonNull String projectIdentifier,
      @NotNull @Valid @Body QueryRecordsRequest queryRecordsRequest) {
    return new RestResponse<>(
        healthSourceService.fetchLogData(queryRecordsRequest, accountIdentifier, orgIdentifier, projectIdentifier));
  }
}