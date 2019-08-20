package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;

import com.google.common.base.Preconditions;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.utils.KmsUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
@Slf4j
public abstract class AbstractSecretManagerValidation extends AbstractDelegateValidateTask {
  AbstractSecretManagerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  public static String getAwsUrlFromRegion(String region) {
    return KmsUtils.generateKmsUrl(region);
  }

  DelegateConnectionResult validateSecretManager() {
    EncryptionConfig encryptionConfig = getEncryptionConfig();
    // local encryption
    if (encryptionConfig == null) {
      return DelegateConnectionResult.builder()
          .criteria("encryption type: " + EncryptionType.LOCAL)
          .validated(true)
          .build();
    }
    Preconditions.checkNotNull(encryptionConfig);

    String secretManagerUrl = encryptionConfig.getEncryptionServiceUrl();
    return validateSecretManagerUrl(
        secretManagerUrl, encryptionConfig.getName(), encryptionConfig.getValidationCriteria());
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    DelegateConnectionResult delegateConnectionResult = validateSecretManager();
    if (!delegateConnectionResult.isValidated()) {
      delegateConnectionResult.setCriteria(getCriteria().get(0));
      return singletonList(delegateConnectionResult);
    }
    return super.validate();
  }

  protected EncryptionConfig getEncryptionConfig() {
    for (Object parameter : getParameters()) {
      if (parameter instanceof EncryptionConfig) {
        return (EncryptionConfig) parameter;
      } else if (parameter instanceof EncryptedDataDetail) {
        return ((EncryptedDataDetail) parameter).getEncryptionConfig();
      } else if (parameter instanceof List) {
        List details = (List) parameter;
        for (Object detail : details) {
          if (detail instanceof EncryptedDataDetail) {
            return ((EncryptedDataDetail) detail).getEncryptionConfig();
          }
        }
      }
    }
    return null;
  }

  @Override
  public List<String> getCriteria() {
    EncryptionConfig encryptionConfig = getEncryptionConfig();
    return singletonList(encryptionConfig.getValidationCriteria());
  }

  private DelegateConnectionResult validateSecretManagerUrl(
      String secretManagerUrl, String secretManagerName, String validationCriteria) {
    // Secret manager URL will be null for LOCAL secret manager, consider it reachable always.
    boolean urlReachable = secretManagerUrl == null || connectableHttpUrl(secretManagerUrl);
    logger.info("Finished validating Vault config '{}' with URL {}.", secretManagerName, secretManagerUrl);
    return DelegateConnectionResult.builder().criteria(validationCriteria).validated(urlReachable).build();
  }
}
