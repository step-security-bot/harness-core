package io.harness.ccm.rbac;

public interface CCMRbacPermissions {
  String PERSPECTIVE_CREATE_AND_EDIT = "ccm_perspective_edit";
  String PERSPECTIVE_VIEW = "ccm_perspective_view";
  String PERSPECTIVE_DELETE = "ccm_perspective_delete";

  String BUDGET_CREATE_AND_EDIT = "ccm_budget_edit";
  String BUDGET_VIEW = "ccm_budget_view";
  String BUDGET_DELETE = "ccm_budget_delete";

  String FOLDER_CREATE_AND_EDIT = "ccm_folder_edit";
  String FOLDER_VIEW = "ccm_folder_view";
  String FOLDER_DELETE = "ccm_folder_delete";

  String COST_CATEGORY_CREATE_AND_EDIT = "ccm_costCategory_edit";
  String COST_CATEGORY_VIEW = "ccm_costCategory_view";
  String COST_CATEGORY_DELETE = "ccm_costCategory_delete";

  String COST_OVERVIEW_VIEW = "ccm_overview_view";
}
