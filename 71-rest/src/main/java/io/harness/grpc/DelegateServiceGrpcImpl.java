package io.harness.grpc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.CreatePerpetualTaskRequest;
import io.harness.delegate.CreatePerpetualTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceImplBase;
import io.harness.delegate.DeletePerpetualTaskRequest;
import io.harness.delegate.DeletePerpetualTaskResponse;
import io.harness.delegate.Document;
import io.harness.delegate.Documents;
import io.harness.delegate.ExecuteParkedTaskRequest;
import io.harness.delegate.ExecuteParkedTaskResponse;
import io.harness.delegate.FetchParkedTaskStatusRequest;
import io.harness.delegate.FetchParkedTaskStatusResponse;
import io.harness.delegate.ObtainDocumentRequest;
import io.harness.delegate.ObtainDocumentResponse;
import io.harness.delegate.RegisterCallbackRequest;
import io.harness.delegate.RegisterCallbackResponse;
import io.harness.delegate.ResetPerpetualTaskRequest;
import io.harness.delegate.ResetPerpetualTaskResponse;
import io.harness.delegate.SendTaskProgressRequest;
import io.harness.delegate.SendTaskProgressResponse;
import io.harness.delegate.SendTaskStatusRequest;
import io.harness.delegate.SendTaskStatusResponse;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskProgressUpdatesRequest;
import io.harness.delegate.TaskProgressUpdatesResponse;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.DelegateProgressData;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.mongo.SampleEntity.SampleEntityKeys;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContext.PerpetualTaskClientContextBuilder;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCallbackRegistry;

import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.grpc.stub.StreamObserver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.mongodb.morphia.query.Query;

@Singleton
@Slf4j
public class DelegateServiceGrpcImpl extends DelegateServiceImplBase {
  private DelegateCallbackRegistry delegateCallbackRegistry;
  private PerpetualTaskService perpetualTaskService;
  private DelegateService delegateService;
  private KryoSerializer kryoSerializer;
  private HPersistence persistence;

  @Inject
  public DelegateServiceGrpcImpl(DelegateCallbackRegistry delegateCallbackRegistry,
      PerpetualTaskService perpetualTaskService, DelegateService delegateService, KryoSerializer kryoSerializer,
      HPersistence persistence) {
    this.delegateCallbackRegistry = delegateCallbackRegistry;
    this.perpetualTaskService = perpetualTaskService;
    this.delegateService = delegateService;
    this.kryoSerializer = kryoSerializer;
    this.persistence = persistence;
  }

  @Override
  public void submitTask(SubmitTaskRequest request, StreamObserver<SubmitTaskResponse> responseObserver) {
    try {
      String taskId = generateUuid();
      TaskDetails taskDetails = request.getDetails();
      Map<String, String> setupAbstractions = request.getSetupAbstractions().getValuesMap();
      LinkedHashMap<String, String> logAbstractions =
          request.getLogAbstractions() == null || request.getLogAbstractions().getValuesMap() == null
          ? new LinkedHashMap<>()
          : new LinkedHashMap<>(request.getLogAbstractions().getValuesMap());
      List<ExecutionCapability> capabilities = request.getCapabilitiesList()
                                                   .stream()
                                                   .map(capability
                                                       -> (ExecutionCapability) kryoSerializer.asInflatedObject(
                                                           capability.getKryoCapability().toByteArray()))
                                                   .collect(Collectors.toList());
      List<String> taskSelectors =
          request.getSelectorsList().stream().map(TaskSelector::getSelector).collect(Collectors.toList());

      DelegateTask task = DelegateTask.builder()
                              .uuid(taskId)
                              .driverId(request.hasCallbackToken() ? request.getCallbackToken().getToken() : null)
                              .waitId(taskId)
                              .accountId(request.getAccountId().getId())
                              .setupAbstractions(setupAbstractions)
                              .logStreamingAbstractions(logAbstractions)
                              .workflowExecutionId(setupAbstractions.get(DelegateTaskKeys.workflowExecutionId))
                              .executionCapabilities(capabilities)
                              .tags(taskSelectors)
                              .data(TaskData.builder()
                                        .parked(taskDetails.getParked())
                                        .async(taskDetails.getMode() == TaskMode.ASYNC)
                                        .taskType(taskDetails.getType().getType())
                                        .parameters(new Object[] {kryoSerializer.asInflatedObject(
                                            taskDetails.getKryoParameters().toByteArray())})
                                        .timeout(Durations.toMillis(taskDetails.getExecutionTimeout()))
                                        .expressionFunctorToken((int) taskDetails.getExpressionFunctorToken())
                                        .expressions(taskDetails.getExpressionsMap())
                                        .build())
                              .build();

      if (task.getData().isParked()) {
        delegateService.saveDelegateTask(task, DelegateTask.Status.PARKED);
      } else {
        if (task.getData().isAsync()) {
          delegateService.queueTask(task);
        } else {
          delegateService.scheduleSyncTask(task);
        }
      }
      responseObserver.onNext(SubmitTaskResponse.newBuilder()
                                  .setTaskId(TaskId.newBuilder().setId(taskId).build())
                                  .setTotalExpiry(Timestamps.fromMillis(task.getExpiry() + task.getData().getTimeout()))
                                  .build());
      responseObserver.onCompleted();

    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing submit task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void executeParkedTask(
      ExecuteParkedTaskRequest request, StreamObserver<ExecuteParkedTaskResponse> responseObserver) {
    try {
      delegateService.queueParkedTask(request.getAccountId().getId(), request.getTaskId().getId());

      responseObserver.onNext(ExecuteParkedTaskResponse.newBuilder()
                                  .setTaskId(TaskId.newBuilder().setId(request.getTaskId().getId()).build())
                                  .build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing execute parked task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void fetchParkedTaskStatus(
      FetchParkedTaskStatusRequest request, StreamObserver<FetchParkedTaskStatusResponse> responseObserver) {
    try {
      byte[] delegateTaskResults = delegateService.getParkedTaskResults(
          request.getAccountId().getId(), request.getTaskId().getId(), request.getCallbackToken().getToken());
      if (delegateTaskResults.length > 0) {
        responseObserver.onNext(FetchParkedTaskStatusResponse.newBuilder()
                                    .setSerializedTaskResults(ByteString.copyFrom(delegateTaskResults))
                                    .setFetchResults(true)
                                    .build());
      } else {
        responseObserver.onNext(FetchParkedTaskStatusResponse.newBuilder().setFetchResults(false).build());
      }
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing fetch parked task status request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void sendTaskStatus(SendTaskStatusRequest request, StreamObserver<SendTaskStatusResponse> responseObserver) {
    try {
      DelegateTaskResponse delegateTaskResponse =
          DelegateTaskResponse.builder()
              .responseCode(DelegateTaskResponse.ResponseCode.OK)
              .accountId(request.getAccountId().getId())
              .response((DelegateResponseData) kryoSerializer.asInflatedObject(
                  request.getTaskResponseData().getKryoResultsData().toByteArray()))
              .build();
      delegateService.processDelegateResponse(
          request.getAccountId().getId(), null, request.getTaskId().getId(), delegateTaskResponse);
      responseObserver.onNext(SendTaskStatusResponse.newBuilder().setSuccess(true).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing send parked task status request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void sendTaskProgress(
      SendTaskProgressRequest request, StreamObserver<SendTaskProgressResponse> responseObserver) {
    try {
      delegateService.publishTaskProgressResponse(request.getAccountId().getId(), request.getCallbackToken().getToken(),
          request.getTaskId().getId(),
          (DelegateProgressData) kryoSerializer.asInflatedObject(
              request.getTaskResponseData().getKryoResultsData().toByteArray()));
      responseObserver.onNext(SendTaskProgressResponse.newBuilder().setSuccess(true).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing send task progress status request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void cancelTask(CancelTaskRequest request, StreamObserver<CancelTaskResponse> responseObserver) {
    try {
      DelegateTask preAbortedTask =
          delegateService.abortTask(request.getAccountId().getId(), request.getTaskId().getId());
      if (preAbortedTask != null) {
        responseObserver.onNext(
            CancelTaskResponse.newBuilder()
                .setCanceledAtStage(DelegateTaskGrpcUtils.mapTaskStatusToTaskExecutionStage(preAbortedTask.getStatus()))
                .build());
        responseObserver.onCompleted();
        return;
      }

      responseObserver.onNext(
          CancelTaskResponse.newBuilder().setCanceledAtStage(TaskExecutionStage.TYPE_UNSPECIFIED).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing cancel task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void taskProgress(TaskProgressRequest request, StreamObserver<TaskProgressResponse> responseObserver) {
    try {
      Optional<DelegateTask> delegateTaskOptional =
          delegateService.fetchDelegateTask(request.getAccountId().getId(), request.getTaskId().getId());

      if (delegateTaskOptional.isPresent()) {
        responseObserver.onNext(TaskProgressResponse.newBuilder()
                                    .setCurrentlyAtStage(DelegateTaskGrpcUtils.mapTaskStatusToTaskExecutionStage(
                                        delegateTaskOptional.get().getStatus()))
                                    .build());
        responseObserver.onCompleted();
        return;
      }

      responseObserver.onNext(
          TaskProgressResponse.newBuilder().setCurrentlyAtStage(TaskExecutionStage.TYPE_UNSPECIFIED).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing task progress request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void taskProgressUpdates(
      TaskProgressUpdatesRequest request, StreamObserver<TaskProgressUpdatesResponse> responseObserver) {
    throw new NotImplementedException(
        "Temporarily removed the implementation until we find more effective way of doing this.");
  }

  @Override
  public void registerCallback(
      RegisterCallbackRequest request, StreamObserver<RegisterCallbackResponse> responseObserver) {
    try {
      String token = delegateCallbackRegistry.ensureCallback(request.getCallback());
      responseObserver.onNext(RegisterCallbackResponse.newBuilder()
                                  .setCallbackToken(DelegateCallbackToken.newBuilder().setToken(token))
                                  .build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing register callback request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void createPerpetualTask(
      CreatePerpetualTaskRequest request, StreamObserver<CreatePerpetualTaskResponse> responseObserver) {
    try {
      String accountId = request.getAccountId().getId();

      PerpetualTaskClientContextBuilder contextBuilder = PerpetualTaskClientContext.builder();

      if (!request.getClientTaskId().isEmpty()) {
        contextBuilder.clientId(request.getClientTaskId());
      }

      if (request.getContext().hasTaskClientParams()) {
        contextBuilder.clientParams(request.getContext().getTaskClientParams().getParamsMap());
      } else if (request.getContext().hasExecutionBundle()) {
        contextBuilder.executionBundle(request.getContext().getExecutionBundle().toByteArray());
      }

      if (request.getContext().getLastContextUpdated() != null) {
        contextBuilder.lastContextUpdated(Timestamps.toMillis(request.getContext().getLastContextUpdated()));
      }

      String perpetualTaskId = perpetualTaskService.createTask(request.getType(), accountId, contextBuilder.build(),
          request.getSchedule(), request.getAllowDuplicate(), request.getTaskDescription());

      responseObserver.onNext(CreatePerpetualTaskResponse.newBuilder()
                                  .setPerpetualTaskId(PerpetualTaskId.newBuilder().setId(perpetualTaskId).build())
                                  .build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing create perpetual task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void deletePerpetualTask(
      DeletePerpetualTaskRequest request, StreamObserver<DeletePerpetualTaskResponse> responseObserver) {
    try {
      perpetualTaskService.deleteTask(request.getAccountId().getId(), request.getPerpetualTaskId().getId());

      responseObserver.onNext(DeletePerpetualTaskResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing delete perpetual task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void resetPerpetualTask(
      ResetPerpetualTaskRequest request, StreamObserver<ResetPerpetualTaskResponse> responseObserver) {
    try {
      perpetualTaskService.resetTask(
          request.getAccountId().getId(), request.getPerpetualTaskId().getId(), request.getTaskExecutionBundle());

      responseObserver.onNext(ResetPerpetualTaskResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing reset perpetual task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void obtainDocument(ObtainDocumentRequest request, StreamObserver<ObtainDocumentResponse> responseObserver) {
    ObtainDocumentResponse.Builder builder = ObtainDocumentResponse.newBuilder();
    for (Documents documents : request.getDocumentsList()) {
      Query<PersistentEntity> query =
          persistence.createQueryForCollection(documents.getCollectionName(), excludeAuthority)
              .field(SampleEntityKeys.uuid)
              .in(documents.getUuidList());

      try (HIterator<PersistentEntity> iterator = new HIterator(query.fetch())) {
        for (PersistentEntity entity : iterator) {
          builder.addDocuments(Document.newBuilder()
                                   .setCollectionName(documents.getCollectionName())
                                   .setKryoBytes(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(entity)))
                                   .build());
        }
      }
    }
    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }
}
