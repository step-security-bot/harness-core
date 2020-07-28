package executor

import (
	"context"
	"encoding/base64"
	"fmt"

	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	cengine "github.com/wings-software/portal/product/ci/engine/grpc/client"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	newAddonClient      = caddon.NewAddonClient
	newLiteEngineClient = cengine.NewLiteEngineClient
)

// StageExecutor represents an interface to execute a stage
type StageExecutor interface {
	Run() error
}

// NewStageExecutor creates a stage executor
func NewStageExecutor(encodedStage, stepLogPath, tmpFilePath string, workerPorts []uint,
	log *zap.SugaredLogger) StageExecutor {
	unitExecutor := NewUnitExecutor(stepLogPath, tmpFilePath, log)
	parallelExecutor := NewParallelExecutor(stepLogPath, tmpFilePath, workerPorts, log)
	return &stageExecutor{
		encodedStage:     encodedStage,
		stepLogPath:      stepLogPath,
		tmpFilePath:      tmpFilePath,
		workerPorts:      workerPorts,
		unitExecutor:     unitExecutor,
		parallelExecutor: parallelExecutor,
		log:              log,
	}
}

type stageExecutor struct {
	log              *zap.SugaredLogger
	stepLogPath      string // File path to store logs of steps
	tmpFilePath      string // File path to store generated temporary files
	encodedStage     string // Stage in base64 encoded format
	workerPorts      []uint // GRPC server ports for worker lite-engines that are used for parallel steps
	unitExecutor     UnitExecutor
	parallelExecutor ParallelExecutor
}

// Executes steps in a stage
func (e *stageExecutor) Run() error {
	ctx := context.Background()
	defer e.stopAddonServer(ctx)
	for _, port := range e.workerPorts {
		defer e.stopWorkerLiteEngine(ctx, port)
	}

	execution, err := e.decodeStage(e.encodedStage)
	if err != nil {
		return err
	}

	for _, step := range execution.GetSteps() {
		switch x := step.GetStep().(type) {
		case *pb.Step_Unit:
			err := e.unitExecutor.Run(ctx, step.GetUnit())
			if err != nil {
				return err
			}
		case *pb.Step_Parallel:
			err := e.parallelExecutor.Run(ctx, step.GetParallel())
			if err != nil {
				return err
			}
		default:
			return fmt.Errorf("Step has unexpected type %T", x)
		}
	}
	return nil
}

// Stop CI-Addon GRPC server
func (e *stageExecutor) stopAddonServer(ctx context.Context) error {
	addonClient, err := newAddonClient(caddon.AddonPort, e.log)
	if err != nil {
		return errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	_, err = addonClient.Client().SignalStop(ctx, &addonpb.SignalStopRequest{})
	if err != nil {
		e.log.Warnw("Unable to send Stop server request", "error_msg", zap.Error(err))
		return errors.Wrap(err, "Could not send stop server request")
	}
	return nil
}

// Stop worker Lite-engine running on a port
func (e *stageExecutor) stopWorkerLiteEngine(ctx context.Context, port uint) error {
	c, err := newLiteEngineClient(port, e.log)
	if err != nil {
		return errors.Wrap(err, "Could not create CI Lite engine client")
	}
	defer c.CloseConn()

	_, err = c.Client().SignalStop(ctx, &pb.SignalStopRequest{})
	if err != nil {
		e.log.Warnw("Unable to send Stop server request", "error_msg", zap.Error(err))
		return errors.Wrap(err, "Could not send stop server request")
	}
	return nil
}

func (e *stageExecutor) decodeStage(encodedStage string) (*pb.Execution, error) {
	decodedStage, err := base64.StdEncoding.DecodeString(e.encodedStage)
	if err != nil {
		e.log.Errorw("Failed to decode stage", "encoded_stage", e.encodedStage, zap.Error(err))
		return nil, err
	}

	execution := &pb.Execution{}
	err = proto.Unmarshal(decodedStage, execution)
	if err != nil {
		e.log.Errorw("Failed to deserialize stage", "decoded_stage", decodedStage, zap.Error(err))
		return nil, err
	}

	e.log.Infow("Deserialized execution", "execution", execution.String())
	return execution, nil
}

// ExecuteStage executes a stage of the pipeline
func ExecuteStage(input, logpath, tmpFilePath string, workerPorts []uint, log *zap.SugaredLogger) {
	executor := NewStageExecutor(input, logpath, tmpFilePath, workerPorts, log)
	if err := executor.Run(); err != nil {
		log.Fatalw(
			"error while executing steps in a stage",
			"embedded_stage", input,
			"log_path", logpath,
			zap.Error(err),
		)
	}
}
