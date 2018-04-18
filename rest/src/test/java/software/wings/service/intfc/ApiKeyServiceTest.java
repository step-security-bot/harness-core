package software.wings.service.intfc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_KEY;

import org.junit.Before;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.query.Query;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.Base;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.dl.PageResponse.PageResponseBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.exception.UnauthorizedException;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.impl.ApiKeyServiceImpl;
import software.wings.utils.Validator;

import java.util.Arrays;

public class ApiKeyServiceTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private MainConfiguration configuration;
  @Mock private AccountService accountService;
  @InjectMocks private ApiKeyService apiKeyService = spy(ApiKeyServiceImpl.class);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGenerate() {
    Account account = anAccount().withUuid(ACCOUNT_ID).withAccountKey(ACCOUNT_KEY).build();
    doReturn(account).when(accountService).get(ACCOUNT_ID);
    String apiKey = apiKeyService.generate(ACCOUNT_ID);
    verify(wingsPersistence).save(any(ApiKeyEntry.class));
    assertThat(apiKey).isNotBlank();
    assertThat(apiKey.length()).isEqualTo(80);
  }

  @Test
  public void testDelete() {
    String uuid = generateUuid();
    apiKeyService.delete(uuid);
    verify(wingsPersistence).delete(ApiKeyEntry.class, uuid);
  }

  private SimpleEncryption getSimpleEncryption(String accountId) {
    Account account = accountService.get(accountId);
    Validator.notNullCheck("Account", account);
    return new SimpleEncryption(account.getAccountKey().toCharArray());
  }

  @Test
  public void testGet() {
    Account account = anAccount().withUuid(ACCOUNT_ID).withAccountKey(ACCOUNT_KEY).build();
    doReturn(account).when(accountService).get(ACCOUNT_ID);
    Query query = mock(Query.class);
    when(wingsPersistence.createQuery(ApiKeyEntry.class)).thenReturn(query);
    when(query.filter(anyString(), anyObject())).thenReturn(query);
    String apiKey = "Foo";
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder()
                                  .appId(Base.GLOBAL_APP_ID)
                                  .encryptedKey(getSimpleEncryption(ACCOUNT_ID).encryptChars(apiKey.toCharArray()))
                                  .hashOfKey("Hash Of Key")
                                  .accountId(ACCOUNT_ID)
                                  .build();
    doReturn(apiKeyEntry).when(query).get();
    String key = apiKeyService.get(generateUuid(), ACCOUNT_ID);
    assertThat(key).isEqualTo(apiKey);
  }

  @Test
  public void testList() {
    PageRequest request = PageRequestBuilder.aPageRequest().build();
    apiKeyService.list(request);
    verify(wingsPersistence).query(ApiKeyEntry.class, request);
  }

  @Test
  public void testValidate() {
    Account account = anAccount().withUuid(ACCOUNT_ID).withAccountKey(ACCOUNT_KEY).build();
    doReturn(account).when(accountService).get(ACCOUNT_ID);
    String apiKey = "Foo";
    PageResponse response =
        PageResponseBuilder.aPageResponse()
            .withResponse(
                Arrays.asList(ApiKeyEntry.builder()
                                  .appId(Base.GLOBAL_APP_ID)
                                  .encryptedKey(getSimpleEncryption(ACCOUNT_ID).encryptChars(apiKey.toCharArray()))
                                  .hashOfKey(hashpw(apiKey, BCrypt.gensalt()))
                                  .accountId(ACCOUNT_ID)
                                  .build()))
            .build();
    // doReturn(response).when(wingsPersistence).query(ApiKeyEntry.class, any(PageRequest.class));
    when(wingsPersistence.query(eq(ApiKeyEntry.class), any(PageRequest.class))).thenReturn(response);
    try {
      apiKeyService.validate(apiKey, ACCOUNT_ID);
    } catch (UnauthorizedException ex) {
      fail("Validation failed: " + ex.getMessage());
    }
  }
}
