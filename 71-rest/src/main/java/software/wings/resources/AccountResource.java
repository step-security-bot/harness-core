package software.wings.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.AccountSalesContactsInfo;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.licensing.LicenseService;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.intfc.AccountService;
import software.wings.utils.AccountPermissionUtils;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("account")
@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Inject private AccountPermissionUtils accountPermissionUtils;

  @GET
  @Path("{accountId}/status")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<String> getStatus(@PathParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getAccountStatus(accountId));
  }

  @POST
  @Path("{accountId}/start-migration")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> startMigration(@PathParam("accountId") String accountId) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to start account migration");
    if (response == null) {
      response = new RestResponse<>(accountService.startAccountMigration(accountId));
    }
    return response;
  }

  @POST
  @Path("{accountId}/complete-migration")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> completeMigration(@PathParam("accountId") String accountId, String newClusterUrl) {
    RestResponse<Boolean> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to complete account migration");
    if (response == null) {
      response = new RestResponse<>(accountService.completeAccountMigration(accountId, newClusterUrl));
    }
    return response;
  }

  @GET
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<PageResponse<Account>> getAccounts(@QueryParam("offset") String offset) {
    PageRequest<Account> accountPageRequest =
        aPageRequest().withOffset(offset).withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE)).build();
    return new RestResponse<>(accountService.getAccounts(accountPageRequest));
  }

  @GET
  @Path("feature-flag-enabled")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> isFeatureEnabled(
      @QueryParam("featureName") String featureName, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(accountService.isFeatureFlagEnabled(featureName, accountId));
  }

  @GET
  @Path("services-cv-24x7")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<PageResponse<CVEnabledService>> getAllServicesFor24x7(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @BeanParam PageRequest<String> request) {
    return new RestResponse<>(
        accountService.getServices(accountId, UserThreadLocal.get().getPublicUser(), request, serviceId));
  }

  @GET
  @Path("services-cv-24x7-breadcrumb")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<Service>> getAllServicesFor24x7(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getServicesBreadCrumb(accountId, UserThreadLocal.get().getPublicUser()));
  }

  @PUT
  @Path("license/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Account> updateAccountLicense(
      @PathParam("accountId") @NotEmpty String accountId, LicenseInfo licenseInfo) {
    RestResponse<Account> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to update account license");
    if (response == null) {
      response = new RestResponse<>(licenseService.updateAccountLicense(accountId, licenseInfo));
    }
    return response;
  }

  @PUT
  @Path("{accountId}/sales-contacts")
  @Timed
  @ExceptionMetered
  public RestResponse<Account> updateAccountSalesContacts(
      @PathParam("accountId") @NotEmpty String accountId, AccountSalesContactsInfo salesContactsInfo) {
    RestResponse<Account> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to update account sales contacts");
    if (response == null) {
      response = new RestResponse<>(
          licenseService.updateAccountSalesContacts(accountId, salesContactsInfo.getSalesContacts()));
    }
    return response;
  }

  @PUT
  @Path("license/generate/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> generateLicense(
      @PathParam("accountId") @NotEmpty String accountId, LicenseInfo licenseInfo) {
    RestResponse<String> response =
        accountPermissionUtils.checkIfHarnessUser("User not allowed to generate a new license");
    if (response == null) {
      response = new RestResponse<>(licenseService.generateLicense(licenseInfo));
    }
    return response;
  }

  @DELETE
  @Path("delete/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteAccount(@PathParam("accountId") @NotEmpty String accountId) {
    RestResponse<Boolean> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to delete account");
    if (response == null) {
      response = new RestResponse<>(accountService.delete(accountId));
    }
    return response;
  }

  @POST
  @Path("new")
  @Timed
  @ExceptionMetered
  public RestResponse<Account> createAccount(@NotNull Account account) {
    RestResponse<Account> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to create account");
    if (response == null) {
      account.setAppId(GLOBAL_APP_ID);
      response = new RestResponse<>(accountService.save(account));
    }
    return response;
  }
}
