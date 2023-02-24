/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws.sam.command;

public enum AwsSamCommandUnitConstants {
  fetchFiles {
    @Override
    public String toString() {
      return "Fetch Files";
    }
  },
  setupDirectory {
    @Override
    public String toString() {
      return "Setup AWS SAM Directory";
    }
  },
  configureCred {
    @Override
    public String toString() {
      return "Configure Credentials";
    }
  },
  publish {
    @Override
    public String toString() {
      return "Publish";
    }
  },
  validateBuildPackage {
    @Override
    public String toString() {
      return "Validate Build Package";
    }
  }
}
