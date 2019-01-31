package software.wings.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.Constants.HTTP_POST;
import static software.wings.common.Constants.HTTP_PUT;
import static software.wings.common.Constants.RESOURCE_URI_CLONE_APP;
import static software.wings.common.Constants.RESOURCE_URI_CLONE_ENVIRONMENT;
import static software.wings.common.Constants.RESOURCE_URI_CLONE_PIPELINE;
import static software.wings.common.Constants.RESOURCE_URI_CLONE_PROVISIONER;
import static software.wings.common.Constants.RESOURCE_URI_CLONE_SERVICE;
import static software.wings.common.Constants.RESOURCE_URI_CLONE_WORKFLOW;
import static software.wings.common.Constants.RESOURCE_URI_CREATE_APP;
import static software.wings.common.Constants.RESOURCE_URI_CREATE_ENVIRONMENT;
import static software.wings.common.Constants.RESOURCE_URI_CREATE_PIPELINE;
import static software.wings.common.Constants.RESOURCE_URI_CREATE_PROVISIONER;
import static software.wings.common.Constants.RESOURCE_URI_CREATE_SERVICE;
import static software.wings.common.Constants.RESOURCE_URI_CREATE_WORKFLOW;
import static software.wings.common.Constants.RESOURCE_URI_UPDATE_ENVIRONMENT;
import static software.wings.common.Constants.RESOURCE_URI_UPDATE_PIPELINE;
import static software.wings.common.Constants.RESOURCE_URI_UPDATE_WORKFLOW;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;

import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 4/20/16.
 */
@Singleton
public class AuthResponseFilter implements ContainerResponseFilter {
  private static final Logger logger = LoggerFactory.getLogger(AuthResponseFilter.class);
  private static final Set<String> createUris =
      Sets.newHashSet(RESOURCE_URI_CREATE_APP, RESOURCE_URI_CREATE_SERVICE, RESOURCE_URI_CREATE_PROVISIONER,
          RESOURCE_URI_CREATE_ENVIRONMENT, RESOURCE_URI_CREATE_WORKFLOW, RESOURCE_URI_CREATE_PIPELINE);
  private static final Set<String> cloneUris =
      Sets.newHashSet(RESOURCE_URI_CLONE_APP, RESOURCE_URI_CLONE_SERVICE, RESOURCE_URI_CLONE_PROVISIONER,
          RESOURCE_URI_CLONE_ENVIRONMENT, RESOURCE_URI_CLONE_WORKFLOW, RESOURCE_URI_CLONE_PIPELINE);
  private static final Set<String> updateUris =
      Sets.newHashSet(RESOURCE_URI_UPDATE_ENVIRONMENT, RESOURCE_URI_UPDATE_WORKFLOW, RESOURCE_URI_UPDATE_PIPELINE);

  @Inject private AuthService authService;
  @Inject private AppService appService;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    UserThreadLocal.unset(); // clear user object from thread local
    invalidateAccountCacheIfNeeded(requestContext);
  }

  private void invalidateAccountCacheIfNeeded(ContainerRequestContext requestContext) {
    String httpMethod = requestContext.getMethod();
    String resourcePath = requestContext.getUriInfo().getAbsolutePath().getPath();

    if (HTTP_PUT.equals(httpMethod) && updateUris.stream().anyMatch(pattern -> resourcePath.matches(pattern))) {
      evict(requestContext, resourcePath);
    } else if (HTTP_POST.equals(httpMethod)
        && (createUris.contains(resourcePath)
               || cloneUris.stream().anyMatch(pattern -> resourcePath.matches(pattern)))) {
      evict(requestContext, resourcePath);
    }
  }

  private void evict(ContainerRequestContext requestContext, String resourcePath) {
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    String accountId = queryParameters.getFirst("accountId");
    try {
      String appId = queryParameters.getFirst("appId");

      // Special handling for AppResource
      if (resourcePath.startsWith("/api/apps") && isEmpty(accountId) && isEmpty(appId)) {
        appId = requestContext.getUriInfo().getPathParameters().getFirst("appId");
      }

      if (isEmpty(accountId) && isEmpty(appId)) {
        logger.error("Cache eviction failed for resource 2 [{}]", ((ContainerRequest) requestContext).getRequestUri());
        return;
      }

      accountId = isEmpty(accountId) ? appService.getAccountIdByAppId(appId) : accountId;
      String finalAccountId = accountId;
      authService.evictAccountUserPermissionInfoCache(finalAccountId, true);
    } catch (Exception ex) {
      logger.error("Cache eviction failed for resourcePath {} for accountId {}", resourcePath, accountId, ex);
    }
  }
}
