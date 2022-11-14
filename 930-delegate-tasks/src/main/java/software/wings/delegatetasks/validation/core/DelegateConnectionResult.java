package software.wings.delegatetasks.validation.core;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@AllArgsConstructor
@OwnedBy(DEL)
public class DelegateConnectionResult {
  private String uuid;
  private String accountId;
  private String delegateId;
  private String criteria;
  private boolean validated;
  private long duration;
  private long lastUpdatedAt;
  @Builder.Default private Date validUntil = getValidUntilTime();

  public static Date getValidUntilTime() {
    return Date.from(OffsetDateTime.now().plusDays(30).toInstant());
  }
}
