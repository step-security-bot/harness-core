package io.harness.cdng.Deployment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@OwnedBy(HarnessTeam.DX)
@Getter
@AllArgsConstructor
public class EntityStatusDetails {
  long createdAt;
  boolean isDeleted;
  long deletedAt;

  public EntityStatusDetails(long createdAt) {
    this.createdAt = createdAt;
    this.isDeleted = false;
    this.deletedAt = -1;
  }
}
