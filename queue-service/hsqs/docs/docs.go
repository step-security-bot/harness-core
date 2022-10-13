// Copyright 2022 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package docs

import "github.com/swaggo/swag"

const docTemplate = `{
    "schemes": {{ marshal .Schemes }},
    "swagger": "2.0",
    "info": {
        "description": "{{escape .Description}}",
        "title": "{{.Title}}",
        "termsOfService": "http://swagger.io/terms/",
        "contact": {
            "name": "API Support",
            "url": "http://www.swagger.io/support",
            "email": "support@swagger.io"
        },
        "license": {
            "name": "Apache 2.0",
            "url": "http://www.apache.org/licenses/LICENSE-2.0.html"
        },
        "version": "{{.Version}}"
    },
    "host": "{{.Host}}",
    "basePath": "{{.BasePath}}",
    "paths": {
        "/v1/ack": {
            "post": {
                "description": "Ack a Redis message consumed successfully",
                "consumes": [
                    "application/json"
                ],
                "produces": [
                    "application/json"
                ],
                "summary": "Ack a Redis message",
                "parameters": [
                    {
                        "description": "query params",
                        "name": "request",
                        "in": "body",
                        "required": true,
                        "schema": {
                            "$ref": "#/definitions/store.AckRequest"
                        }
                    },
                    {
                        "type": "string",
                        "description": "Authorization",
                        "name": "Authorization",
                        "in": "header",
                        "required": true
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/store.AckResponse"
                        }
                    }
                }
            }
        },
        "/v1/dequeue": {
            "post": {
                "description": "Dequeue a request",
                "consumes": [
                    "application/json"
                ],
                "produces": [
                    "application/json"
                ],
                "summary": "Dequeue in Redis",
                "parameters": [
                    {
                        "description": "query params",
                        "name": "request",
                        "in": "body",
                        "required": true,
                        "schema": {
                            "$ref": "#/definitions/store.DequeueRequest"
                        }
                    },
                    {
                        "type": "string",
                        "description": "Authorization",
                        "name": "Authorization",
                        "in": "header",
                        "required": true
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/store.DequeueResponse"
                        }
                    }
                }
            }
        },
        "/v1/queue": {
            "post": {
                "description": "Enqueue the request",
                "consumes": [
                    "application/json"
                ],
                "produces": [
                    "application/json"
                ],
                "summary": "Enqueue",
                "parameters": [
                    {
                        "description": "query params",
                        "name": "request",
                        "in": "body",
                        "required": true,
                        "schema": {
                            "$ref": "#/definitions/store.EnqueueRequest"
                        }
                    },
                    {
                        "type": "string",
                        "description": "Authorization",
                        "name": "Authorization",
                        "in": "header",
                        "required": true
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/store.EnqueueResponse"
                        }
                    }
                }
            }
        },
        "/v1/unack": {
            "post": {
                "description": "UnAck a Redis message or SubTopic to stop processing",
                "consumes": [
                    "application/json"
                ],
                "produces": [
                    "application/json"
                ],
                "summary": "UnAck a Redis message or SubTopic",
                "parameters": [
                    {
                        "description": "query params",
                        "name": "request",
                        "in": "body",
                        "required": true,
                        "schema": {
                            "$ref": "#/definitions/store.UnAckRequest"
                        }
                    },
                    {
                        "type": "string",
                        "description": "Authorization",
                        "name": "Authorization",
                        "in": "header",
                        "required": true
                    }
                ],
                "responses": {
                    "200": {
                        "description": "OK",
                        "schema": {
                            "$ref": "#/definitions/store.UnAckResponse"
                        }
                    }
                }
            }
        }
    },
    "definitions": {
        "store.AckRequest": {
            "type": "object",
            "properties": {
                "consumerName": {
                    "type": "string"
                },
                "itemID": {
                    "type": "string"
                },
                "subTopic": {
                    "type": "string"
                },
                "topic": {
                    "type": "string"
                }
            }
        },
        "store.AckResponse": {
            "type": "object",
            "properties": {
                "itemID": {
                    "type": "string"
                }
            }
        },
        "store.DequeueItemMetadata": {
            "type": "object",
            "properties": {
                "currentRetryCount": {
                    "type": "integer"
                },
                "maxProcessingTime": {
                    "type": "number"
                }
            }
        },
        "store.DequeueRequest": {
            "type": "object",
            "properties": {
                "batchSize": {
                    "type": "integer"
                },
                "consumerName": {
                    "type": "string"
                },
                "maxWaitDuration": {
                    "type": "integer"
                },
                "topic": {
                    "type": "string"
                }
            }
        },
        "store.DequeueResponse": {
            "type": "object",
            "properties": {
                "itemId": {
                    "type": "string"
                },
                "metadata": {
                    "$ref": "#/definitions/store.DequeueItemMetadata"
                },
                "payload": {
                    "type": "array",
                    "items": {
                        "type": "integer"
                    }
                },
                "queueKey": {
                    "type": "string"
                },
                "timeStamp": {
                    "type": "integer"
                }
            }
        },
        "store.EnqueueRequest": {
            "type": "object",
            "properties": {
                "payload": {
                    "type": "array",
                    "items": {
                        "type": "integer"
                    }
                },
                "producerName": {
                    "type": "string"
                },
                "subTopic": {
                    "type": "string"
                },
                "topic": {
                    "type": "string"
                }
            }
        },
        "store.EnqueueResponse": {
            "type": "object",
            "properties": {
                "itemId": {
                    "description": "ItemID is the identifier of the task in the Queue",
                    "type": "string"
                }
            }
        },
        "store.UnAckRequest": {
            "type": "object",
            "properties": {
                "itemID": {
                    "type": "string"
                },
                "retryTimeAfterDuration": {
                    "description": "Retry topic + subtopic after RetryAfterTimeDuration nanoseconds",
                    "type": "integer"
                },
                "subTopic": {
                    "type": "string"
                },
                "topic": {
                    "type": "string"
                },
                "type": {
                    "type": "integer"
                }
            }
        },
        "store.UnAckResponse": {
            "type": "object",
            "properties": {
                "itemID": {
                    "type": "string"
                },
                "subTopic": {
                    "type": "string"
                },
                "topic": {
                    "type": "string"
                },
                "type": {
                    "type": "integer"
                }
            }
        }
    }
}`

// SwaggerInfo holds exported Swagger Info so clients can modify it
var SwaggerInfo = &swag.Spec{
	Version:          "1.0",
	Host:             "localhost:9091",
	BasePath:         "/",
	Schemes:          []string{"http"},
	Title:            "Swagger Doc- hsqs",
	Description:      "This is a queuing client.",
	InfoInstanceName: "swagger",
	SwaggerTemplate:  docTemplate,
}

func init() {
	swag.Register(SwaggerInfo.InstanceName(), SwaggerInfo)
}
