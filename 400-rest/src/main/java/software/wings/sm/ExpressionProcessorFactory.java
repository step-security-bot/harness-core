/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

/**
 *
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;

/**
 * A factory for creating ExpressionProcessor objects.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ExpressionProcessorFactory {
  /**
   * Gets the expression processor.
   *
   * @param expression the expression
   * @param context    the context
   * @return the expression processor
   */
  ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context);

  /**
   * Gets the expression processors.
   *
   * @param executionContext the execution context
   * @return the expression processors
   */
  List<ExpressionProcessor> getExpressionProcessors(ExecutionContext executionContext);
}
