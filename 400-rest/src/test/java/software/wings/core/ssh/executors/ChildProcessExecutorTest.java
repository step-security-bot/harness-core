/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.core.ssh.executors;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.PRABU;

import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.shell.ChildProcessStopper;

import software.wings.WingsBaseTest;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@OwnedBy(CDC)
public class ChildProcessExecutorTest extends WingsBaseTest {
  private String FILE_NAME = "FILE";
  private File workingDirectory = new File(System.getProperty("user.dir"));
  private ProcessExecutor processExecutor = Mockito.mock(ProcessExecutor.class);
  private ChildProcessStopper childProcessStopper =
      new ChildProcessStopper(FILE_NAME, workingDirectory, processExecutor);

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecuteScript() throws IllegalAccessException, IOException, InterruptedException, TimeoutException {
    Process process = mock(Process.class);
    when(processExecutor.execute()).thenReturn(new ProcessResult(0, null));
    Logger mockLogger = mock(Logger.class);
    setStaticFieldValue(ChildProcessStopper.class, "log", mockLogger);
    childProcessStopper.stop(process);
    Mockito.verify(processExecutor).command("/bin/bash", "kill-" + FILE_NAME);
    Mockito.verify(mockLogger)
        .info("Kill child processes command: list_descendants ()\n"
            + "{\n"
            + "  local -a children=()\n"
            + "  read -ra children <<< \"$(ps -ef | grep -v grep | grep $1 | awk '{print $2}' | tr '\\n' ' ')\"\n"
            + "\n"
            + "  for (( c=1; c<${#children[@]} ; c++ ))\n"
            + "  do\n"
            + "    list_descendants ${children[c]}\n"
            + "  done\n"
            + "\n"
            + "  if kill -9 ${children[0]}; then\n"
            + "    echo \"Termination successful for PID: ${children[0]}\"\n"
            + "  else\n"
            + "    echo \"Failed to terminate PID: ${children[0]}\"\n"
            + "  fi\n"
            + "}\n"
            + "\n"
            + "list_descendants $(ps -ef | grep -v grep | grep -m1 FILE | awk '{print $2}')");
  }
}
