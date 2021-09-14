/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.jira.deserializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.JiraProjectBasicNG;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

@OwnedBy(CDC)
public class JiraProjectBasicDeserializer extends StdDeserializer<JiraProjectBasicNG> {
  public JiraProjectBasicDeserializer() {
    this(null);
  }

  public JiraProjectBasicDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JiraProjectBasicNG deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);
    return new JiraProjectBasicNG(node);
  }
}
