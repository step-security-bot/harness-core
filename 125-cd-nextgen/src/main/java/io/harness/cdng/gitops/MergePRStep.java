/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.gitops;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.exception.WingsException.USER;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.gitops.steps.GitOpsStepHelper;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.gitapi.GitApiRequestType;
import io.harness.delegate.beans.gitapi.GitApiTaskParams;
import io.harness.delegate.beans.gitapi.GitRepoType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitOpsTaskType;
import io.harness.delegate.task.git.NGGitOpsResponse;
import io.harness.delegate.task.git.NGGitOpsTaskParams;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.utils.ConnectorUtils;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.GITOPS)
@Slf4j
public class MergePRStep extends TaskExecutableWithRollbackAndRbac<NGGitOpsResponse> {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StepHelper stepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private GitOpsStepHelper gitOpsStepHelper;
  @Inject private ConnectorUtils connectorUtils;

  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.GITOPS_MERGE_PR.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // Nothing to validate
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<NGGitOpsResponse> responseDataSupplier) throws Exception {
    ResponseData responseData = responseDataSupplier.get();

    NGGitOpsResponse ngGitOpsResponse = (NGGitOpsResponse) responseData;

    if (TaskStatus.SUCCESS.equals(ngGitOpsResponse.getTaskStatus())) {
      return StepResponse.builder()
          .unitProgressList(ngGitOpsResponse.getUnitProgressData().getUnitProgresses())
          .status(Status.SUCCEEDED)
          .build();
    }

    return StepResponse.builder()
        .unitProgressList(ngGitOpsResponse.getUnitProgressData().getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().setErrorMessage(ngGitOpsResponse.getErrorMessage()).build())
        .build();
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    MergePRStepParams gitOpsSpecParams = (MergePRStepParams) stepParameters.getSpec();

    ManifestOutcome releaseRepoOutcome = gitOpsStepHelper.getReleaseRepoOutcome(ambiance);

    GitStoreDelegateConfig gitStoreDelegateConfig = getGitStoreDelegateConfig(ambiance, releaseRepoOutcome);
    String repoOwner = null, repoName = null;
    switch (gitStoreDelegateConfig.getGitConfigDTO().getConnectorType()) {
      case GITHUB:
        GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) gitStoreDelegateConfig.getGitConfigDTO();
        repoOwner = githubConnectorDTO.getGitRepositoryDetails().getOrg();
        repoName = githubConnectorDTO.getGitRepositoryDetails().getName();
      default:
    }

    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.CREATE_PR_OUTCOME));
    int prNumber;
    if (optionalSweepingOutput != null && optionalSweepingOutput.isFound()) {
      CreatePROutcome createPROutcome = (CreatePROutcome) optionalSweepingOutput.getOutput();
      prNumber = createPROutcome.getPrNumber();
    } else {
      throw new InvalidRequestException("Pull Request Details are missing", USER);
    }

    String accountId = AmbianceUtils.getAccountId(ambiance);

    ConnectorInfoDTO connectorInfoDTO =
        cdStepHelper.getConnector(releaseRepoOutcome.getStore().getConnectorReference().getValue(), ambiance);
    String scope = "";
    if (!isEmpty(connectorInfoDTO.getProjectIdentifier()) & !isEmpty(accountId)) {
      scope = "org.";
    } else {
      scope = "account.";
    }
    ConnectorDetails connectorDetails =
        connectorUtils.getConnectorDetails(IdentifierRef.builder()
                                               .accountIdentifier(accountId)
                                               .orgIdentifier(connectorInfoDTO.getOrgIdentifier())
                                               .projectIdentifier(connectorInfoDTO.getProjectIdentifier())
                                               .build(),
            scope + connectorInfoDTO.getIdentifier());

    GitApiTaskParams gitApiTaskParams = GitApiTaskParams.builder()
                                            .gitRepoType(GitRepoType.GITHUB)
                                            .requestType(GitApiRequestType.MERGE_PR)
                                            .connectorDetails(connectorDetails)
                                            .prNumber(String.valueOf(prNumber))
                                            .owner(repoOwner)
                                            .repo(repoName)
                                            .build();

    NGGitOpsTaskParams ngGitOpsTaskParams = NGGitOpsTaskParams.builder()
                                                .gitOpsTaskType(GitOpsTaskType.MERGE_PR)
                                                .accountId(accountId)
                                                .connectorInfoDTO(connectorInfoDTO)
                                                .gitApiTaskParams(gitApiTaskParams)
                                                .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                                  .taskType(TaskType.GITOPS_TASK_NG.name())
                                  .parameters(new Object[] {ngGitOpsTaskParams})
                                  .build();

    String taskName = TaskType.GITOPS_TASK_NG.getDisplayName();

    return prepareCDTaskRequest(ambiance, taskData, kryoSerializer, gitOpsSpecParams.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(gitOpsSpecParams.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(Ambiance ambiance, ManifestOutcome manifestOutcome) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) manifestOutcome.getStore();
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = cdStepHelper.getConnector(connectorId, ambiance);

    return cdStepHelper.getGitStoreDelegateConfig(
        gitStoreConfig, connectorDTO, manifestOutcome, new ArrayList<>(), ambiance);
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return null;
  }
}
