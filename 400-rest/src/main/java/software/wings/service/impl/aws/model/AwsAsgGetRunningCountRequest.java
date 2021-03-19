package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.GET_RUNNING_COUNT;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsAsgGetRunningCountRequest extends AwsAsgRequest {
  private String infraMappingId;

  @Builder
  public AwsAsgGetRunningCountRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String infraMappingId) {
    super(awsConfig, encryptionDetails, GET_RUNNING_COUNT, region);
    this.infraMappingId = infraMappingId;
  }
}
