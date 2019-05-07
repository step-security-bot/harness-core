package software.wings.security.authentication;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.security.authentication.TwoFactorAuthenticationMechanism.TOTP;

import com.google.inject.Inject;

import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.RepeatRule.Repeat;
import org.joda.time.DateTimeUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.TwoFactorAuthenticationSettings.TwoFactorAuthenticationSettingsBuilder;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class TwoFactorAuthenticationManagerTest extends WingsBaseTest {
  @Mock UserService userService;
  @Mock AuthService authService;
  @Inject @InjectMocks TOTPAuthHandler totpAuthHandler;
  @Inject @InjectMocks TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  @Mock AuthenticationUtils authenticationUtils;
  @Mock AccountService accountService;

  @Test
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void shouldTwoFactorAuthenticationUsingTOTP() throws InterruptedException {
    try {
      TwoFactorAuthHandler handler = twoFactorAuthenticationManager.getTwoFactorAuthHandler(TOTP);
      User user = spy(new User());
      when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
      String totpSecretKey = TimeBasedOneTimePasswordUtil.generateBase32Secret();
      user.setTotpSecretKey(totpSecretKey);
      doReturn(TOTP).when(user).getTwoFactorAuthenticationMechanism();
      String encryptedCode = null;

      for (int t = 1; t < 60; t++) {
        DateTimeUtils.setCurrentMillisOffset(t * 1000);
        int i = -5000;
        while (i < 6000) {
          long currentTime = DateTimeUtils.currentTimeMillis();
          long timeWithLag = currentTime + i;
          log().info("Running test with time lag: [{}],currentTime=[{}],timeWithLag=[{}]", i, new Date(currentTime),
              new Date(timeWithLag));
          String code = TimeBasedOneTimePasswordUtil.generateNumberString(totpSecretKey, timeWithLag, 30);
          User authenticatedUser = spy(new User());
          authenticatedUser.setToken("ValidToken");

          when(authService.generateBearerTokenForUser(user)).thenReturn(authenticatedUser);
          encryptedCode = encodeBase64("testJWTToken:" + code);
          assertThat(twoFactorAuthenticationManager.authenticate(encryptedCode)).isEqualTo(authenticatedUser);
          i = i + 1000;
        }
      }
      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(null);
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.USER_DOES_NOT_EXIST.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        user.setTotpSecretKey(null);

        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.INVALID_TWO_FACTOR_AUTHENTICATION_CONFIGURATION.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        user.setTotpSecretKey(totpSecretKey);
        encryptedCode = encodeBase64("testJWTToken:1234");
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.INVALID_TOTP_TOKEN.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        String code =
            TimeBasedOneTimePasswordUtil.generateNumberString(totpSecretKey, System.currentTimeMillis() - 100000, 30);
        user.setTotpSecretKey(totpSecretKey);
        encryptedCode = encodeBase64("testJWTToken:" + code);
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.INVALID_TOTP_TOKEN.name());
      }

      try {
        when(userService.verifyJWTToken(anyString(), any(JWT_CATEGORY.class))).thenReturn(user);
        user.setTotpSecretKey(totpSecretKey);
        encryptedCode = encodeBase64("testJWTToken:faketoken");
        twoFactorAuthenticationManager.authenticate(encryptedCode);
        failBecauseExceptionWasNotThrown(WingsException.class);
      } catch (WingsException e) {
        assertThat(e).hasMessage(ErrorCode.UNKNOWN_ERROR.name());
      }

    } catch (GeneralSecurityException e) {
      fail(e.getMessage());
    } finally {
      DateTimeUtils.setCurrentMillisOffset(0);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCreateTwoFactorAuthenticationSettingsTotp() {
    User user = spy(new User());
    Account account = mock(Account.class);
    when(account.getCompanyName()).thenReturn("TestCompany");
    when(authenticationUtils.getPrimaryAccount(user)).thenReturn(account);

    TwoFactorAuthenticationSettings settings =
        twoFactorAuthenticationManager.createTwoFactorAuthenticationSettings(user, TOTP);
    assertThat(settings.getMechanism()).isEqualTo(TOTP);
    assertThat(settings.isTwoFactorAuthenticationEnabled()).isFalse();
    assertThat(settings.getTotpSecretKey()).isNotEmpty();
    assertThat(settings.getTotpqrurl()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldOverrideTwoFactorAuthentication() {
    Account account = getAccount(AccountType.PAID, false);
    TwoFactorAdminOverrideSettings twoFactorAdminOverrideSettings = new TwoFactorAdminOverrideSettings(true);
    when(accountService.isCommunityAccount(account.getUuid())).thenReturn(false);

    when(userService.overrideTwoFactorforAccount(
             account.getUuid(), twoFactorAdminOverrideSettings.isAdminOverrideTwoFactorEnabled()))
        .thenReturn(true);

    assertThat(twoFactorAuthenticationManager.overrideTwoFactorAuthentication(
                   account.getUuid(), twoFactorAdminOverrideSettings))
        .isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotEnableTwoFactorAuthenticationForCommunityAccount() {
    Account account = getAccount(AccountType.COMMUNITY, false);
    TwoFactorAdminOverrideSettings twoFactorAdminOverrideSettings = new TwoFactorAdminOverrideSettings(true);
    when(accountService.isCommunityAccount(account.getUuid())).thenReturn(true);

    try {
      twoFactorAuthenticationManager.overrideTwoFactorAuthentication(account.getUuid(), twoFactorAdminOverrideSettings);
      Assert.fail();
    } catch (WingsException e) {
      assertEquals(ErrorCode.GENERAL_ERROR, e.getCode());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForNoAdminEnforce() {
    Account account = getAccount(AccountType.PAID, false);

    // Original user object
    User user = getUser(true);
    user.setAccounts(Arrays.asList(account));

    // Updated user object
    User updatedUser = getUser(false);
    updatedUser.setAccounts(Arrays.asList(account));

    // Should allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(updatedUser.isTwoFactorAuthenticationEnabled()).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForAdminEnforce() {
    Account account = getAccount(AccountType.PAID, true);

    User user = getUser(true);
    user.setAccounts(Arrays.asList(account));

    // Should not allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(user.isTwoFactorAuthenticationEnabled()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForMultiAccounts() {
    Account account1 = getAccount(AccountType.PAID, false);
    Account account2 = getAccount(AccountType.PAID, false);

    User user = getUser(true);
    user.setAccounts(Arrays.asList(account1, account2));

    // Should allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(user.isTwoFactorAuthenticationEnabled()).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDisableTwoFactorAuthenticationForNoAccounts() {
    User user = getUser(true);
    user.setAccounts(null);

    // Should allow disable
    twoFactorAuthenticationManager.disableTwoFactorAuthentication(user);
    assertThat(user.isTwoFactorAuthenticationEnabled()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotEnableTwoFactorAuthenticationForUserWhosePrimaryAccountIsCommunity() {
    Account account1 = getAccount(AccountType.COMMUNITY, false);
    Account account2 = getAccount(AccountType.PAID, false);

    User user = getUser(false);
    user.setAccounts(Arrays.asList(account1, account2));

    when(accountService.isCommunityAccount(account1.getUuid())).thenReturn(true);
    when(accountService.isCommunityAccount(account2.getUuid())).thenReturn(false);

    TwoFactorAuthenticationSettings settings =
        new TwoFactorAuthenticationSettingsBuilder().twoFactorAuthenticationEnabled(true).mechanism(TOTP).build();

    try {
      twoFactorAuthenticationManager.enableTwoFactorAuthenticationSettings(user, settings);
      Assert.fail();
    } catch (WingsException e) {
      assertEquals(ErrorCode.GENERAL_ERROR, e.getCode());
    }
  }

  private Account getAccount(String accountType, boolean twoFactorAdminEnforced) {
    Account account = anAccount().withUuid(UUID.randomUUID().toString()).withAccountName("Harness").build();
    account.setTwoFactorAdminEnforced(twoFactorAdminEnforced);
    LicenseInfo license = getLicenseInfo();
    license.setAccountType(accountType);
    account.setLicenseInfo(license);

    return account;
  }

  private User getUser(boolean twoFactorEnabled) {
    User user = spy(new User());
    user.setTwoFactorAuthenticationEnabled(twoFactorEnabled);
    user.setTwoFactorAuthenticationMechanism(TOTP);
    return user;
  }
}