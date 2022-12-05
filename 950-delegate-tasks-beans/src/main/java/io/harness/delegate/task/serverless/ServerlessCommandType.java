/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

public enum ServerlessCommandType {
  SERVERLESS_AWS_LAMBDA_DEPLOY,
  SERVERLESS_AWS_LAMBDA_ROLLBACK,
  SERVERLESS_AWS_LAMBDA_PREPARE_ROLLBACK,
  SERVERLESS_S3_FETCH_TASK_NG
}
