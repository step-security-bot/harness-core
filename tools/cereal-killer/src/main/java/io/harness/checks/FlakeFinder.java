package io.harness.checks;

import io.harness.checks.buildpulse.client.BuildPulseClient;
import io.harness.checks.buildpulse.dto.TestFlakiness;
import io.harness.checks.buildpulse.dto.TestFlakinessList;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FlakeFinder {
  private final BuildPulseClient buildPulseClient;
  private final double maxFailChance;

  public FlakeFinder(BuildPulseClient buildPulseClient, double maxFailChance) {
    this.buildPulseClient = buildPulseClient;
    this.maxFailChance = maxFailChance;
  }

  double cumulativeFailChance(Collection<TestFlakiness> tests) {
    double passChance = 1;
    for (TestFlakiness test : tests) {
      passChance *= test.getPassChance();
    }
    return 1 - passChance;
  }

  public Set<String> fetchFlakyTests() {
    List<TestFlakiness> flakyTests = new ArrayList<>();
    try {
      Response<TestFlakinessList> portal = buildPulseClient.listFlakyTests("wings-software", "portal").execute();
      if (portal.isSuccessful()) {
        flakyTests = Objects.requireNonNull(portal.body()).getTests();
        flakyTests.sort(Comparator.comparingDouble(TestFlakiness::getDisruptiveness));
        logger.info("Total flakes: {}, failChance: {}", flakyTests.size(), cumulativeFailChance(flakyTests));
        Set<TestFlakiness> allowedTests = new HashSet<>();
        double minPassChance = 1 - maxFailChance;
        double cumulativePassChance = 1;
        for (TestFlakiness test : flakyTests) {
          // Check if allowing this test to run will reduce pass chance below the minimum allowed
          logger.info("Checking test {} with disruptiveness {}", test.getFqdn(), test.getDisruptiveness());
          double cumulativePassChanceIncludingTest = cumulativePassChance * test.getPassChance();
          if (cumulativePassChanceIncludingTest < minPassChance) {
            break;
          }
          allowedTests.add(test);
          cumulativePassChance = cumulativePassChanceIncludingTest;
        }
        logger.info("Out of {} flakes, {} allowed with cumulative pass chance {}", flakyTests.size(),
            allowedTests.size(), cumulativePassChance);
        flakyTests.removeAll(allowedTests);
      } else {
        logger.warn("Received error response {}", portal.code());
      }
    } catch (IOException e) {
      logger.error("Exception fetching flaky tests", e);
    }
    return flakyTests.stream().map(TestFlakiness::getFqdn).collect(Collectors.toSet());
  }
}
