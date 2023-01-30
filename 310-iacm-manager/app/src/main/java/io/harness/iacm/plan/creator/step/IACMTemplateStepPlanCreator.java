/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.iacm.plan.creator.step;

import io.harness.beans.steps.nodes.iacm.IACMTemplateStepNode;
import io.harness.beans.steps.stepinfo.IACMStepInfoType;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class IACMTemplateStepPlanCreator extends CIPMSStepPlanCreatorV2<IACMTemplateStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(IACMStepInfoType.IACM_TERRAFORM_PLAN.getDisplayName());
  }

  @Override
  public Class<IACMTemplateStepNode> getFieldClass() {
    return IACMTemplateStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, IACMTemplateStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
