/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.resources.governance;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.rbac.CCMRbacHelper;
import io.harness.ccm.views.dto.CreatePolicyExecutionDTO;
import io.harness.ccm.views.dto.CreatePolicyExecutionFilterDTO;
import io.harness.ccm.views.dto.CreatePolicyPackDTO;
import io.harness.ccm.views.entities.PolicyExecution;
import io.harness.ccm.views.entities.PolicyExecutionFilter;
import io.harness.ccm.views.service.PolicyExecutionService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CE;

@Api("governance")
@Path("governance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@NextGenManagerAuth
@PublicApi
@Service
@OwnedBy(CE)
@Slf4j
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })

public class GovernancePolicyExecutionResource {
  private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
  private static final String BINARY = "binary";
  private static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final String ATTACHMENT_FILENAME = "attachment; filename=";
  private static final String EXTENSION = ".json";
  private static final String RESOURCESFILENAME = "resources";
  public static final String GCP_CREDENTIALS_PATH = "GOOGLE_APPLICATION_CREDENTIALS";
  private final CCMRbacHelper rbacHelper;
  private final PolicyExecutionService policyExecutionService;
  @Inject CENextGenConfiguration configuration;
  @Inject private BigQueryService bigQueryService;

  @Inject
  public GovernancePolicyExecutionResource(CCMRbacHelper rbacHelper, PolicyExecutionService policyExecutionService) {
    this.rbacHelper = rbacHelper;
    this.policyExecutionService = policyExecutionService;
  }

  @POST
  @Hidden
  @Path("execution")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Add a new policy execution api", nickname = "addPolicyExecution")
  @Operation(operationId = "addPolicyExecution", summary = "Add a new policy execution ",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns newly created policy Execution")
      })
  public ResponseDTO<PolicyExecution>
  create(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true, description = "Request body containing Policy Execution store object")
      @Valid CreatePolicyExecutionDTO createPolicyExecutionDTO) {
    // rbacHelper.checkPolicyExecutionEditPermission(accountId, null, null);
    PolicyExecution policyExecution = createPolicyExecutionDTO.getPolicyExecution();
    policyExecution.setAccountId(accountId);
    policyExecutionService.save(policyExecution);
    return ResponseDTO.newResponse(policyExecution.toDTO());
  }

    @POST
    @Path("execution/list")
    @ApiOperation(value = "Get execution for account", nickname = "getPolicyExecution")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getPolicyExecution", description = "Fetch PolicyExecution ", summary = "Fetch PolicyExecution for account",
            responses =
                    {
                            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                                    description = "Returns List of PolicyExecution", content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
                    })
    public ResponseDTO<List<PolicyExecution>>
    filterPolicy(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
            NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
                 @RequestBody(required = true,
                           description = "Request body containing policy pack object") @Valid CreatePolicyExecutionFilterDTO createPolicyExecutionFilterDTO) {
        // rbacHelper.checkPolicyExecutionPermission(accountId, null, null);
        // TODO: Implement search support in this api
        PolicyExecutionFilter policyExecutionFilter= createPolicyExecutionFilterDTO.getPolicyExecutionFilter();
        return ResponseDTO.newResponse(policyExecutionService.filterExecution(policyExecutionFilter));
    }

  @GET
  @Path("execution/{policyExecutionId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Return logs for a policy execution", nickname = "getPolicyExecutionDetails")
  @Operation(operationId = "getPolicyExecutionDetails", summary = "Return logs for a policy execution ",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Return logs for a policy execution")
      })
  public Response
  getPolicyExecutionDetails(
      @Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @PathParam("policyExecutionId") @NotNull @Valid String policyExecutionId) {
    // Only resources.json file is of interest to us.
    log.info("policyExecutionId {}", policyExecutionId);
    PolicyExecution policyExecution = policyExecutionService.get(accountId, policyExecutionId);
    log.info("policyExecution from mongo {}", policyExecution);
    String[] path =
        "ccm-custodian-bucket/wFHXHD0RRQWoO8tIZT5YVw/ba68515791a54d5793e0a546473963c9/ec2-unoptimized-ebs/resources.json"
            .split("/");
    // policyExecution.getExecutionLogPath().split("/");
    String bucket = path[0];
    String[] objectPath = new String[path.length - 1];
    for (int i = 0; i < objectPath.length; i++) {
      objectPath[i] = path[1 + i];
    }
    String objectName = String.join("/", objectPath);
    log.info("objectName: {}, bucket: {}", objectName, bucket);
    // if (Objects.equals(policyExecution.getExecutionLogBucketType(), "GCS")) {
    // Other ways to return this file is by using a signed url concept.
    // https://cloud.google.com/storage/docs/access-control/signed-urls
    log.info("Fetching files from GCS");
    ServiceAccountCredentials credentials = bigQueryService.getCredentials(GCP_CREDENTIALS_PATH);
    log.info("configuration.getGcpConfig().getGcpProjectId(): {}", configuration.getGcpConfig().getGcpProjectId());
    Storage storage = StorageOptions.newBuilder()
                          .setProjectId(configuration.getGcpConfig().getGcpProjectId())
                          .setCredentials(credentials)
                          .build()
                          .getService();
    log.info("storage {}", storage);
    BlobId blobId = BlobId.of(bucket, objectName);
    log.info("blobId {}", blobId);
    Blob blob = storage.get(blobId);
    log.info("blob: {}", blob);
    return Response.ok(blob.getContent())
        .header(CONTENT_TRANSFER_ENCODING, BINARY)
        .type("text/plain; charset=UTF-8")
        .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + RESOURCESFILENAME + EXTENSION)
        .build();
    //    }
    //    return null;
  }
}
