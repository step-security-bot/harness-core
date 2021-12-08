package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsKmsConstants.ASSUME_IAM_ROLE)
@ApiModel("AwsKmsCredentialSpecAssumeIAM")
@Schema(name = "AwsKmsCredentialSpecAssumeIAM",
    description = "Returns the Delegate Selectors used by this AWS KMS Secret Manager Connector.")
public class AwsKmsCredentialSpecAssumeIAMDTO implements AwsKmsCredentialSpecDTO {
  @NotNull @Size(min = 1, message = "Delegate Selectors cannot be empty") private Set<String> delegateSelectors;
}
