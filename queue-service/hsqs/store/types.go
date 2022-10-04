// Copyright 2022 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Package store package created to store all data types used by hsqs
package store

import (
	"time"
)

// Request and Response Objects for HSQS API's

// RegisterTopicMetadata Request object for Registering Topic Metadata
type RegisterTopicMetadata struct {
	Topic      string
	MaxRetries int
	// time in nanoseconds
	MaxProcessingTime      time.Duration
	MaxUnProcessedMessages int
}

// EnqueueRequest Request object for Enqueuing messages
type EnqueueRequest struct {
	Topic        string `json:"topic"`
	SubTopic     string `json:"subTopic"`
	Payload      []byte `json:"payload"`
	ProducerName string `json:"producerName"`
}

// EnqueueResponse Response object for Enqueuing messages
type EnqueueResponse struct {
	// ItemID is the identifier of the task in the Queue
	ItemID string `json:"itemId"`
}

// EnqueueErrorResponse Error Mesaage object for Enqueuing messages
type EnqueueErrorResponse struct {
	ErrorMessage string
}

// DequeueRequest Request object for Dequeuing messages
type DequeueRequest struct {
	Topic        string
	BatchSize    int
	ConsumerName string
}

// DequeueResponse Response object for Dequeuing messages
type DequeueResponse struct {
	ItemID       string              `json:"itemId"`
	Timestamp    int64               `json:"timeStamp"`
	Payload      []byte              `json:"payload"`
	QueueKey     string              `json:"queueKey"`
	ItemMetadata DequeueItemMetadata `json:"metadata"`
}

// DequeueItemMetadata DequeuingItem metadata request
type DequeueItemMetadata struct {
	CurrentRetryCount int
	MaxProcessingTime float64
}

// DequeueErrorResponse Error Response object for Dequeue Request response
type DequeueErrorResponse struct {
	ErrorMessage string
}

// AckRequest Request object for Acknowledging a message
type AckRequest struct {
	ItemID       string
	Topic        string
	SubTopic     string
	ConsumerName string
}

// AckResponse Response object for Acknowledging a message
type AckResponse struct {
	ItemID string
}

// AckErrorResponse Error Response object for Acknowledging a message
type AckErrorResponse struct {
	ErrorMessage string
}

type UnAckType int

const (
	UnAckTopic UnAckType = iota + 1
	UnAckItem
)

func (u UnAckType) String() string {
	return []string{"UnAckTopic", "UnAckItem"}[u]
}

// UnAckRequest Request object for UnAck a message
type UnAckRequest struct {
	ItemID   string
	Topic    string
	SubTopic string
	// Retry topic + subtopic after RetryAfterTimeDuration nanoseconds
	RetryAfterTimeDuration time.Duration
	Type                   UnAckType
}

// UnAckResponse Response object for UnAck a message
type UnAckResponse struct {
	ItemID   string
	Topic    string
	SubTopic string
	Type     UnAckType
}

// UnAckErrorResponse Response object for UnAck a message
type UnAckErrorResponse struct {
	ErrorMessage string
}
