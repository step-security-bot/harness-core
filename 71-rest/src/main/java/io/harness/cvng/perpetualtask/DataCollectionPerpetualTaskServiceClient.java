package io.harness.cvng.perpetualtask;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.Cd1SetupFields;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataCollectionPerpetualTaskServiceClient implements PerpetualTaskServiceClient {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private KryoSerializer kryoSerializer;
  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    logger.info("getting client params {}", clientContext.getClientParams());
    Map<String, String> clientParams = clientContext.getClientParams();
    String accountId = clientParams.get("accountId");
    String cvConfigId = clientParams.get("cvConfigId");
    String verificationTaskId = clientParams.get("verificationTaskId");
    String connectorId = clientParams.get("connectorId");
    String dataCollectionWorkerId = clientParams.get("dataCollectionWorkerId");

    SettingAttribute settingAttribute = settingsService.get(connectorId);
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    if (settingAttribute.getValue() instanceof EncryptableSetting) {
      encryptedDataDetails = secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue());
    }
    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .settingValue(settingAttribute.getValue())
                                                    .encryptedDataDetails(encryptedDataDetails)
                                                    .build();
    ByteString bytes = ByteString.copyFrom(kryoSerializer.asBytes(cvDataCollectionInfo));
    DataCollectionPerpetualTaskParams.Builder params = DataCollectionPerpetualTaskParams.newBuilder()
                                                           .setAccountId(accountId)
                                                           .setDataCollectionInfo(bytes)
                                                           .setDataCollectionWorkerId(dataCollectionWorkerId);
    if (verificationTaskId != null) {
      params.setVerificationTaskId(verificationTaskId);
    }
    if (cvConfigId != null) {
      params.setCvConfigId(cvConfigId);
    }
    return params.build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    logger.info("Get validation task {} {}", accountId, clientContext);
    Map<String, String> clientParams = clientContext.getClientParams();
    logger.info("Client params {}", clientParams);
    // TODO: move this to capability framework. For now the validation will always pass.
    return DelegateTask.builder()
        .accountId(accountId)
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.DATA_COLLECTION_NEXT_GEN_VALIDATION.name())
                  .parameters(new Object[] {"test"})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .build();
  }
}