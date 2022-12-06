package io.harness.delegate.pcf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.CfCommandUnitConstants.Wrapup;
import static io.harness.pcf.PcfUtils.encodeColor;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.CREATE_SERVICE_MANIFEST_ELEMENT;
import static io.harness.pcf.model.PcfConstants.INSTANCE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NO_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;
import static io.harness.pcf.model.PcfConstants.PROCESSES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PROCESSES_TYPE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.RANDOM_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.WEB_PROCESS_TYPE_MANIFEST_YML_ELEMENT;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.tas.TasNgConfigMapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfProdAppInfo;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.delegate.cf.retry.RetryAbleTaskExecutor;
import io.harness.delegate.cf.retry.RetryPolicy;
import io.harness.delegate.task.cf.CfCommandTaskHelperNG;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.pcf.request.CfBasicSetupRequestNG;
import io.harness.delegate.task.pcf.request.CfCommandRequestNG;
import io.harness.delegate.task.pcf.response.CfBasicSetupResponseNG;
import io.harness.delegate.task.pcf.response.CfCommandResponseNG;
import io.harness.delegate.task.pcf.response.TasInfraConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.sanitizer.ExceptionMessageSanitizer;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logging.Misc;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfManifestFileData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CloudFoundryConfig;

import software.wings.delegatetasks.pcf.PcfCommandTaskHelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

@Singleton
@NoArgsConstructor
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class TasBasicSetupTaskHandler extends CfCommandTaskNGHandler {
  private static Yaml yaml = null;
  @Inject PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper;
  @Inject TasNgConfigMapper tasNgConfigMapper;
  @Inject protected CfCommandTaskHelperNG cfCommandTaskHelperNG;
  @Inject CfDeploymentManager cfDeploymentManager;
  @Inject protected PcfCommandTaskHelper pcfCommandTaskHelper;

  private static final int MAX_RELEASE_VERSIONS_TO_KEEP = 3;

  static {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setExplicitStart(true);
    yaml = new Yaml(new SafeConstructor(), new Representer(), options);
  }

  @Override
  protected CfCommandResponseNG executeTaskInternal(CfCommandRequestNG cfCommandRequestNG,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(cfCommandRequestNG instanceof CfBasicSetupRequestNG)) {
      throw new InvalidArgumentsException(Pair.of("cfCommandRequestNG", "Must be instance of CfBasicSetupRequestNG"));
    }

    LogCallback logCallback = iLogStreamingTaskClient.obtainLogCallback(cfCommandRequestNG.getCommandName());
    CfManifestFileData pcfManifestFileData = CfManifestFileData.builder().varFiles(new ArrayList<>()).build();

    CfBasicSetupRequestNG basicSetupRequestNG = (CfBasicSetupRequestNG) cfCommandRequestNG;
    TasInfraConfig tasInfraConfig = basicSetupRequestNG.getTasInfraConfig();
    CloudFoundryConfig cfConfig = tasNgConfigMapper.mapTasConfigWithDecryption(
        tasInfraConfig.getTasConnectorDTO(), tasInfraConfig.getEncryptionDataDetails());
    CfRequestConfig cfRequestConfig = getCfRequestConfig(basicSetupRequestNG, cfConfig);

    File artifactFile = null;
    File workingDirectory = null;
    List<ApplicationSummary> previousReleases =
        cfDeploymentManager.getPreviousReleases(cfRequestConfig, basicSetupRequestNG.getReleaseNamePrefix());

    CfProdAppInfo currentProdInfo = getCurrentProdInfo(previousReleases);

    try {
      workingDirectory = generateWorkingDirectoryOnDelegate(basicSetupRequestNG);

      CfAppAutoscalarRequestData cfAppAutoscalarRequestData =
          CfAppAutoscalarRequestData.builder()
              .cfRequestConfig(cfRequestConfig)
              .configPathVar(workingDirectory.getAbsolutePath())
              .timeoutInMins(basicSetupRequestNG.getTimeoutIntervalInMin())
              .build();

      logCallback.saveExecutionLog("\n# Fetching all existing applications ");

      // Print Existing applications information
      printExistingApplicationsDetails(logCallback, previousReleases);

      // saveDetailAboutProdAppForRollback() - DONE
      // downloadArtifactFile()
      // deleteOlderApplications();
      // renameProdApp() --> getNew name using something like getReleaseRevisionForNewApplication()
      // createNewApp() --> generateManifestAndVars()

      artifactFile = downloadArtifactFile(basicSetupRequestNG);

      deleteOlderApplications(previousReleases, cfRequestConfig, basicSetupRequestNG, cfAppAutoscalarRequestData,
          logCallback, currentProdInfo);

      renameProductionApplication(previousReleases, basicSetupRequestNG, cfRequestConfig, logCallback);

      boolean varsYmlPresent = checkIfVarsFilePresent(basicSetupRequestNG);
      CfCreateApplicationRequestData requestData =
          CfCreateApplicationRequestData.builder()
              .cfRequestConfig(cfRequestConfig)
              .artifactPath(artifactFile == null ? null : artifactFile.getAbsolutePath())
              .configPathVar(workingDirectory.getAbsolutePath())
              // TODO - verify how this can be done
              //                      .password(pcfCommandTaskHelper.getPassword(basicSetupRequestNG.getArtifactStreamAttributes()))
              .newReleaseName(basicSetupRequestNG.getReleaseNamePrefix())
              .pcfManifestFileData(pcfManifestFileData)
              .varsYmlFilePresent(varsYmlPresent)
              // TODO - verify how this can be done
              .dockerBasedDeployment(false)
              .build();

      requestData.setFinalManifestYaml(generateManifestYamlForPush(basicSetupRequestNG, requestData));
      // Create manifest.yaml file
      prepareManifestYamlFile(requestData);

      if (varsYmlPresent) {
        prepareVarsYamlFile(requestData, basicSetupRequestNG);
      }

      // Create new Application
      logCallback.saveExecutionLog(color("\n# Creating new Application", White, Bold));
      // Update pcfRequestConfig with details to create application

      // TODO - instead of updating create new object of CfRequestConfig
      //      updatePcfRequestConfig(cfCommandSetupRequest, cfRequestConfig, newReleaseName);

      ApplicationDetail newApplication = createAppAndPrintDetails(logCallback, requestData);

      CfBasicSetupResponseNG cfSetupCommandResponse =
          CfBasicSetupResponseNG.builder()
              .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
              .newApplicationDetails(CfAppSetupTimeDetails.builder()
                                         .applicationGuid(newApplication.getId())
                                         .applicationName(newApplication.getName())
                                         .oldName(newApplication.getName())
                                         .urls(new ArrayList<>(newApplication.getUrls()))
                                         .initialInstanceCount(0)
                                         .build())
              .currentProdInfo(currentProdInfo)
              .build();

      logCallback.saveExecutionLog("\n ----------  PCF Setup process completed successfully", INFO, SUCCESS);
      return cfSetupCommandResponse;

    } catch (RuntimeException | PivotalClientApiException | IOException e) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      log.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Setup task [{}]", basicSetupRequestNG,
          sanitizedException);
      logCallback.saveExecutionLog(
          "\n\n ----------  PCF Setup process failed to complete successfully", ERROR, CommandExecutionStatus.FAILURE);

      Misc.logAllMessages(sanitizedException, logCallback);
      return CfBasicSetupResponseNG.builder()
          .currentProdInfo(currentProdInfo)
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(sanitizedException))
          .build();
    } finally {
      logCallback = iLogStreamingTaskClient.obtainLogCallback(Wrapup);
      removeTempFilesCreated(basicSetupRequestNG, logCallback, artifactFile, workingDirectory, pcfManifestFileData);
      logCallback.saveExecutionLog("#----------  Cleaning up temporary files completed", INFO, SUCCESS);
    }
  }

  void prepareManifestYamlFile(CfCreateApplicationRequestData requestData) throws IOException {
    File manifestYamlFile = pcfCommandTaskBaseHelper.createManifestYamlFileLocally(requestData);
    requestData.setManifestFilePath(manifestYamlFile.getAbsolutePath());
    requestData.getPcfManifestFileData().setManifestFile(manifestYamlFile);
  }

  void prepareVarsYamlFile(CfCreateApplicationRequestData requestData, CfBasicSetupRequestNG setupRequest)
      throws IOException {
    if (!requestData.isVarsYmlFilePresent()) {
      return;
    }

    PcfManifestsPackage pcfManifestsPackage = setupRequest.getPcfManifestsPackage();
    AtomicInteger varFileIndex = new AtomicInteger(0);
    pcfManifestsPackage.getVariableYmls().forEach(varFileYml -> {
      File varsYamlFile =
          pcfCommandTaskBaseHelper.createManifestVarsYamlFileLocally(requestData, varFileYml, varFileIndex.get());
      if (varsYamlFile != null) {
        varFileIndex.incrementAndGet();
        requestData.getPcfManifestFileData().getVarFiles().add(varsYamlFile);
      }
    });
  }

  ApplicationDetail createAppAndPrintDetails(
      LogCallback executionLogCallback, CfCreateApplicationRequestData requestData) throws PivotalClientApiException {
    requestData.getCfRequestConfig().setLoggedin(false);
    ApplicationDetail newApplication = cfDeploymentManager.createApplication(requestData, executionLogCallback);
    executionLogCallback.saveExecutionLog(color("# Application created successfully", White, Bold));
    executionLogCallback.saveExecutionLog("# App Details: ");
    pcfCommandTaskBaseHelper.printApplicationDetail(newApplication, executionLogCallback);
    return newApplication;
  }
  private void renameProductionApplication(List<ApplicationSummary> previousReleases,
      CfBasicSetupRequestNG basicSetupRequestNG, CfRequestConfig cfRequestConfig, LogCallback logCallback)
      throws PivotalClientApiException {
    ApplicationSummary currentProdApplicationSummary = getCurrentProdApplicationSummary(previousReleases);

    if (EmptyPredicate.isEmpty(previousReleases) || currentProdApplicationSummary == null) {
      return;
    }
    String revision = "";
    if (previousReleases.size() == 1) {
      revision = "0";
    } else {
      String previousVersionedAppName = previousReleases.get(previousReleases.size() - 1).getName();
      int latestVersionUsed = pcfCommandTaskBaseHelper.getRevisionFromReleaseName(previousVersionedAppName);
      revision = latestVersionUsed == -1 ? "0" : String.valueOf(latestVersionUsed + 1);
    }

    String appNamePrefix = basicSetupRequestNG.getReleaseNamePrefix();
    String newName = appNamePrefix + revision;

    pcfCommandTaskBaseHelper.renameApp(currentProdApplicationSummary, cfRequestConfig, logCallback, newName);
  }

  private File downloadArtifactFile(CfBasicSetupRequestNG basicSetupRequestNG) {
    File artifactFile = null;
    if (basicSetupRequestNG.isPackageArtifact()) {
      // TODO - how artifact artifact.getArtifactFiles() works in NG or should we do the way it is done for Wbe App
      // artifactFile = fetchArtifactFileForDeployment(cfCommandSetupRequest, workingDirectory, executionLogCallback);
    }
    return artifactFile;
  }

  private ApplicationSummary getCurrentProdApplicationSummary(List<ApplicationSummary> previousReleases) {
    if (EmptyPredicate.isEmpty(previousReleases)) {
      return null;
    }

    ApplicationSummary currentActiveApplication =
        previousReleases.stream()
            .filter(applicationSummary -> applicationSummary.getInstances() > 0)
            .reduce((first, second) -> second)
            .orElse(null);

    // if not found, get Most recent version with non-zero count.
    if (currentActiveApplication == null) {
      currentActiveApplication = previousReleases.get(previousReleases.size() - 1);
    }
    return currentActiveApplication;
  }
  private CfProdAppInfo getCurrentProdInfo(List<ApplicationSummary> previousReleases) {
    ApplicationSummary currentActiveApplication = getCurrentProdApplicationSummary(previousReleases);
    if (currentActiveApplication == null) {
      return CfProdAppInfo.builder().build();
    }

    return CfProdAppInfo.builder()
        .applicationName(currentActiveApplication.getName())
        .applicationGuid(currentActiveApplication.getId())
        .attachedRoutes(currentActiveApplication.getUrls())
        .runningCount(currentActiveApplication.getRunningInstances())
        .build();
  }
  private CfRequestConfig getCfRequestConfig(CfBasicSetupRequestNG basicSetupRequestNG, CloudFoundryConfig cfConfig) {
    return CfRequestConfig.builder()
        .userName(String.valueOf(cfConfig.getUserName()))
        .password(String.valueOf(cfConfig.getPassword()))
        .endpointUrl(cfConfig.getEndpointUrl())
        .orgName(basicSetupRequestNG.getTasInfraConfig().getOrganization())
        .spaceName(basicSetupRequestNG.getTasInfraConfig().getSpace())
        .timeOutIntervalInMins(basicSetupRequestNG.getTimeoutIntervalInMin())
        .useCFCLI(basicSetupRequestNG.isUseCfCLI())
        .cfCliPath(cfCommandTaskHelperNG.getCfCliPathOnDelegate(
            basicSetupRequestNG.isUseCfCLI(), basicSetupRequestNG.getCfCliVersion()))
        .cfCliVersion(basicSetupRequestNG.getCfCliVersion())
        .build();
  }

  private File generateWorkingDirectoryOnDelegate(CfBasicSetupRequestNG cfCommandSetupRequest)
      throws PivotalClientApiException, IOException {
    File workingDirectory = pcfCommandTaskBaseHelper.generateWorkingDirectoryForDeployment();
    if (cfCommandSetupRequest.isUseCfCLI() || cfCommandSetupRequest.isUseAppAutoscalar()) {
      if (workingDirectory == null) {
        throw new PivotalClientApiException("Failed to generate CF-CLI Working directory");
      }
    }
    return workingDirectory;
  }

  private void printExistingApplicationsDetails(
      LogCallback executionLogCallback, List<ApplicationSummary> previousReleases) {
    if (EmptyPredicate.isEmpty(previousReleases)) {
      executionLogCallback.saveExecutionLog("# No Existing applications found");
    } else {
      StringBuilder appNames = new StringBuilder(color("# Existing applications: ", White, Bold));
      previousReleases.forEach(
          applicationSummary -> appNames.append("\n").append(encodeColor(applicationSummary.getName())));
      executionLogCallback.saveExecutionLog(appNames.toString());
    }
  }

  void deleteOlderApplications(List<ApplicationSummary> previousReleases, CfRequestConfig cfRequestConfig,
      CfBasicSetupRequestNG cfCommandSetupRequest, CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback logCallback, CfProdAppInfo currentProdInfo) {
    if (EmptyPredicate.isEmpty(previousReleases) || previousReleases.size() == 1) {
      return;
    }

    int olderVersionCountToKeep = cfCommandSetupRequest.getOlderActiveVersionCountToKeep() == null
        ? MAX_RELEASE_VERSIONS_TO_KEEP
        : cfCommandSetupRequest.getOlderActiveVersionCountToKeep();

    logCallback.saveExecutionLog("# Existing applications to Keep: " + olderVersionCountToKeep);

    // Now, we need to keep "olderVersionCountToKeep" no of apps.
    // We will keep most recent/active one as is, and downsize olderActiveVersionCountToKeep - 1
    // apps to 0, so they will be deleted in next deployment.
    int olderValidAppsFound = 1;
    for (int index = previousReleases.size() - 1; index >= 0; index--) {
      ApplicationSummary applicationSummary = previousReleases.get(index);
      if (olderValidAppsFound < olderVersionCountToKeep && currentProdInfo != null
          && applicationSummary.getName().equals(currentProdInfo.getApplicationName())) {
        olderValidAppsFound++;
        downsizeApplicationToZero(
            applicationSummary, cfRequestConfig, cfCommandSetupRequest, appAutoscalarRequestData, logCallback);
      } else {
        logCallback.saveExecutionLog("# Older application being deleted: " + encodeColor(applicationSummary.getName()));
        deleteApplication(applicationSummary, cfRequestConfig, logCallback);
      }
    }
  }

  private void deleteApplication(
      ApplicationSummary applicationSummary, CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) {
    cfRequestConfig.setApplicationName(applicationSummary.getName());
    try {
      cfDeploymentManager.deleteApplication(cfRequestConfig);
      //      appsDeleted.add(applicationSummary.getName());
    } catch (PivotalClientApiException e) {
      executionLogCallback.saveExecutionLog("Failed while deleting application: "
              + encodeColor(applicationSummary.getName()) + ", Continuing for next one",
          LogLevel.ERROR);
    }
  }
  void downsizeApplicationToZero(ApplicationSummary applicationSummary, CfRequestConfig cfRequestConfig,
      CfBasicSetupRequestNG cfCommandSetupRequest, CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(
        "# Application Being Downsized To 0: " + encodeColor(applicationSummary.getName()));

    RetryAbleTaskExecutor retryAbleTaskExecutor = RetryAbleTaskExecutor.getExecutor();
    if (cfCommandSetupRequest.isUseAppAutoscalar()) {
      appAutoscalarRequestData.setApplicationName(applicationSummary.getName());
      appAutoscalarRequestData.setApplicationGuid(applicationSummary.getId());
      appAutoscalarRequestData.setExpectedEnabled(true);
      pcfCommandTaskBaseHelper.disableAutoscalarSafe(appAutoscalarRequestData, executionLogCallback);
    }

    cfRequestConfig.setApplicationName(applicationSummary.getName());
    cfRequestConfig.setDesiredCount(0);

    unMapRoutes(cfRequestConfig, executionLogCallback, retryAbleTaskExecutor);
    unsetEnvVariables(cfRequestConfig, executionLogCallback, retryAbleTaskExecutor);
    downsizeApplication(applicationSummary, cfRequestConfig, executionLogCallback, retryAbleTaskExecutor);
  }

  private void unMapRoutes(
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback, RetryAbleTaskExecutor retryAbleTaskExecutor) {
    try {
      ApplicationDetail applicationDetail = cfDeploymentManager.getApplicationByName(cfRequestConfig);
      // Unmap routes from application having 0 instances
      if (isNotEmpty(applicationDetail.getUrls())) {
        RetryPolicy retryPolicy =
            RetryPolicy.builder()
                .userMessageOnFailure(String.format(
                    "Failed to un map routes from application - %s", encodeColor(cfRequestConfig.getApplicationName())))
                .finalErrorMessage(String.format("Please manually unmap the routes for application : %s ",
                    encodeColor(cfRequestConfig.getApplicationName())))
                .retry(3)
                .build();

        retryAbleTaskExecutor.execute(()
                                          -> cfDeploymentManager.unmapRouteMapForApplication(
                                              cfRequestConfig, applicationDetail.getUrls(), executionLogCallback),
            executionLogCallback, log, retryPolicy);
      }
    } catch (PivotalClientApiException exception) {
      log.warn(ExceptionMessageSanitizer.sanitizeException(exception).getMessage());
    }
  }

  private void unsetEnvVariables(
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback, RetryAbleTaskExecutor retryAbleTaskExecutor) {
    // TODO this only for BG
    // Remove Env Variable "HARNESS__STATUS__IDENTIFIER"
    RetryPolicy retryPolicy =
        RetryPolicy.builder()
            .userMessageOnFailure(String.format("Failed to un set env variable for application - %s",
                encodeColor(cfRequestConfig.getApplicationName())))
            .finalErrorMessage(String.format(
                "Failed to un set env variable for application - %s. Please manually un set it to avoid any future issue ",
                encodeColor(cfRequestConfig.getApplicationName())))
            .retry(3)
            .build();

    retryAbleTaskExecutor.execute(
        ()
            -> cfDeploymentManager.unsetEnvironmentVariableForAppStatus(cfRequestConfig, executionLogCallback),
        executionLogCallback, log, retryPolicy);
  }

  private void downsizeApplication(ApplicationSummary applicationSummary, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, RetryAbleTaskExecutor retryAbleTaskExecutor) {
    RetryPolicy retryPolicy =
        RetryPolicy.builder()
            .userMessageOnFailure(
                String.format("Failed while Downsizing application: %s", encodeColor(applicationSummary.getName())))
            .finalErrorMessage(String.format("Failed to downsize application: %s. Please downsize it manually",
                encodeColor(applicationSummary.getName())))
            .retry(3)
            .build();
    retryAbleTaskExecutor.execute(
        () -> cfDeploymentManager.resizeApplication(cfRequestConfig), executionLogCallback, log, retryPolicy);
  }

  boolean checkIfVarsFilePresent(CfBasicSetupRequestNG setupRequest) {
    if (setupRequest.getPcfManifestsPackage() == null) {
      return false;
    }

    List<String> varFiles = setupRequest.getPcfManifestsPackage().getVariableYmls();
    if (isNotEmpty(varFiles)) {
      varFiles = varFiles.stream().filter(StringUtils::isNotBlank).collect(toList());
    }

    return isNotEmpty(varFiles);
  }

  public String generateManifestYamlForPush(CfBasicSetupRequestNG cfCommandSetupRequest,
      CfCreateApplicationRequestData requestData) throws PivotalClientApiException {
    // Substitute name,
    String manifestYaml = cfCommandSetupRequest.getManifestYaml();

    Map<String, Object> map;
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      map = (Map<String, Object>) mapper.readValue(manifestYaml, Map.class);
    } catch (Exception e) {
      throw new UnexpectedException("Failed to get Yaml Map", e);
    }

    List<Map> applicationMaps = (List<Map>) map.get(APPLICATION_YML_ELEMENT);

    if (isEmpty(applicationMaps)) {
      throw new InvalidArgumentsException(
          Pair.of("Manifest.yml does not have any elements under \'applications\'", manifestYaml));
    }

    Map mapForUpdate = applicationMaps.get(0);
    TreeMap<String, Object> applicationToBeUpdated = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    applicationToBeUpdated.putAll(mapForUpdate);

    applicationToBeUpdated.put(NAME_MANIFEST_YML_ELEMENT, requestData.getNewReleaseName());

    // TODO - verify how this can be done
    //    updateArtifactDetails(requestData, cfCommandSetupRequest, applicationToBeUpdated);

    applicationToBeUpdated.put(INSTANCE_MANIFEST_YML_ELEMENT, 0);

    if (applicationToBeUpdated.containsKey(PROCESSES_MANIFEST_YML_ELEMENT)) {
      Object processes = applicationToBeUpdated.get(PROCESSES_MANIFEST_YML_ELEMENT);
      if (processes instanceof ArrayList<?>) {
        ArrayList<Map<String, Object>> allProcesses = (ArrayList<Map<String, Object>>) processes;
        for (Map<String, Object> process : allProcesses) {
          Object p = process.get(PROCESSES_TYPE_MANIFEST_YML_ELEMENT);
          if ((p instanceof String) && (p.toString().equals(WEB_PROCESS_TYPE_MANIFEST_YML_ELEMENT))) {
            process.put(INSTANCE_MANIFEST_YML_ELEMENT, 0);
          }
        }
      }
    }
    // Update routes.
    updateConfigWithRoutesIfRequired(requestData, applicationToBeUpdated, cfCommandSetupRequest);
    // We do not want to change order

    // remove "create-services" elements as it would have been used by cf cli plugin to create services.
    // This elements is not needed for cf push
    map.remove(CREATE_SERVICE_MANIFEST_ELEMENT);

    // TODO - not needed for Basic, Canary
    //    addInactiveIdentifierToManifest(applicationToBeUpdated, requestData, cfCommandSetupRequest);
    Map<String, Object> applicationMapForYamlDump =
        pcfCommandTaskBaseHelper.generateFinalMapForYamlDump(applicationToBeUpdated);

    // replace map for first application that we are deploying
    applicationMaps.set(0, applicationMapForYamlDump);
    try {
      return yaml.dump(map);
    } catch (Exception e) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Failed to generate final version of  Manifest.yml file. ")
                                              .append(manifestYaml)
                                              .toString(),
          e);
    }
  }

  private void updateConfigWithRoutesIfRequired(
      CfCreateApplicationRequestData requestData, TreeMap applicationToBeUpdated, CfBasicSetupRequestNG setupRequest) {
    applicationToBeUpdated.remove(ROUTES_MANIFEST_YML_ELEMENT);

    // 1. Check and handle no-route scenario
    boolean isNoRoute = applicationToBeUpdated.containsKey(NO_ROUTE_MANIFEST_YML_ELEMENT)
        && (boolean) applicationToBeUpdated.get(NO_ROUTE_MANIFEST_YML_ELEMENT);
    if (isNoRoute) {
      applicationToBeUpdated.remove(ROUTES_MANIFEST_YML_ELEMENT);
      return;
    }

    // 2. Check if random-route config is needed. This happens if random-route=true in manifest or
    // user has not provided any route value.
    if (pcfCommandTaskBaseHelper.shouldUseRandomRoute(applicationToBeUpdated, setupRequest.getRouteMaps())) {
      applicationToBeUpdated.put(RANDOM_ROUTE_MANIFEST_YML_ELEMENT, true);
      return;
    }

    // 3. Insert routes provided by user.
    List<String> routesForUse = setupRequest.getRouteMaps();
    List<Map<String, String>> routeMapList = new ArrayList<>();
    routesForUse.forEach(routeString -> {
      Map<String, String> mapEntry = Collections.singletonMap(ROUTE_MANIFEST_YML_ELEMENT, routeString);
      routeMapList.add(mapEntry);
    });

    // Add this route config to applicationConfig
    applicationToBeUpdated.put(ROUTES_MANIFEST_YML_ELEMENT, routeMapList);
  }

  // Remove downloaded artifact and generated yaml files
  private void removeTempFilesCreated(CfBasicSetupRequestNG cfCommandSetupRequest, LogCallback executionLogCallback,
      File artifactFile, File workingDirectory, CfManifestFileData pcfManifestFileData) {
    try {
      executionLogCallback.saveExecutionLog("# Deleting any temporary files created");
      List<File> filesToBeRemoved = new ArrayList<>();

      // Delete all manifests created.
      File manifestYamlFile = pcfManifestFileData.getManifestFile();
      if (manifestYamlFile != null) {
        filesToBeRemoved.add(pcfManifestFileData.getManifestFile());
      }
      filesToBeRemoved.addAll(pcfManifestFileData.getVarFiles());

      if (artifactFile != null) {
        filesToBeRemoved.add(artifactFile);
      }

      if (cfCommandSetupRequest.isUseCfCLI() && manifestYamlFile != null) {
        filesToBeRemoved.add(
            new File(pcfCommandTaskBaseHelper.generateFinalManifestFilePath(manifestYamlFile.getAbsolutePath())));
      }

      pcfCommandTaskBaseHelper.deleteCreatedFile(filesToBeRemoved);

      if (workingDirectory != null) {
        FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
      }
    } catch (Exception e) {
      log.warn("Failed to remove temp files created", e);
    }
  }
}
