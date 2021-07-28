package software.wings.service.impl.aws.delegate;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationSourceType;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GitOperationContext;
import software.wings.service.impl.aws.client.CloseableAmazonWebServiceClient;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.utils.GitUtilsDelegate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryResult;
import com.amazonaws.services.cloudformation.model.ParameterDeclaration;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@Slf4j
@OwnedBy(CDP)
public class AwsCFHelperServiceDelegateImpl extends AwsHelperServiceDelegateBase implements AwsCFHelperServiceDelegate {
  @Inject private GitUtilsDelegate gitUtilsDelegate;

  @VisibleForTesting
  AmazonCloudFormationClient getAmazonCloudFormationClient(Regions region, AwsConfig awsConfig) {
    AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard().withRegion(region);
    attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonCloudFormationClient) builder.build();
  }

  @Override
  public List<AwsCFTemplateParamsData> getParamsData(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String data, String type, GitFileConfig gitFileConfig, GitConfig gitConfig,
      List<EncryptedDataDetail> sourceRepoEncryptedDetail) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      GetTemplateSummaryRequest request = new GetTemplateSummaryRequest();
      if ("s3".equalsIgnoreCase(type)) {
        request.withTemplateURL(data);
      } else if (CloudFormationSourceType.GIT.name().equalsIgnoreCase(type)) {
        GitOperationContext gitOperationContext =
            gitUtilsDelegate.cloneRepo(gitConfig, gitFileConfig, sourceRepoEncryptedDetail);
        String absoluteTemplatePath =
            gitUtilsDelegate.resolveAbsoluteFilePath(gitOperationContext, gitFileConfig.getFilePath());
        request.withTemplateBody(gitUtilsDelegate.getRequestDataFromFile(absoluteTemplatePath));
      } else {
        request.withTemplateBody(data);
      }
      tracker.trackCFCall("Get Template Summary");
      GetTemplateSummaryResult result = closeableAmazonCloudFormationClient.getClient().getTemplateSummary(request);
      List<ParameterDeclaration> parameters = result.getParameters();
      if (isNotEmpty(parameters)) {
        return parameters.stream()
            .map(parameter
                -> AwsCFTemplateParamsData.builder()
                       .paramKey(parameter.getParameterKey())
                       .paramType(parameter.getParameterType())
                       .defaultValue(parameter.getDefaultValue())
                       .build())
            .collect(toList());
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception getParamsData", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return emptyList();
  }

  @Override
  public String getStackBody(AwsConfig awsConfig, String region, String stackId) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      GetTemplateRequest getTemplateRequest = new GetTemplateRequest().withStackName(stackId);
      tracker.trackCFCall("Get Template");
      GetTemplateResult getTemplateResult =
          closeableAmazonCloudFormationClient.getClient().getTemplate(getTemplateRequest);
      return getTemplateResult.getTemplateBody();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception getStackBody", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return "";
  }

  @Override
  public List<String> getCapabilities(AwsConfig awsConfig, String region, String data, String type) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      GetTemplateSummaryRequest request = new GetTemplateSummaryRequest();
      if ("s3".equalsIgnoreCase(type)) {
        request.withTemplateURL(data);
      } else {
        request.withTemplateBody(data);
      }
      tracker.trackCFCall("Get Template Summary");
      GetTemplateSummaryResult result = closeableAmazonCloudFormationClient.getClient().getTemplateSummary(request);
      return result.getCapabilities();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception getCapabilities", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return emptyList();
  }
}
