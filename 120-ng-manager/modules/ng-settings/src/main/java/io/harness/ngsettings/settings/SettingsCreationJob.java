/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngsettings.settings;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ngsettings.entities.NGSettingsConfigurationState;
import io.harness.ngsettings.entities.SettingConfiguration;
import io.harness.ngsettings.services.SettingsService;
import io.harness.repositories.ConfigurationStateRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
@Singleton
public class SettingsCreationJob {
  private final SettingsConfig settingsConfig;
  private final SettingsService settingsService;
  private final ConfigurationStateRepository configurationStateRepository;
  private static final String SETTINGS_YAML_PATH = "io/harness/ngsettings/settings.yml";

  @Inject
  public SettingsCreationJob(
      SettingsService settingsService, ConfigurationStateRepository configurationStateRepository) {
    this.configurationStateRepository = configurationStateRepository;
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    URL url = getClass().getClassLoader().getResource(SETTINGS_YAML_PATH);
    try {
      byte[] bytes = Resources.toByteArray(url);
      this.settingsConfig = om.readValue(bytes, SettingsConfig.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Settings file path is invalid or the syntax is incorrect", e);
    }
    this.settingsService = settingsService;
  }

  public void run() {
    Optional<NGSettingsConfigurationState> optional =
        configurationStateRepository.getByIdentifier(settingsConfig.getName());
    if (optional.isPresent() && optional.get().getConfigVersion() >= settingsConfig.getVersion()) {
      log.info("Settings are already updated in the database");
      return;
    }
    log.info("Updating settings in the database");
    Set<SettingConfiguration> latestSettings = settingsConfig.getSettings();
    Set<SettingConfiguration> currentSettings = new HashSet<>(settingsService.listDefaultSettings());

    Set<String> latestIdentifiers =
        latestSettings.stream().map(SettingConfiguration::getIdentifier).collect(Collectors.toSet());
    Set<String> currentIdentifiers =
        currentSettings.stream().map(SettingConfiguration::getIdentifier).collect(Collectors.toSet());
    Set<String> removedIdentifiers = Sets.difference(currentIdentifiers, latestIdentifiers);

    if (latestIdentifiers.size() < latestSettings.size()) {
      for (Iterator<SettingConfiguration> settingConfigurationIterator = latestSettings.iterator();
           settingConfigurationIterator.hasNext();) {
        String identifier = settingConfigurationIterator.next().getIdentifier();
        if (latestIdentifiers.contains(identifier)) {
          latestIdentifiers.remove(identifier);
        } else {
          throw new InvalidRequestException(
              String.format("Identifier- %s is not unique in %s", identifier, SETTINGS_YAML_PATH));
        }
      }
    }

    Map<String, String> settingIdMap = new HashMap<>();
    currentSettings.forEach(settingConfiguration -> {
      settingIdMap.put(settingConfiguration.getIdentifier(), settingConfiguration.getId());
      settingConfiguration.setId(null);
    });
    Set<SettingConfiguration> upsertedSettings = new HashSet<>(latestSettings);
    upsertedSettings.removeAll(Arrays.asList(currentSettings.toArray()));

    upsertedSettings.forEach(setting -> {
      setting.setId(settingIdMap.get(setting.getIdentifier()));
      settingsService.upsertConfig(setting);
    });
    removedIdentifiers.forEach(settingsService::deleteConfig);
    log.info("Updated the settings in the database");

    NGSettingsConfigurationState configurationState =
        optional.orElseGet(() -> NGSettingsConfigurationState.builder().identifier(settingsConfig.getName()).build());
    configurationState.setConfigVersion(settingsConfig.getVersion());
    configurationStateRepository.upsert(configurationState);
  }
}
