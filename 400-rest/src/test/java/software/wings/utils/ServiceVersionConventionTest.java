package software.wings.utils;

import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceVersionConventionTest extends CategoryTest {
  public static final int LEN = 10;

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetServiceName() {
    String rev = "";
    assertThat(ServiceVersionConvention.getServiceName("p", rev)).isEqualTo("p");
    rev = null;
    assertThat(ServiceVersionConvention.getServiceName("p", rev)).isEqualTo("p");
    rev = "12";
    assertThat(ServiceVersionConvention.getServiceName("p", rev)).isEqualTo("p__12");
  }
}
