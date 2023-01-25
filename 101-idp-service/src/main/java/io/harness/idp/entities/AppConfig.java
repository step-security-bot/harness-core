package io.harness.idp.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.morphia.annotations.Entity;
import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AppConfigKeys")
@StoreIn(DbAliases.IDP)
@Entity(value = "appConfig", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("appConfig")
@OwnedBy(HarnessTeam.IDP)
public class AppConfig implements PersistentEntity {
}
