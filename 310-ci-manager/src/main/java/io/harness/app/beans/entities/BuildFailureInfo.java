package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildFailureInfo {
  private String piplineName;
  private String branch;
  private String commit;
  private String commitID;
  private String startTs;
  private String endTs;
}
