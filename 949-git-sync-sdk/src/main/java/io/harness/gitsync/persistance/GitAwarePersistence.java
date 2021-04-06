package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.YamlDTO;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(DX)
public interface GitAwarePersistence<B extends GitSyncableEntity, Y extends YamlDTO> {
  List<B> find(@NotNull Query query, String projectIdentifier, String orgIdentifier, String accountId);

  B save(B objectToSave, Y yaml, ChangeType changeType);

  /**
   * Default save which will treat changeType as ADD on git.
   */
  B save(B objectToSave, Y yaml);
}
