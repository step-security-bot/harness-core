package software.wings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.rules.WingsRule;

/**
 * Created by anubhaw on 4/28/16.
 */
public abstract class WingsBaseTest {
  @Rule public TestName testName = new TestName();
  @Rule public WingsRule wingsRule = new WingsRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void logTestCaseName() {
    System.out.println(String.format("Running test %s", testName.getMethodName()));
  }

  protected Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
