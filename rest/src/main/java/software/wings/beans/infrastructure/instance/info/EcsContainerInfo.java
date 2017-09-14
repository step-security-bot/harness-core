package software.wings.beans.infrastructure.instance.info;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcsContainerInfo extends ContainerInfo {
  private String taskArn;
  private String taskDefinitionArn;
  private String serviceName;
  private long version;
  private long startedAt;
  private String startedBy;

  private EcsContainerInfo() {
    super();
  }

  public static final class Builder {
    private String taskArn;
    private String taskDefinitionArn;
    private String clusterName;
    private String serviceName;
    private long version;
    private long startedAt;
    private String startedBy;

    private Builder() {}

    public static Builder anEcsContainerInfo() {
      return new Builder();
    }

    public Builder withTaskDefinitionArn(String taskDefinitionArn) {
      this.taskDefinitionArn = taskDefinitionArn;
      return this;
    }

    public Builder withTaskArn(String taskArn) {
      this.taskArn = taskArn;
      return this;
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withVersion(long version) {
      this.version = version;
      return this;
    }

    public Builder withStartedAt(long startedAt) {
      this.startedAt = startedAt;
      return this;
    }

    public Builder withStartedBy(String startedBy) {
      this.startedBy = startedBy;
      return this;
    }

    public Builder but() {
      return anEcsContainerInfo()
          .withTaskDefinitionArn(taskDefinitionArn)
          .withTaskArn(taskArn)
          .withClusterName(clusterName)
          .withServiceName(serviceName)
          .withVersion(version)
          .withStartedAt(startedAt)
          .withStartedBy(startedBy);
    }

    public EcsContainerInfo build() {
      EcsContainerInfo ecsContainerInfo = new EcsContainerInfo();
      ecsContainerInfo.setTaskArn(taskArn);
      ecsContainerInfo.setTaskDefinitionArn(taskDefinitionArn);
      ecsContainerInfo.setClusterName(clusterName);
      ecsContainerInfo.setServiceName(serviceName);
      ecsContainerInfo.setVersion(version);
      ecsContainerInfo.setStartedAt(startedAt);
      ecsContainerInfo.setStartedBy(startedBy);
      return ecsContainerInfo;
    }
  }
}
