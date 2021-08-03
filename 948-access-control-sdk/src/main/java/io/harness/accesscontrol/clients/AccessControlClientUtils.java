package io.harness.accesscontrol.clients;

import static io.harness.security.dto.PrincipalType.SERVICE;

import io.harness.accesscontrol.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class AccessControlClientUtils {
  private static boolean serviceContextAndNoPrincipalInBody(
      io.harness.security.dto.Principal principalInContext, Principal principalToCheckPermissions) {
    Optional<io.harness.security.dto.Principal> serviceCall =
        Optional.ofNullable(principalInContext).filter(x -> SERVICE.equals(x.getType()));

    return serviceCall.isPresent()
        && (principalToCheckPermissions == null
            || Objects.equals(serviceCall.get().getName(), principalToCheckPermissions.getPrincipalIdentifier()));
  }

  private static boolean userContextAndDifferentPrincipalInBody(
      io.harness.security.dto.Principal principalInContext, Principal principalToCheckPermissions) {
    /* check if a principal of type other than SERVICE is trying to check permissions for any
       other principal */
    Optional<io.harness.security.dto.Principal> nonServiceCall =
        Optional.ofNullable(principalInContext).filter(x -> !SERVICE.equals(x.getType()));
    return nonServiceCall.isPresent()
        && (principalToCheckPermissions != null
            && !Objects.equals(principalInContext.getName(), principalToCheckPermissions.getPrincipalIdentifier()));
  }

  private static boolean checkForValidContext(io.harness.security.dto.Principal principalInContext) {
    return principalInContext != null && principalInContext.getName() != null && principalInContext.getType() != null;
  }

  public static AccessControlDTO getAccessControlDTO(PermissionCheckDTO permissionCheckDTO, boolean permitted) {
    return AccessControlDTO.builder()
        .permission(permissionCheckDTO.getPermission())
        .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
        .resourceScope(permissionCheckDTO.getResourceScope())
        .resourceType(permissionCheckDTO.getResourceType())
        .permitted(permitted)
        .build();
  }

  public static boolean checkPreconditions(
      io.harness.security.dto.Principal contextPrincipal, Principal principalToCheckPermissionsFor) {
    return true;
    /*
    boolean validContext = checkForValidContext(contextPrincipal);
    return validContext
        && (serviceContextAndNoPrincipalInBody(contextPrincipal, principalToCheckPermissionsFor)
            || !userContextAndDifferentPrincipalInBody(contextPrincipal, principalToCheckPermissionsFor));

     */
  }
}
