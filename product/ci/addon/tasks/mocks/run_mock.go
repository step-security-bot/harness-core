// Code generated by MockGen. DO NOT EDIT.
// Source: run.go

// Package steps is a generated GoMock package.
package steps

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
)

// MockRunTask is a mock of RunTask interface.
type MockRunTask struct {
	ctrl     *gomock.Controller
	recorder *MockRunTaskMockRecorder
}

// MockRunTaskMockRecorder is the mock recorder for MockRunTask.
type MockRunTaskMockRecorder struct {
	mock *MockRunTask
}

// NewMockRunTask creates a new mock instance.
func NewMockRunTask(ctrl *gomock.Controller) *MockRunTask {
	mock := &MockRunTask{ctrl: ctrl}
	mock.recorder = &MockRunTaskMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockRunTask) EXPECT() *MockRunTaskMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockRunTask) Run(ctx context.Context) (map[string]string, int32, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx)
	ret0, _ := ret[0].(map[string]string)
	ret1, _ := ret[1].(int32)
	ret2, _ := ret[2].(error)
	return ret0, ret1, ret2
}

// Run indicates an expected call of Run.
func (mr *MockRunTaskMockRecorder) Run(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockRunTask)(nil).Run), ctx)
}
