package io.harness.connector.entities.embedded.githubconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.githubconnector.GithubSshAuthentication")
@OwnedBy(DX)
public class GithubSshAuthentication implements GithubAuthentication {
  String sshKeyRef;
}
