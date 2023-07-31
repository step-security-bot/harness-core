/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model.customObjects;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PrimitiveCustomObject implements CustomObject {
  private Object value;

  @Override
  public CustomObject getNode(String fieldName) {
    return null;
  }

  @Override
  public void setNode(String fieldName, Object object) {}
}
