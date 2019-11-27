package software.wings.service.impl.ldap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import javax.validation.constraints.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapGetUsersRequest extends AbstractLdapRequest {
  LdapSearch ldapSearch;
  String groupBaseDn;

  public LdapGetUsersRequest(@NotNull final LdapUserConfig ldapUserConfig, @NotNull final LdapSearch ldapSearch,
      int responseTimeoutInSeconds, String groupBaseDn) {
    super(ldapUserConfig, responseTimeoutInSeconds);
    this.ldapSearch = ldapSearch;
    this.groupBaseDn = groupBaseDn;
  }
}
