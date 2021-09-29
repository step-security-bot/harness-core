package software.wings.beans.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.GitFetchFilesConfig;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._870_YAML_BEANS)
public class GitFetchFilesFromMultipleRepoResult extends GitCommandResult {
  Map<String, GitFetchFilesResult> filesFromMultipleRepo;
  Map<String, GitFetchFilesConfig> gitFetchFilesConfigMap;

  public GitFetchFilesFromMultipleRepoResult() {
    super(GitCommandType.FETCH_FILES_FROM_MULTIPLE_REPO);
  }

  public GitFetchFilesFromMultipleRepoResult(
      Map<String, GitFetchFilesResult> filesFromMultipleRepo, Map<String, GitFetchFilesConfig> gitFetchFilesConfigMap) {
    super(GitCommandType.FETCH_FILES_FROM_MULTIPLE_REPO);
    this.filesFromMultipleRepo = filesFromMultipleRepo;
    this.gitFetchFilesConfigMap = gitFetchFilesConfigMap;
  }
}
