package io.harness.ccm.remote.resources.policies;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.rbac.CCMRbacHelper;
import io.harness.ccm.utils.LogAccountIdentifier;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Api("policy")
@Path("policy")
@OwnedBy(CE)
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json"})
@Tag(name = "Policy", description = "This contains APIs related to Policy Management ")
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
//@NextGenManagerAuth
@PublicApi
public class GovernancePolicy {
  private final PolicyService policyService;
  private final CCMRbacHelper rbacHelper;
  @Inject
  public GovernancePolicy(PolicyService policyService, CCMRbacHelper rbacHelper) {
    this.policyService = policyService;
    this.rbacHelper = rbacHelper;
  }

  // Internal API for OOTB policy creation

  @POST
  //  @Hidden
  //  @InternalApi
  @Path("add")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Add a new policy internal api", nickname = "addPolicyNameInternal")
  @Operation(operationId = "addPolicyNameInternal", summary = "Add a new OOTB policy to be executed",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns newly created policy")
      })
  public ResponseDTO<Policy>
  create(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true,
          description = "Request body containing Policy store object") @Valid CreatePolicyDTO createPolicyDTO) {
   // rbacHelper.checkPolicyEditPermission(accountId, null, null);
    Policy policy = createPolicyDTO.getPolicy();
    policy.setAccountId(accountId);
    String uuid=policy.getUuid();
    if(policyService.listid(accountId, uuid)==null) {
      policyService.save(policy);
    }
    else
    {
      ResponseDTO res= ResponseDTO.newResponse(policy);
      res.setStatus(Status.ERROR);
      res.setMetaData("Policy already exists with this name");
      return res;
    }
    return ResponseDTO.newResponse(policy.toDTO());
  }

  // Update a policy already made

  @PUT
  //  @Hidden
  //  @InternalApi
  @Path("update")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a existing OOTB Policy", nickname = "updatePolicy")
  @LogAccountIdentifier
  @Operation(operationId = "updatePolicy", description = "Update a Policy", summary = "Update a Policy",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "update a existing OOTB Policy",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Policy>
  updatePolicy(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                   NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true,
          description = "Request body containing ceViewFolder object") @Valid CreatePolicyDTO createPolicyDTO) {
    //rbacHelper.checkPolicyEditPermission(accountId, null, null);
    Policy policy = createPolicyDTO.getPolicy();
    policy.toDTO();
    policy.setAccountId(accountId);
    policyService.update(policy);
    return ResponseDTO.newResponse(policy);
  }

  // Internal API for deletion of OOTB policies

  @DELETE
  //  @Hidden
  //  @InternalApi
  @Path("{policyId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete a policy", nickname = "deletePolicy")
  @LogAccountIdentifier
  @Operation(operationId = "deletePolicy", description = "Delete a Policy for the given a ID.",
      summary = "Delete a policy",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "A boolean whether the delete was successful or not",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Boolean>
  delete(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @PathParam("policyId") @Parameter(
          required = true, description = "Unique identifier for the policy") @NotNull @Valid String uuid) {
    //rbacHelper.checkPolicyDeletePermission(accountId, null, null);
    boolean result = policyService.delete(accountId, uuid);
    return ResponseDTO.newResponse(result);
  }

  // API to list all OOTB Policies

  @POST
  @Path("list")
  @ApiOperation(value = "Get OOTB policies for account", nickname = "getPolicies")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(operationId = "getPolicies", description = "Fetch policies ", summary = "Fetch policies for account",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description = "Returns List of policies", content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<List<Policy>>
  listPolicy(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                 NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(
          required = true, description = "Request body containing ceViewFolder object") @Valid ListDTO listDTO) {
    //rbacHelper.checkPolicyViewPermission(accountId, null, null);
    PolicyRequest query = listDTO.getPolicyRequest();
    List<Policy> Policies = new ArrayList<>();
    query.setAccountId(accountId);
    String uuid = query.getUuid();
    String isStablePolicy = query.getIsStablePolicy();
    String resource = query.getResource();
    String tags = query.getTags();
    if (uuid != null) {
      Policy policy = policyService.listid(accountId, uuid);
      Policies.add(policy);
      ResponseDTO res= ResponseDTO.newResponse(Policies);
      if(policyService.listid(accountId, uuid)==null)
      {
      res.setStatus(Status.ERROR);
      res.setMetaData("No such policy exists");
      }
      return res;
    }
    if (isStablePolicy != null) {
      Policies = policyService.findByStability(isStablePolicy, accountId);
      return ResponseDTO.newResponse(Policies);
    }
    if (resource != null && tags == null) {
      Policies = policyService.findByResource(resource, accountId);
      return ResponseDTO.newResponse(Policies);
    }
    if (resource == null && tags != null) {
      Policies = policyService.findByTag(tags, accountId);
      return ResponseDTO.newResponse(Policies);
    }
    if (resource != null && tags != null) {
      Policies = policyService.findByTagAndResource(resource, tags, accountId);
      return ResponseDTO.newResponse(Policies);
    }

    Policies = policyService.list(accountId);
    return ResponseDTO.newResponse(Policies);
  }
}
