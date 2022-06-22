package io.harness.ngsettings.beans;

import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.setting.SettingCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotBlank;

public class SettingValueRequestDTO {
  @Schema(description = SettingConstants.IDENTIFIER) @NotNull @NotBlank @EntityIdentifier String identifier;
  @NotNull @NotBlank @Schema(description = SettingConstants.CATEGORY) SettingCategory category;
}
