package io.harness.connector.mappers.ceawsmapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig;
import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig.CEAwsConfigBuilder;
import io.harness.connector.entities.embedded.ceawsconnector.CURAttributes;
import io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails;
import io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails.S3BucketDetailsBuilder;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.CEAwsSetupConfig;

import com.amazonaws.arn.Arn;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(DX)
public class CEAwsDTOToEntity implements ConnectorDTOToEntityMapper<CEAwsConnectorDTO, CEAwsConfig> {
  @Inject AwsClient awsClient;
  @Inject CEAwsSetupConfig ceAwsSetupConfig;

  @Override
  public CEAwsConfig toConnectorEntity(CEAwsConnectorDTO connectorDTO) {
    CEAwsConfigBuilder ceAwsConfigBuilder = CEAwsConfig.builder();

    List<CEAwsFeatures> featuresList = connectorDTO.getFeaturesEnabled();
    if (featuresList.contains(CEAwsFeatures.CUR)) {
      final AwsCurAttributesDTO awsCurAttributes = connectorDTO.getCurAttributes();

      if (awsCurAttributes == null) {
        throw new InvalidRequestException("curAttributes should be provided when the features 'CUR' is enabled.");
      }

      final S3BucketDetailsBuilder s3BucketDetailsBuilder =
          S3BucketDetails.builder().s3BucketName(awsCurAttributes.getS3BucketName());

      Optional<ReportDefinition> report = getReportDefinition(connectorDTO);
      if (report.isPresent()) {
        s3BucketDetailsBuilder.region(report.get().getS3Region());
        s3BucketDetailsBuilder.s3Prefix(report.get().getS3Prefix());
      }

      final CURAttributes curAttributes = CURAttributes.builder()
                                              .reportName(awsCurAttributes.getReportName())
                                              .s3BucketDetails(s3BucketDetailsBuilder.build())
                                              .build();
      ceAwsConfigBuilder.curAttributes(curAttributes);
    }

    final CrossAccountAccessDTO crossAccountAccessDTO = connectorDTO.getCrossAccountAccess();

    return ceAwsConfigBuilder.crossAccountAccess(crossAccountAccessDTO)
        .awsAccountId(getAccountId(crossAccountAccessDTO))
        .featuresEnabled(featuresList)
        .build();
  }

  @VisibleForTesting
  public Optional<ReportDefinition> getReportDefinition(CEAwsConnectorDTO connectorDTO) {
    try {
      final AWSCredentialsProvider credentialsProvider =
          awsClient.getAssumedCredentialsProvider(awsClient.constructStaticBasicAwsCredentials(
                                                      ceAwsSetupConfig.getAccessKey(), ceAwsSetupConfig.getSecretKey()),
              connectorDTO.getCrossAccountAccess().getCrossAccountRoleArn(),
              connectorDTO.getCrossAccountAccess().getExternalId());
      return awsClient.getReportDefinition(credentialsProvider, connectorDTO.getCurAttributes().getReportName());
    } catch (Exception ex) {
      log.error("Error getting report definition", ex);
      return Optional.empty();
    }
  }

  private static String getAccountId(final CrossAccountAccessDTO crossAccountAccessDTO) {
    Arn roleArn = Arn.fromString(crossAccountAccessDTO.getCrossAccountRoleArn());
    return roleArn.getAccountId();
  }
}
