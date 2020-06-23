// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/engine/proto/execution.proto

package io.harness.product.ci.engine.proto;

@javax.annotation.Generated(value = "protoc", comments = "annotations:ExecutionOuterClass.java.pb.meta")
public final class ExecutionOuterClass {
  private ExecutionOuterClass() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_product_ci_engine_proto_Execution_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_product_ci_engine_proto_Execution_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_product_ci_engine_proto_StepContext_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_product_ci_engine_proto_StepContext_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_product_ci_engine_proto_RunStep_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_product_ci_engine_proto_RunStep_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_product_ci_engine_proto_SaveCacheStep_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_product_ci_engine_proto_SaveCacheStep_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_product_ci_engine_proto_Step_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_product_ci_engine_proto_Step_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n\'product/ci/engine/proto/execution.prot"
        + "o\022\"io.harness.product.ci.engine.proto\"K\n"
        + "\tExecution\022>\n\005steps\030\001 \003(\0132(.io.harness.p"
        + "roduct.ci.engine.proto.StepR\005steps\"d\n\013St"
        + "epContext\022\037\n\013num_retries\030\001 \001(\005R\nnumRetri"
        + "es\0224\n\026execution_timeout_secs\030\002 \001(\003R\024exec"
        + "utionTimeoutSecs\"p\n\007RunStep\022\032\n\010commands\030"
        + "\001 \003(\tR\010commands\022I\n\007context\030\002 \001(\0132/.io.ha"
        + "rness.product.ci.engine.proto.StepContex"
        + "tR\007context\"7\n\rSaveCacheStep\022\020\n\003key\030\001 \001(\t"
        + "R\003key\022\024\n\005paths\030\002 \003(\tR\005paths\"\326\001\n\004Step\022\016\n\002"
        + "id\030\001 \001(\tR\002id\022!\n\014display_name\030\002 \001(\tR\013disp"
        + "layName\022?\n\003run\030\003 \001(\0132+.io.harness.produc"
        + "t.ci.engine.proto.RunStepH\000R\003run\022R\n\nsave"
        + "_cache\030\004 \001(\01321.io.harness.product.ci.eng"
        + "ine.proto.SaveCacheStepH\000R\tsaveCacheB\006\n\004"
        + "stepB\016P\001Z\ncienginepbb\006proto3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
        descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
    internal_static_io_harness_product_ci_engine_proto_Execution_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_product_ci_engine_proto_Execution_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_product_ci_engine_proto_Execution_descriptor,
            new java.lang.String[] {
                "Steps",
            });
    internal_static_io_harness_product_ci_engine_proto_StepContext_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_product_ci_engine_proto_StepContext_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_product_ci_engine_proto_StepContext_descriptor,
            new java.lang.String[] {
                "NumRetries",
                "ExecutionTimeoutSecs",
            });
    internal_static_io_harness_product_ci_engine_proto_RunStep_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_product_ci_engine_proto_RunStep_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_product_ci_engine_proto_RunStep_descriptor,
            new java.lang.String[] {
                "Commands",
                "Context",
            });
    internal_static_io_harness_product_ci_engine_proto_SaveCacheStep_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_product_ci_engine_proto_SaveCacheStep_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_product_ci_engine_proto_SaveCacheStep_descriptor,
            new java.lang.String[] {
                "Key",
                "Paths",
            });
    internal_static_io_harness_product_ci_engine_proto_Step_descriptor = getDescriptor().getMessageTypes().get(4);
    internal_static_io_harness_product_ci_engine_proto_Step_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_product_ci_engine_proto_Step_descriptor,
            new java.lang.String[] {
                "Id",
                "DisplayName",
                "Run",
                "SaveCache",
                "Step",
            });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
