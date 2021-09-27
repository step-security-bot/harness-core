package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.state.inspection.ExpressionVariableUsage;

import software.wings.beans.GraphNode;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@OwnedBy(CDC)
@Value
@Builder
public class GraphNodeMetadata implements GraphNodeVisitable, ExecutionDetailsMetadata {
  // id is used for post-processing and getting execution data.
  @Getter @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String id;

  String name;
  String type;
  boolean rollback;
  ExecutionStatus status;

  boolean valid;
  String validationMessage;

  InstanceCountMetadata instanceCount;
  Map<String, Object> executionDetails;

  // activityId is used to fill up subCommands later when we want to query execution logs.
  @Getter @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String activityId;
  @NonFinal @Setter List<ActivityCommandUnitMetadata> subCommands;

  // executionContext contain variables used at runtime and their values.
  @Getter @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) boolean hasInspection;
  @NonFinal @Setter List<ExpressionVariableUsage.Item> executionContext;

  // interruptHistory contains the interrupt history.
  @Getter @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) int interruptHistoryCount;
  @NonFinal @Setter List<ExecutionInterruptMetadata> interruptHistory;

  // executionHistory contains the execution history like retries.
  @Getter @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) int executionHistoryCount;
  @NonFinal @Setter List<ExecutionHistoryMetadata> executionHistory;

  GraphGroupMetadata subGraph;

  TimingMetadata timing;

  public void accept(GraphNodeVisitor visitor) {
    visitor.visitGraphNode(this);
    if (subGraph != null) {
      subGraph.accept(visitor);
    }
  }

  static List<GraphNodeMetadata> fromOriginGraphNode(GraphNode origin) {
    List<GraphNodeMetadata> graphNodes = new ArrayList<>();
    addSiblingGraphNodes(origin, graphNodes);
    return nullIfEmpty(graphNodes);
  }

  private static void addSiblingGraphNodes(GraphNode node, @NotNull List<GraphNodeMetadata> graphNodes) {
    if (node == null) {
      return;
    }

    GraphNodeMetadataBuilder nodeMetadataBuilder = GraphNodeMetadata.builder()
                                                       .id(node.getId())
                                                       .name(node.getName())
                                                       .type(node.getType())
                                                       .status(extractExecutionStatus(node))
                                                       .rollback(node.isRollback())
                                                       .valid(node.isValid())
                                                       .validationMessage(node.getValidationMessage())
                                                       .hasInspection(node.isHasInspection())
                                                       .interruptHistoryCount(node.getInterruptHistoryCount())
                                                       .executionHistoryCount(node.getExecutionHistoryCount())
                                                       .subGraph(GraphGroupMetadata.fromGraphGroup(node.getGroup()));
    updateWithExecutionDetails(node, nodeMetadataBuilder);
    GraphNodeMetadata nodeMetadata = nodeMetadataBuilder.build();

    graphNodes.add(nodeMetadata);
    addSiblingGraphNodes(node.getNext(), graphNodes);
  }

  private static void updateWithExecutionDetails(
      @NotNull GraphNode node, @NotNull GraphNodeMetadataBuilder nodeMetadataBuilder) {
    ExecutionDetailsInternalMetadata executionDetailsInternalMetadata =
        ExecutionDetailsInternalMetadata.fromGraphNode(node);
    if (executionDetailsInternalMetadata == null) {
      return;
    }

    nodeMetadataBuilder.instanceCount(executionDetailsInternalMetadata.getInstanceCount())
        .executionDetails(executionDetailsInternalMetadata.getExecutionDetails())
        .activityId(executionDetailsInternalMetadata.getActivityId())
        .timing(executionDetailsInternalMetadata.getTiming());
  }

  private static ExecutionStatus extractExecutionStatus(@NotNull GraphNode node) {
    try {
      return ExecutionStatus.valueOf(node.getStatus());
    } catch (Exception ex) {
      return null;
    }
  }
}
