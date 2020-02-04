package software.wings.service.impl.yaml;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.ARTIFACT_SOURCE;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.VersionedTemplate;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

public class YamlGitServiceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private YamlGitServiceImpl yamlGitService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSyncForTemplates() {
    TemplateFolder templateFolder = TemplateFolder.builder()
                                        .appId(GLOBAL_APP_ID)
                                        .accountId(GLOBAL_ACCOUNT_ID)
                                        .name("test folder")
                                        .uuid("uuid")
                                        .build();
    wingsPersistence.save(templateFolder);
    BaseTemplate baseTemplate = ArtifactSourceTemplate.builder().build();
    Template template = Template.builder()
                            .appId(GLOBAL_APP_ID)
                            .name("test template")
                            .uuid("uuidtemplate")
                            .version(1)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .folderId(templateFolder.getUuid())
                            .folderPathId(templateFolder.getUuid())
                            .type(ARTIFACT_SOURCE)
                            .templateObject(baseTemplate)
                            .build();
    wingsPersistence.save(template);
    VersionedTemplate versionedTemplate = VersionedTemplate.builder()
                                              .accountId(GLOBAL_ACCOUNT_ID)
                                              .templateId(template.getUuid())
                                              .version(Long.valueOf(1))
                                              .templateObject(baseTemplate)
                                              .build();
    wingsPersistence.save(versionedTemplate);
    YamlGitConfig yamlGitConfig = YamlGitConfig.builder()
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .entityId(GLOBAL_ACCOUNT_ID)
                                      .enabled(true)
                                      .syncMode(YamlGitConfig.SyncMode.BOTH)
                                      .entityType(EntityType.ACCOUNT)
                                      .build();
    wingsPersistence.save(yamlGitConfig);
    yamlGitService.syncForTemplates(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID);
    YamlChangeSet yamlChangeSet = wingsPersistence.createQuery(YamlChangeSet.class).get();
    assertThat(yamlChangeSet).isNotNull();
    assertThat(yamlChangeSet.getGitFileChanges().get(0).getFilePath())
        .isEqualTo("Setup/Template Library/test folder/test template.yaml");
  }
}
