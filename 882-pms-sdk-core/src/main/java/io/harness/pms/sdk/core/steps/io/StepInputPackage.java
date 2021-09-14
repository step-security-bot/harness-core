/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.steps.io;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.pms.sdk.core.data.StepTransput;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class StepInputPackage {
  @Singular List<ResolvedRefInput> inputs;

  public List<StepTransput> findByRefKey(String refKey) {
    if (isEmpty(inputs)) {
      return Collections.emptyList();
    }
    return inputs.stream()
        .filter(resolvedRefInput -> resolvedRefInput.getRefObject().getKey().equals(refKey))
        .map(ResolvedRefInput::getTransput)
        .collect(Collectors.toList());
  }
}
