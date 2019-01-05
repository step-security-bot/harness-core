package software.wings.beans.appmanifest;

import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.GitFileConfig;
import software.wings.yaml.BaseEntityYaml;

@Entity("applicationManifests")
@Indexes(@Index(options = @IndexOptions(name = "appManifestIdx", unique = true),
    fields = { @Field("appId")
               , @Field("envId"), @Field("serviceId"), @Field("kind") }))
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class ApplicationManifest extends Base {
  public static final String SERVICE_ID_KEY = "serviceId";
  public static final String ENV_ID_KEY = "envId";
  public static final String KIND_KEY = "kind";

  private String serviceId;
  private String envId;
  @NonNull @Default private AppManifestKind kind = K8S_MANIFEST;
  @NonNull StoreType storeType;
  GitFileConfig gitFileConfig;

  public ApplicationManifest cloneInternal() {
    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .serviceId(this.serviceId)
                                       .envId(this.envId)
                                       .storeType(this.storeType)
                                       .gitFileConfig(this.gitFileConfig)
                                       .kind(this.kind)
                                       .build();
    manifest.setAppId(this.appId);
    return manifest;
  }

  public enum AppManifestType { SERVICE, ENV, ENV_SERVICE }

  @Data
  @EqualsAndHashCode(callSuper = false)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String storeType;
    private GitFileConfig gitFileConfig;

    @Builder
    public Yaml(String type, String harnessApiVersion, String storeType, GitFileConfig gitFileConfig, String kind) {
      super(type, harnessApiVersion);
      this.storeType = storeType;
      this.gitFileConfig = gitFileConfig;
    }
  }
}
