/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import com.google.inject.Inject;
import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.taskprogress.ITaskProgressClient;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.git.GitFetchFilesTaskHelper;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.delegate.task.serverless.request.ServerlessGitFetchRequest;
import io.harness.delegate.task.serverless.response.ServerlessGitFetchResponse;
import io.harness.git.GitClientV2;
import io.harness.git.model.FetchFilesResult;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static org.mockito.Mockito.*;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerlessGitFetchTaskTest extends CategoryTest {

    final DelegateTaskPackage delegateTaskPackage =
            DelegateTaskPackage.builder().data(TaskData.builder().build()).build();
    @Mock ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;
    @Mock GitAuthenticationDTO gitAuthenticationDTO = GitHTTPAuthenticationDTO.builder().build();
    @Mock BooleanSupplier preExecute;
    @Mock Consumer<DelegateTaskResponse> consumer;
    @Mock ILogStreamingTaskClient logStreamingTaskClient;
    @Mock GitDecryptionHelper gitDecryptionHelper;
    @Mock GitClientV2 gitClientV2;
    @Mock ITaskProgressClient taskProgressClient;
    @Mock ExecutorService executorService;
    @Mock GitFetchFilesTaskHelper gitFetchFilesTaskHelper;
    @Mock ScmFetchFilesHelperNG scmFetchFilesHelper;
    @Mock NGGitService ngGitService;
    @Mock SecretDecryptionService secretDecryptionService;
    @Inject
    @InjectMocks ServerlessGitFetchTask serverlessGitFetchTask = new ServerlessGitFetchTask(delegateTaskPackage,
            logStreamingTaskClient, consumer, preExecute);

    String identifier = "iden";
    String manifestType = "serverless-lambda";
    String url = "url";
    GitConnectionType gitConnectionType = GitConnectionType.ACCOUNT;
    ScmConnector scmConnector = GitConfigDTO.builder().url(url).gitAuthType(GitAuthType.HTTP).gitAuth(gitAuthenticationDTO)
            .validationRepo("asfd").branchName("asfdf").executeOnDelegate(false).delegateSelectors(Collections.emptySet()).gitConnectionType(gitConnectionType).build();
    String branch = "bran";
    String path = "path/";
    String accountId = "account";
    String configOverridePath = "override/";
    GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder().gitConfigDTO(scmConnector).
            fetchType(FetchType.BRANCH).branch(branch).optimizedFilesFetch(true).path(path).build();
    ServerlessGitFetchFileConfig serverlessGitFetchFileConfig = ServerlessGitFetchFileConfig.builder().identifier(identifier)
            .manifestType(manifestType).configOverridePath(configOverridePath).gitStoreDelegateConfig(gitStoreDelegateConfig).build();
    String activityId = "activityid";
    TaskParameters taskParameters = ServerlessGitFetchRequest.builder().activityId(activityId).accountId(accountId).
            serverlessGitFetchFileConfig(serverlessGitFetchFileConfig).shouldOpenLogStream(true).closeLogStream(true).build();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Reflect.on(serverlessGitFetchTask).set("serverlessGitFetchTaskHelper", serverlessGitFetchTaskHelper);
        Reflect.on(serverlessGitFetchTaskHelper).set("gitDecryptionHelper", gitDecryptionHelper);
        Reflect.on(serverlessGitFetchTaskHelper).set("gitClientV2", gitClientV2);
        Reflect.on(serverlessGitFetchTaskHelper).set("gitFetchFilesTaskHelper", gitFetchFilesTaskHelper);
        Reflect.on(serverlessGitFetchTaskHelper).set("scmFetchFilesHelper", scmFetchFilesHelper);
        Reflect.on(serverlessGitFetchTaskHelper).set("ngGitService", ngGitService);
        Reflect.on(serverlessGitFetchTaskHelper).set("secretDecryptionService", secretDecryptionService);
        doReturn(taskProgressClient).when(logStreamingTaskClient).obtainTaskProgressClient();
        doReturn(executorService).when(logStreamingTaskClient).obtainTaskProgressExecutor();
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void runTest() {

        String combinedPath = path+configOverridePath;
        List<String> filePaths = Collections.singletonList(combinedPath);
        FetchFilesResult fetchFilesResult = FetchFilesResult.builder().build();

        doReturn(fetchFilesResult).when(serverlessGitFetchTaskHelper).fetchFileFromRepo(gitStoreDelegateConfig, filePaths,
                accountId, null);
        ServerlessGitFetchResponse serverlessGitFetchResponse = (ServerlessGitFetchResponse) serverlessGitFetchTask.run(taskParameters);
        Map<String, FetchFilesResult> filesFromMultipleRepo = new HashMap<>();
        filesFromMultipleRepo.put(serverlessGitFetchFileConfig.getIdentifier(), fetchFilesResult);
        assertThat(serverlessGitFetchResponse.getFilesFromMultipleRepo()).isEqualTo(filesFromMultipleRepo);
    }
}
