package io.harness.aws.awsv2;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_BACKOFF_MAX_ERROR_RETRIES;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.amazon.awssdk.core.retry.backoff.BackoffStrategy.defaultStrategy;
import static software.amazon.awssdk.core.retry.backoff.BackoffStrategy.defaultThrottlingStrategy;
import static software.amazon.awssdk.core.retry.conditions.RetryCondition.defaultRetryCondition;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.data.structure.UUIDGenerator;

import software.wings.beans.AmazonClientSDKDefaultBackoffStrategy;

import com.google.inject.Singleton;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class AwsApiV2HelperServiceImpl implements AwsApiV2HelperService {
  @Override
  public AwsCredentialsProvider getAwsCredentialsProvider(AwsInternalConfig awsConfig) {
    AwsCredentialsProvider credentialsProvider;
    if (awsConfig.isUseEc2IamCredentials()) {
      credentialsProvider = getIamRoleAwsCredentialsProvider();
    } else if (awsConfig.isUseIRSA()) {
      credentialsProvider = getIrsaAwsCredentialsProvider(awsConfig);
    } else {
      credentialsProvider = getStaticAwsCredentialsProvider(awsConfig);
    }
    if (awsConfig.isAssumeCrossAccountRole()) {
      return getStsAssumeRoleAwsCredentialsProvider(awsConfig, credentialsProvider);
    }
    return credentialsProvider;
  }

  @Override
  public ClientOverrideConfiguration getClientOverrideConfiguration(AwsInternalConfig awsConfig) {
    AmazonClientSDKDefaultBackoffStrategy defaultBackoffStrategy = awsConfig.getAmazonClientSDKDefaultBackoffStrategy();
    RetryPolicy retryPolicy;
    if (defaultBackoffStrategy != null) {
      retryPolicy =
          RetryPolicy.builder()
              .retryCondition(defaultRetryCondition())
              .numRetries(defaultBackoffStrategy.getMaxErrorRetry())
              .backoffStrategy(FullJitterBackoffStrategy.builder()
                                   .baseDelay(Duration.ofMillis(defaultBackoffStrategy.getBaseDelayInMs()))
                                   .maxBackoffTime(Duration.ofMillis(defaultBackoffStrategy.getMaxBackoffInMs()))
                                   .build())
              .throttlingBackoffStrategy(
                  EqualJitterBackoffStrategy.builder()
                      .baseDelay(Duration.ofMillis(defaultBackoffStrategy.getThrottledBaseDelayInMs()))
                      .maxBackoffTime(Duration.ofMillis(defaultBackoffStrategy.getMaxBackoffInMs()))
                      .build())
              .build();
    } else {
      retryPolicy = RetryPolicy.builder()
                        .retryCondition(defaultRetryCondition())
                        .numRetries(DEFAULT_BACKOFF_MAX_ERROR_RETRIES)
                        .backoffStrategy(defaultStrategy())
                        .throttlingBackoffStrategy(defaultThrottlingStrategy())
                        .build();
    }
    return ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build();
  }

  private AwsCredentialsProvider getIamRoleAwsCredentialsProvider() {
    try {
      if (System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI") != null
          || System.getenv("AWS_CONTAINER_CREDENTIALS_FULL_URI") != null) {
        return ContainerCredentialsProvider.builder().build();
      } else {
        return InstanceProfileCredentialsProvider.create();
      }
    } catch (SecurityException var2) {
      log.debug("Security manager did not allow access to the ECS credentials environment variable"
          + " AWS_CONTAINER_CREDENTIALS_RELATIVE_URI or the container full URI environment variable"
          + " AWS_CONTAINER_CREDENTIALS_FULL_URI. Please provide access to this environment variable "
          + "if you want to load credentials from ECS Container.");
      return InstanceProfileCredentialsProvider.create();
    }
  }

  private AwsCredentialsProvider getIrsaAwsCredentialsProvider(AwsInternalConfig awsConfig) {
    WebIdentityTokenFileCredentialsProvider.Builder providerBuilder = WebIdentityTokenFileCredentialsProvider.builder();
    providerBuilder.roleSessionName(awsConfig.getAccountId() + UUIDGenerator.generateUuid());
    return providerBuilder.build();
  }

  private AwsCredentialsProvider getStaticAwsCredentialsProvider(AwsInternalConfig awsConfig) {
    AwsBasicCredentials awsBasicCredentials =
        AwsBasicCredentials.create(new String(awsConfig.getAccessKey()), new String(awsConfig.getSecretKey()));
    return StaticCredentialsProvider.create(awsBasicCredentials);
  }

  private AwsCredentialsProvider getStsAssumeRoleAwsCredentialsProvider(
      AwsInternalConfig awsConfig, AwsCredentialsProvider primaryCredentialProvider) {
    AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                                              .roleArn(awsConfig.getCrossAccountAttributes().getCrossAccountRoleArn())
                                              .roleSessionName(UUID.randomUUID().toString())
                                              .externalId(awsConfig.getCrossAccountAttributes().getExternalId())
                                              .build();

    StsClient stsClient = StsClient.builder()
                              .credentialsProvider(primaryCredentialProvider)
                              .region(isNotBlank(awsConfig.getDefaultRegion()) ? Region.of(awsConfig.getDefaultRegion())
                                                                               : Region.of(AWS_DEFAULT_REGION))
                              .build();

    return StsAssumeRoleCredentialsProvider.builder().stsClient(stsClient).refreshRequest(assumeRoleRequest).build();
  }
}
