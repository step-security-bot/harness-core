package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.service.intf.CEYamlService.CLOUD_COST_K8S_CLUSTER_SETUP;
import static io.harness.ccm.service.intf.CEYamlService.DOT_YAML;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.config.CEFeatures;
import io.harness.ccm.remote.beans.K8sClusterSetupRequest;
import io.harness.ccm.service.intf.CEYamlService;
import io.harness.exception.DelegateNotAvailableException;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Api("yaml")
@Path("/yaml")
@Produces({MediaType.APPLICATION_JSON})
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class CEYamlResource {
  @Inject private CEYamlService ceYamlService;

  private static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final String ATTACHMENT_FILENAME = "attachment; filename=";
  private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
  private static final String BINARY = "binary";

  // @deprecated - use 'io.harness.ccm.remote.resources.CEYamlResource.cloudCostK8sClusterSetup'
  @POST
  @Path("/generate-cost-optimisation-yaml")
  @ApiOperation(value = "Get Cost Optimisation Yaml", nickname = "getCostOptimisationYamlTemplate")
  @Deprecated
  public Response generateCostOptimisationYaml(@Context HttpServletRequest request,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier) throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      String serverName = request.getServerName();
      String harnessHost = request.getScheme() + "://" + serverName;
      File yamlFile =
          ceYamlService.downloadCostOptimisationYaml(accountId, connectorIdentifier, harnessHost, serverName);
      return Response.ok(yamlFile)
          .header(CONTENT_TRANSFER_ENCODING, BINARY)
          .type("text/plain; charset=UTF-8")
          .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + "cost-optimisation-crd" + DOT_YAML)
          .build();
    }
  }

  @POST
  @Path(CLOUD_COST_K8S_CLUSTER_SETUP)
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get k8s cluster setup yaml based on features enabled", nickname = CLOUD_COST_K8S_CLUSTER_SETUP)
  public Response cloudCostK8sClusterSetup(@Context HttpServletRequest request,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @Valid @NotNull @RequestBody K8sClusterSetupRequest body) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (body.getFeaturesEnabled().contains(CEFeatures.BILLING)) {
        throw new InvalidRequestException("Feature BILLING not supported for CEK8sCluster Connector Setup");
      }

      final String serverName = request.getServerName();
      final String harnessHost = request.getScheme() + "://" + serverName;

      try {
        final String yamlFileContent =
            ceYamlService.unifiedCloudCostK8sClusterYaml(accountId, harnessHost, serverName, body);

        return Response.ok(yamlFileContent)
            .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + CLOUD_COST_K8S_CLUSTER_SETUP + DOT_YAML)
            .type("text/plain; charset=UTF-8")
            .build();
      } catch (DelegateServiceDriverException ex) {
        log.error("DelegateServiceDriverException: msg:[{}]", ex.getMessage(), ex);

        throw new HintException(
            String.format(HintException.DELEGATE_NOT_AVAILABLE,
                "https://ngdocs.harness.io/article/ltt65r6k39-set-up-cost-visibility-for-kubernetes"),
            new DelegateNotAvailableException(ex.getMessage(), WingsException.USER));
      } catch (Exception ex) {
        log.error("cloudCostK8sClusterSetup Exception", ex);

        throw new InvalidRequestException(ex.getMessage());
      }
    }
  }
}
