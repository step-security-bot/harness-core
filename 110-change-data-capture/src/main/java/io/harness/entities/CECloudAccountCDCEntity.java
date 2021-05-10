package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changehandlers.CECloudAccountBigQueryChangeHandler;
import io.harness.changehandlers.TimeScaleDBChangeHandler;
import io.harness.persistence.PersistentEntity;

import software.wings.beans.ce.CECloudAccount;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CE)
public class CECloudAccountCDCEntity implements CDCEntity<CECloudAccount> {
  @Inject TimeScaleDBChangeHandler timeScaleDBChangeHandler;
  @Inject CECloudAccountBigQueryChangeHandler ceCloudAccountBigQueryChangeHandler;

  @Override
  public ChangeHandler getChangeHandler(String handlerClass) {
    if (handlerClass.equals(CECloudAccountBigQueryChangeHandler.class.getSimpleName())) {
      return ceCloudAccountBigQueryChangeHandler;
    }
    return timeScaleDBChangeHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return CECloudAccount.class;
  }
}
