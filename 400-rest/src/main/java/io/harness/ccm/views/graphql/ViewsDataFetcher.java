package io.harness.ccm.views.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.views.service.CEViewService;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;

@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class ViewsDataFetcher extends AbstractObjectDataFetcher<QLCEViewsData, QLNoOpQueryParameters> {
  @Inject private CEViewService viewService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEViewsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    return QLCEViewsData.builder().customerViews(viewService.getAllViews(accountId, false)).build();
  }
}
