package io.harness.yaml.core.variables;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.walktree.visitor.Visitable;

@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
public interface NGVariable extends Visitable {
  NGVariableType getType();
  String getName();
  String getDescription();
  boolean isRequired();
}
