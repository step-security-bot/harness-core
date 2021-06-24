package io.harness.connector.entities.embedded.jira;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "JiraConnectorKeys")
@EqualsAndHashCode(callSuper = true)
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.jira.JiraConnector")
public class JiraConnector extends Connector {
  String jiraUrl;
  String username;
  String usernameRef;
  String passwordRef;
}
