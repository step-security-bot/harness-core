package io.harness.functional;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graphql.GraphQL;
import io.harness.CategoryTest;
import io.harness.GraphQLTestMixin;
import io.harness.beans.ExecutionStatus;
import io.harness.framework.DelegateExecutor;
import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.harness.restutils.PipelineRestUtils;
import io.harness.restutils.WorkflowRestUtils;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.harness.utils.FileUtils;
import io.restassured.RestAssured;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import software.wings.beans.Account;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;

@Slf4j
public abstract class AbstractFunctionalTest extends CategoryTest implements GraphQLTestMixin {
  protected static String bearerToken;
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public FunctionalTestRule rule = new FunctionalTestRule(lifecycleRule.getClosingFactory());

  @Override
  public GraphQL getGraphQL() {
    return rule.getGraphQL();
  }

  @BeforeClass
  public static void setup() {
    Setup.portal();
    RestAssured.useRelaxedHTTPSValidation();
  }

  //  @Inject private AccountGenerator accountGenerator;
  @Inject private DelegateExecutor delegateExecutor;
  //  @Inject OwnerManager ownerManager;
  @Inject private AccountSetupService accountSetupService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Getter static Account account;

  @Before
  public void testSetup() throws IOException {
    account = accountSetupService.ensureAccount();
    delegateExecutor.ensureDelegate(account);
    bearerToken = Setup.getAuthToken("admin@harness.io", "admin");
    logger.info("Basic setup completed");
  }

  protected void resetCache() {
    RestResponse<User> userRestResponse = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("accountId", account.getUuid())
                                              .put("/users/reset-cache")
                                              .as(new GenericType<RestResponse<User>>() {}.getType());
    assertThat(userRestResponse).isNotNull();
  }

  @AfterClass
  public static void cleanup() {
    FileUtils.deleteModifiedConfig();
    logger.info("All tests exit");
  }

  public WorkflowExecution runWorkflow(String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    WorkflowExecution original = WorkflowRestUtils.startWorkflow(bearerToken, appId, envId, executionArgs);

    Awaitility.await().atMost(120, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      final WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
      return workflowExecution != null && ExecutionStatus.isFinalStatus(workflowExecution.getStatus());
    });

    return workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
  }

  public WorkflowExecution runPipeline(String bearerToken, String appId, String envId, ExecutionArgs executionArgs) {
    WorkflowExecution original = PipelineRestUtils.startPipeline(bearerToken, appId, envId, executionArgs);

    Awaitility.await().atMost(120, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      final WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
      return workflowExecution != null && ExecutionStatus.isFinalStatus(workflowExecution.getStatus());
    });

    return workflowExecutionService.getWorkflowExecution(appId, original.getUuid());
  }
}
