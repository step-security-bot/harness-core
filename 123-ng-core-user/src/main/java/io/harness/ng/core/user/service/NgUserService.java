package io.harness.ng.core.user.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.dto.RoleBinding;
import io.harness.ng.core.user.AddUsersDTO;
import io.harness.ng.core.user.AddUsersResponse;
import io.harness.ng.core.user.NGRemoveUserFilter;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.scim.PatchRequest;
import io.harness.scim.ScimListResponse;
import io.harness.scim.ScimUser;
import io.harness.user.remote.UserFilterNG;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface NgUserService {
  void addUserToCG(String userId, Scope scope);

  Optional<UserInfo> getUserById(String userId);

  Optional<UserMetadataDTO> getUserByEmail(String emailId, boolean fetchFromCurrentGen);

  Optional<UserMetadataDTO> getUserMetadata(String userId);

  AddUsersResponse addUsers(Scope scope, AddUsersDTO addUsersDTO);

  /**
   * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
   */
  Page<UserInfo> listCurrentGenUsers(String accountIdentifier, String searchString, Pageable page);

  List<UserInfo> listCurrentGenUsers(String accountId, UserFilterNG userFilter);

  ScimListResponse<ScimUser> searchScimUsersByEmailQuery(
      String accountId, String searchQuery, Integer count, Integer startIndex);

  List<UserMetadataDTO> listUsersHavingRole(Scope scope, String roleIdentifier);

  Optional<UserMembership> getUserMembership(String userId, Scope scope);

  /**
   * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
   */
  PageResponse<UserMetadataDTO> listUsers(Scope scope, PageRequest pageRequest, UserFilter userFilter);

  List<String> listUserIds(Scope scope);

  List<UserMetadataDTO> listUsers(Scope scope);

  List<UserMetadataDTO> getUserMetadata(List<String> userIds);

  void addServiceAccountToScope(
      String serviceAccountId, Scope scope, String roleIdentifier, UserMembershipUpdateSource source);

  Page<UserMembership> listUserMemberships(Criteria criteria, Pageable pageable);

  void addUserToScope(String userId, Scope scope, List<RoleBinding> roleBindings, List<String> userGroups,
      UserMembershipUpdateSource source);

  boolean isUserAtScope(String userId, Scope scope);

  boolean isUserLastAdminAtScope(String userId, Scope scope);

  boolean updateUserMetadata(UserMetadataDTO user);

  boolean removeUserFromScope(
      String userId, Scope scope, UserMembershipUpdateSource source, NGRemoveUserFilter removeUserFilter);

  boolean removeUserWithCriteria(String userId, UserMembershipUpdateSource source, Criteria criteria);

  boolean isUserPasswordSet(String accountIdentifier, String email);

  List<String> listUserAccountIds(String userId);

  boolean removeUser(String userId, String accountId);

  ScimUser updateScimUser(String accountId, String userId, PatchRequest patchRequest);

  boolean updateScimUser(String accountId, String userId, ScimUser scimUser);

  boolean updateUserDisabled(String accountId, String userId, boolean disabled);
}
