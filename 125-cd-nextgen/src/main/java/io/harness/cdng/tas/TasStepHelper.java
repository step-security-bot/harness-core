/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import static io.harness.cdng.manifest.ManifestType.TAS_AUTOSCALER;
import static io.harness.cdng.manifest.ManifestType.TAS_MANIFEST;
import static io.harness.cdng.manifest.ManifestType.TAS_VARS;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.INSTANCE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NO_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PROCESSES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PROCESSES_TYPE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.WEB_PROCESS_TYPE_MANIFEST_YML_ELEMENT;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Splitter;
import io.fabric8.utils.Strings;
import io.harness.beans.FileReference;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.k8s.beans.CustomFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.AutoScalerManifestOutcome;
import io.harness.cdng.manifest.yaml.CustomRemoteStoreConfig;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.TasManifestOutcome;
import io.harness.cdng.manifest.yaml.VarsManifestOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.localstore.LocalStoreFetchFilesResult;
import io.harness.delegate.task.manifests.request.CustomManifestFetchConfig;
import io.harness.delegate.task.manifests.request.CustomManifestValuesFetchParams;
import io.harness.delegate.task.manifests.response.CustomManifestValuesFetchResponse;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.filestore.dto.node.FileNodeDTO;
import io.harness.filestore.dto.node.FileStoreNodeDTO;
import io.harness.filestore.service.FileStoreService;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.manifest.CustomManifestSource;
import io.harness.manifest.CustomSourceFile;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.filestore.NGFileType;
import io.harness.pcf.CfCommandUnitConstants;
import io.harness.pcf.model.PcfConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.yaml.ParameterField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.GitFileConfig;
import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;
import software.wings.beans.TaskType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;

@Slf4j
public class TasStepHelper {
  @Inject protected OutcomeService outcomeService;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private FileStoreService fileStoreService;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StepHelper stepHelper;
  private static int DEFAULT_INSTANCE_COUNT = 2;

  private static final Splitter lineSplitter = Splitter.onPattern("\\r?\\n").trimResults().omitEmptyStrings();

  public static final String FILE_START_REPO_ROOT_REGEX = PcfConstants.FILE_START_REPO_ROOT_REGEX;
  public static final String FILE_START_SERVICE_MANIFEST_REGEX = PcfConstants.FILE_START_SERVICE_MANIFEST_REGEX;
  public static final String FILE_END_REGEX = "(\\s|,|;|'|\"|:|$)";

  public static final Pattern PATH_REGEX_REPO_ROOT_PATTERN =
          Pattern.compile(FILE_START_REPO_ROOT_REGEX + ".*?" + FILE_END_REGEX);
  public static final Pattern FILE_START_SERVICE_MANIFEST_PATTERN =
          Pattern.compile(FILE_START_SERVICE_MANIFEST_REGEX + ".*?" + FILE_END_REGEX);

  public static final String START_SLASH_ALL_MATCH = "\\A/+";
  public static final String END_SLASH_ALL_MATCH = "/+\\Z";

  public TaskChainResponse startChainLink(
      TasStepExecutor tasStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {
    ManifestsOutcome manifestsOutcome = resolveManifestsOutcome(ambiance);
    ExpressionEvaluatorUtils.updateExpressions(
        manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    cdStepHelper.validateManifestsOutcome(ambiance, manifestsOutcome);

    List<AutoScalerManifestOutcome> autoScalerManifestOutcomeList = new ArrayList<>();
    List<VarsManifestOutcome> varsManifestOutcomeList = new ArrayList<>();
    TasManifestOutcome tasManifestOutcome = filterManifestOutcomesByTypeAndReturnTasManifest(
        manifestsOutcome.values(), autoScalerManifestOutcomeList, varsManifestOutcomeList);

    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder()
                                                        .tasManifestOutcome(tasManifestOutcome)
                                                        .manifestOutcomeList(new ArrayList<>(manifestsOutcome.values()))
                                                        .varsManifestOutcomeList(varsManifestOutcomeList)
                                                        .autoScalerManifestOutcomeList(autoScalerManifestOutcomeList)
                                                        .build();
    shouldExecuteStoreFetch(tasStepPassThroughData);
    tasStepPassThroughData.setShouldCloseFetchFilesStream(false);
    tasStepPassThroughData.setShouldOpenFetchFilesStream(
        shouldOpenFetchFilesStream(tasStepPassThroughData.getShouldOpenFetchFilesStream()));

    return prepareManifests(tasStepExecutor, ambiance, stepElementParameters, tasStepPassThroughData);
  }

  public String removeCommentedLineFromScript(String scriptString) {
    return lineSplitter.splitToList(scriptString)
            .stream()
            .filter(line -> !line.isEmpty())
            .filter(line -> line.charAt(0) != '#')
            .collect(Collectors.joining("\n"));
  }

  public TaskChainResponse startChainLinkForCommandStep(
          TasStepExecutor tasStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {

    TasCommandStepParameters tasCommandStepParameters = (TasCommandStepParameters) stepElementParameters.getSpec();

    HarnessStore storeConfig;
    if(ManifestStoreType.HARNESS.equals(tasCommandStepParameters.getScript().getStore().getSpec())) {
      storeConfig = (HarnessStore) tasCommandStepParameters.getScript().getStore().getSpec();
    } else {
      throw new InvalidRequestException("Harness Store is only supported for TAS Command Scripts", USER);
    }

    LogCallback logCallback = cdStepHelper.getLogCallback(
            CfCommandUnitConstants.FetchCommandScript, ambiance, true);
    String scriptString = null;
    TasManifestFileContents tasManifestFileContents = getFileContentsFromManifest(AmbianceUtils.getNgAccess(ambiance), getParameterFieldValue(storeConfig.getFiles()),
            "TasCommandScript" , "TasCommandScript", logCallback);
    if(tasManifestFileContents.getLocalStoreFetchFilesResult() != null && tasManifestFileContents.getLocalStoreFetchFilesResult().getLocalStoreFileContents() != null
      && tasManifestFileContents.getLocalStoreFetchFilesResult().getLocalStoreFileContents().size() == 1) {
      scriptString = tasManifestFileContents.getLocalStoreFetchFilesResult().getLocalStoreFileContents().get(0);
    }
    logCallback.saveExecutionLog("Done", INFO, SUCCESS);

    //Resolving expressions
    ExpressionEvaluatorUtils.updateExpressions(
            scriptString, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    String rawScript = removeCommentedLineFromScript(scriptString);

    ManifestsOutcome manifestsOutcome = resolveManifestsOutcome(ambiance);
    ExpressionEvaluatorUtils.updateExpressions(
            manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    cdStepHelper.validateManifestsOutcome(ambiance, manifestsOutcome);

    List<AutoScalerManifestOutcome> autoScalerManifestOutcomeList = new ArrayList<>();
    List<VarsManifestOutcome> varsManifestOutcomeList = new ArrayList<>();
    TasManifestOutcome tasManifestOutcome = filterManifestOutcomesByTypeAndReturnTasManifest(
            manifestsOutcome.values(), autoScalerManifestOutcomeList, varsManifestOutcomeList);

    final boolean serviceManifestStoreInGitSubset = ManifestStoreType.isInGitSubset(tasManifestOutcome.getStore().getKind());

    //Finding Repo Root
    String repoRoot = "/";
    if (serviceManifestStoreInGitSubset) {
      repoRoot = getRepoRoot(tasManifestOutcome);
    }

    final List<String> pathsFromScript = findPathFromScript(rawScript, repoRoot);

    TasStepPassThroughData tasStepPassThroughData = TasStepPassThroughData.builder()
            .tasManifestOutcome(tasManifestOutcome)
            .manifestOutcomeList(new ArrayList<>(manifestsOutcome.values()))
            .varsManifestOutcomeList(varsManifestOutcomeList)
            .autoScalerManifestOutcomeList(autoScalerManifestOutcomeList)
            .rawScript(rawScript)
            .repoRoot(repoRoot)
            .pathsFromScript(pathsFromScript)
            .build();

      //  fire task to fetch remote files
    shouldExecuteStoreFetch(tasStepPassThroughData);
    tasStepPassThroughData.setShouldCloseFetchFilesStream(false);
    tasStepPassThroughData.setShouldOpenFetchFilesStream(
            shouldOpenFetchFilesStream(tasStepPassThroughData.getShouldOpenFetchFilesStream()));

    return prepareManifests(tasStepExecutor, ambiance, stepElementParameters, tasStepPassThroughData);
  }

  private String toRelativePath(String path) {
    return path.trim().replaceFirst(START_SLASH_ALL_MATCH, "");
  }

  private String getRepoRoot(TasManifestOutcome tasManifestOutcome) {
    if(ManifestStoreType.isInGitSubset(tasManifestOutcome.getStore().getKind())) {
      final GitStoreConfig gitFileConfig = (GitStoreConfig) tasManifestOutcome.getStore();
      List<String> paths = getParameterFieldValue(gitFileConfig.getPaths());
      return "/" + toRelativePath((paths != null && !paths.isEmpty()) ? paths.get(0) : "/").trim();
    }
    else {
      return "/";
    }
  }

  List<String> findPathFromScript(String rendredScript, String repoRoot) {
    final Set<String> finalPathLists = new HashSet<>();
    final List<String> repoRootPrefixPathList =
            findPathFromScript(rendredScript, PATH_REGEX_REPO_ROOT_PATTERN, FILE_START_REPO_ROOT_REGEX, FILE_END_REGEX);
    List<String> serviceManifestPrefixPathList = findPathFromScript(
            rendredScript, FILE_START_SERVICE_MANIFEST_PATTERN, FILE_START_SERVICE_MANIFEST_REGEX, FILE_END_REGEX);

    if (!(isEmpty(repoRoot) || "/".equals(repoRoot))) {
      serviceManifestPrefixPathList = serviceManifestPrefixPathList.stream()
              .map(path -> repoRoot + path)
              .map(this::removeTrailingSlash)
              .collect(Collectors.toList());
    }

    finalPathLists.addAll(repoRootPrefixPathList);
    finalPathLists.addAll(serviceManifestPrefixPathList);
    return new ArrayList<>(finalPathLists);
  }

  private String removeTrailingSlash(String s) {
    return s.replaceFirst(END_SLASH_ALL_MATCH, "");
  }

  private List<String> findPathFromScript(
          String renderedScript, Pattern matchPattern, String prefixRegex, String fileEndRegex) {
    final Matcher matcher = matchPattern.matcher(renderedScript);
    List<String> filePathList = new ArrayList<>();
    while (matcher.find()) {
      final String filePath = renderedScript.substring(matcher.start(), matcher.end())
              .trim()
              .replaceFirst(prefixRegex, "")
              .replaceFirst(fileEndRegex, "");
      filePathList.add(filePath);
    }
    return filePathList.stream().map(this::canonacalizePath).distinct().collect(Collectors.toList());
  }

  private String canonacalizePath(String path) {
    return Strings.defaultIfEmpty(path.trim(), "/");
  }


  private TaskChainResponse prepareManifests(TasStepExecutor tasStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, TasStepPassThroughData tasStepPassThroughData) {
    Map<String, List<TasManifestFileContents>> localStoreFileMapContents = new HashMap<>();
    LogCallback logCallback = cdStepHelper.getLogCallback(
        CfCommandUnitConstants.FetchFiles, ambiance, tasStepPassThroughData.getShouldOpenFetchFilesStream());
    if (tasStepPassThroughData.getShouldExecuteHarnessStoreFetch()) {
      fetchFilesFromLocalStore(localStoreFileMapContents, ambiance, tasStepPassThroughData, logCallback);
    }
    TasStepPassThroughData updatedTasStepPassThroughData =
        tasStepPassThroughData.toBuilder().localStoreFileMapContents(localStoreFileMapContents).build();

    logCallback.saveExecutionLog("Fetched all manifests from Harness Store ", INFO, CommandExecutionStatus.SUCCESS);
    return prepareManifestFilesFetchTask(
        tasStepExecutor, ambiance, stepElementParameters, updatedTasStepPassThroughData);
  }

  public void fetchFilesFromLocalStore(Map<String, List<TasManifestFileContents>> localStoreFileMapContents,
      Ambiance ambiance, TasStepPassThroughData tasStepPassThroughData, LogCallback logCallback) {
    logCallback.saveExecutionLog(color(format("%nStarting Harness Fetch Files"), LogColor.White, LogWeight.Bold));
    if (ManifestStoreType.HARNESS.equals(tasStepPassThroughData.getTasManifestOutcome().getStore().getKind())) {
      localStoreFileMapContents.put(tasStepPassThroughData.getTasManifestOutcome().getIdentifier(),
          getFileContentsAsLocalStoreFetchFilesResult(
              tasStepPassThroughData.getTasManifestOutcome(), AmbianceUtils.getNgAccess(ambiance), logCallback));
    }
    if (isNotEmpty(tasStepPassThroughData.getAutoScalerManifestOutcomeList())) {
      localStoreFileMapContents.putAll(getFileContentsForLocalStore(
          tasStepPassThroughData.getAutoScalerManifestOutcomeList(), AmbianceUtils.getNgAccess(ambiance), logCallback));
    }
    if (isNotEmpty(tasStepPassThroughData.getVarsManifestOutcomeList())) {
      localStoreFileMapContents.putAll(getFileContentsForLocalStore(
          tasStepPassThroughData.getAutoScalerManifestOutcomeList(), AmbianceUtils.getNgAccess(ambiance), logCallback));
    }
    logCallback.saveExecutionLog(
        color(format("%nHarness Fetch Files completed successfully."), LogColor.White, LogWeight.Bold));
  }

  public List<TasManifestFileContents> getFileContentsAsLocalStoreFetchFilesResult(
      TasManifestOutcome manifestOutcome, NGAccess ngAccess, LogCallback logCallback) {
    List<TasManifestFileContents> localStoreFetchFilesResultMap = new ArrayList<>();
    String manifestIdentifier = manifestOutcome.getIdentifier();
    HarnessStore localStoreConfig = (HarnessStore) manifestOutcome.getStore();
    if (localStoreConfig.getFiles().getValue().size() != 1) {
      throw new UnsupportedOperationException("Only one TAS manifest File is supported");
    }
    List<String> varsScopedFilePathList = getParameterFieldValue(manifestOutcome.getVarsPaths());
    List<String> autoScalerScopedFilePath = getParameterFieldValue(manifestOutcome.getAutoScalerPath());

    localStoreFetchFilesResultMap.add(getFileContentsFromManifest(ngAccess,
        List.of(localStoreConfig.getFiles().getValue().get(0)), TAS_MANIFEST, manifestIdentifier, logCallback));
    localStoreFetchFilesResultMap.add(
        getFileContentsFromManifest(ngAccess, varsScopedFilePathList, TAS_VARS, manifestIdentifier, logCallback));
    localStoreFetchFilesResultMap.add(
        getFileContentsFromManifest(ngAccess, autoScalerScopedFilePath, TAS_AUTOSCALER, manifestIdentifier, logCallback));
    return localStoreFetchFilesResultMap;
    // TODO: Check if default vars.yaml file need to be fetched
  }

  public TasManifestFileContents getFileContentsFromManifest(NGAccess ngAccess, List<String> scopedFilePathList,
      String manifestType, String manifestIdentifier, LogCallback logCallback) {
    List<String> fileContents = new ArrayList<>();
    List<String> filePaths = new ArrayList<>();
    if (isNotEmpty(scopedFilePathList)) {
      logCallback.saveExecutionLog(
          color(format("%nFetching %s files with identifier: %s", manifestType, manifestIdentifier), LogColor.White,
              LogWeight.Bold));
      logCallback.saveExecutionLog(color("Fetching following Files :", LogColor.White));
      printFilesFetchedFromHarnessStore(scopedFilePathList, logCallback);
      logCallback.saveExecutionLog(color("Successfully fetched following files: ", LogColor.White, LogWeight.Bold));
      for (String scopedFilePath : scopedFilePathList) {
        Optional<FileStoreNodeDTO> varsFile =
            validateAndFetchFileFromHarnessStore(scopedFilePath, ngAccess, manifestIdentifier);
        if (varsFile.isPresent()) {
          FileStoreNodeDTO fileStoreNodeDTO = varsFile.get();
          if (NGFileType.FILE.equals(fileStoreNodeDTO.getType())) {
            FileNodeDTO file = (FileNodeDTO) fileStoreNodeDTO;
            fileContents.add(file.getContent());
            filePaths.add(scopedFilePath);
            logCallback.saveExecutionLog(color(format("- %s", scopedFilePath), LogColor.White));
          } else {
            throw new UnsupportedOperationException("Only File type is supported. Please enter the correct file path");
          }
        }
      }
    }
    return TasManifestFileContents.builder()
        .manifestType(manifestType)
        .localStoreFetchFilesResult(LocalStoreFetchFilesResult.builder().LocalStoreFileContents(fileContents).LocalStoreFilePaths(filePaths).build())
        .build();
  }

  public void printFilesFetchedFromHarnessStore(List<String> scopedFilePathList, LogCallback logCallback) {
    for (String scopedFilePath : scopedFilePathList) {
      logCallback.saveExecutionLog(color(format("- %s", scopedFilePath), LogColor.White));
    }
  }

  private ManifestsOutcome resolveManifestsOutcome(Ambiance ambiance) {
    OptionalOutcome manifestsOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    if (!manifestsOutcome.isFound()) {
      String stageName =
          AmbianceUtils.getStageLevelFromAmbiance(ambiance).map(Level::getIdentifier).orElse("Deployment stage");
      String stepType =
          Optional.ofNullable(AmbianceUtils.getCurrentStepType(ambiance)).map(StepType::getType).orElse("Kubernetes");
      throw new GeneralException(format(
          "No manifests found in stage %s. %s step requires at least one manifest defined in stage service definition",
          stageName, stepType));
    }
    return (ManifestsOutcome) manifestsOutcome.getOutcome();
  }

  private TasManifestOutcome filterManifestOutcomesByTypeAndReturnTasManifest(
      Collection<ManifestOutcome> manifestOutcomes, List<AutoScalerManifestOutcome> autoScalerManifestOutcomeList,
      List<VarsManifestOutcome> varsManifestOutcomeList) {
    if (isEmpty(manifestOutcomes)) {
      throw new InvalidRequestException("Manifests are mandatory for TAS step.", USER);
    }
    List<ManifestOutcome> orderedManifestOutcomes = new ArrayList<>(manifestOutcomes)
                                                        .stream()
                                                        .sorted(Comparator.comparingInt(ManifestOutcome::getOrder))
                                                        .collect(Collectors.toCollection(LinkedList::new));
    TasManifestOutcome tasManifestOutcome = null;
    for (ManifestOutcome manifestOutcome : orderedManifestOutcomes) {
      if (TAS_AUTOSCALER.equals(manifestOutcome.getType())) {
        autoScalerManifestOutcomeList.add((AutoScalerManifestOutcome) manifestOutcome);
      } else if (TAS_VARS.equals(manifestOutcome.getType())) {
        varsManifestOutcomeList.add((VarsManifestOutcome) manifestOutcome);
      } else if (TAS_MANIFEST.equals(manifestOutcome.getType())) {
        if (isNull(tasManifestOutcome)) {
          tasManifestOutcome = (TasManifestOutcome) manifestOutcome;
        } else {
          throw new InvalidRequestException("There can be only a single TAS manifest", USER);
        }
      } else {
        throw new InvalidRequestException(format("Invalid Manifest type: %s", manifestOutcome.getType()), USER);
      }
    }
    return tasManifestOutcome;
  }

  public TaskChainResponse executeNextLink(TasStepExecutor tasStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    TasStepPassThroughData tasStepPassThroughData = (TasStepPassThroughData) passThroughData;
    TasManifestOutcome tasManifest = tasStepPassThroughData.getTasManifestOutcome();
    ResponseData responseData = responseDataSupplier.get();
    UnitProgressData unitProgressData = null;

    try {
      if (responseData instanceof GitFetchResponse) {
        unitProgressData = ((GitFetchResponse) responseData).getUnitProgressData();
        return handleGitFetchFilesResponse(
            responseData, tasStepExecutor, ambiance, stepElementParameters, tasStepPassThroughData, tasManifest);
      }

      if (responseData instanceof CustomManifestValuesFetchResponse) {
        unitProgressData = ((CustomManifestValuesFetchResponse) responseData).getUnitProgressData();
        return handleCustomFetchResponse(
            responseData, tasStepExecutor, ambiance, stepElementParameters, tasStepPassThroughData, tasManifest);
      }
    } catch (Exception e) {
      return TaskChainResponse.builder()
          .chainEnd(true)
          .passThroughData(StepExceptionPassThroughData.builder()
                               .errorMessage(ExceptionUtils.getMessage(e))
                               .unitProgressData(cdStepHelper.completeUnitProgressData(
                                   unitProgressData, ambiance, ExceptionUtils.getMessage(e)))
                               .build())
          .build();
    }

    return TaskChainResponse.builder()
        .chainEnd(true)
        .passThroughData(
            StepExceptionPassThroughData.builder()
                .errorMessage("Done")
                .unitProgressData(cdStepHelper.completeUnitProgressData(unitProgressData, ambiance, "Done"))
                .build())
        .build();
  }

  private TaskChainResponse handleGitFetchFilesResponse(ResponseData responseData, TasStepExecutor tasStepExecutor,
      Ambiance ambiance, StepElementParameters stepElementParameters, TasStepPassThroughData tasStepPassThroughData,
      TasManifestOutcome tasManifest) {
    GitFetchResponse gitFetchResponse = (GitFetchResponse) responseData;
    if (gitFetchResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      GitFetchResponsePassThroughData gitFetchResponsePassThroughData =
          GitFetchResponsePassThroughData.builder()
              .errorMsg(gitFetchResponse.getErrorMessage())
              .unitProgressData(gitFetchResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().chainEnd(true).passThroughData(gitFetchResponsePassThroughData).build();
    }
    LogCallback logCallback = cdStepHelper.getLogCallback(
        CfCommandUnitConstants.FetchGitFiles, ambiance, tasStepPassThroughData.getShouldOpenFetchFilesStream());
    logCallback.saveExecutionLog("Fetched all manifests from Git", INFO, CommandExecutionStatus.SUCCESS);
    TasStepPassThroughData updatedTasStepPassThroughData =
        tasStepPassThroughData.toBuilder().gitFetchFilesResultMap(gitFetchResponse.getFilesFromMultipleRepo()).build();
    return executeTasTask(ambiance, stepElementParameters, tasStepExecutor, updatedTasStepPassThroughData, tasManifest);
  }

  private TaskChainResponse handleCustomFetchResponse(ResponseData responseData, TasStepExecutor tasStepExecutor,
      Ambiance ambiance, StepElementParameters stepElementParameters, TasStepPassThroughData tasStepPassThroughData,
      ManifestOutcome tasManifestOutcome) {
    CustomManifestValuesFetchResponse customManifestValuesFetchResponse =
        (CustomManifestValuesFetchResponse) responseData;

    if (customManifestValuesFetchResponse.getCommandExecutionStatus() != SUCCESS) {
      CustomFetchResponsePassThroughData customFetchResponsePassThroughData =
          CustomFetchResponsePassThroughData.builder()
              .errorMsg(customManifestValuesFetchResponse.getErrorMessage())
              .unitProgressData(customManifestValuesFetchResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().chainEnd(true).passThroughData(customFetchResponsePassThroughData).build();
    }

    LogCallback logCallback = cdStepHelper.getLogCallback(
        CfCommandUnitConstants.FetchCustomFiles, ambiance, tasStepPassThroughData.getShouldOpenFetchFilesStream());
    logCallback.saveExecutionLog("Fetched all manifests from Custom remote", INFO, CommandExecutionStatus.SUCCESS);
    TasStepPassThroughData updatedTasStepPassThroughData =
        tasStepPassThroughData.toBuilder()
            .customFetchContent(customManifestValuesFetchResponse.getValuesFilesContentMap())
            .zippedManifestFileId(customManifestValuesFetchResponse.getZippedManifestFileId())
            .build();
    updatedTasStepPassThroughData.setShouldOpenFetchFilesStream(
        shouldOpenFetchFilesStream(updatedTasStepPassThroughData.getShouldOpenFetchFilesStream()));

    if (tasStepPassThroughData.getShouldExecuteGitStoreFetch()) {
      return prepareGitFetchTaskChainResponse(
          ambiance, stepElementParameters, updatedTasStepPassThroughData, tasManifestOutcome.getStore());
    }

    return executeTasTask(
        ambiance, stepElementParameters, tasStepExecutor, updatedTasStepPassThroughData, tasManifestOutcome);
  }

  public static boolean shouldOpenFetchFilesStream(Boolean openFetchFilesStream) {
    return openFetchFilesStream == null;
  }

  public void printFilesFetched(PcfManifestsPackage pcfManifestsPackage, LogCallback logCallback) {
    if (!isNull(pcfManifestsPackage)) {
      logCallback.saveExecutionLog(
          color(format("Tas Manifest File - %s", pcfManifestsPackage.getManifestYml()), LogColor.White));
      for (String varsFile : pcfManifestsPackage.getVariableYmls()) {
        logCallback.saveExecutionLog(color(format("Vars Manifest Files - %s", varsFile), LogColor.White));
      }
      logCallback.saveExecutionLog(color(
          format("AutoScaler Manifest File - %s", pcfManifestsPackage.getAutoscalarManifestYml()), LogColor.White));
    }
  }

  public Optional<FileStoreNodeDTO> validateAndFetchFileFromHarnessStore(
      String scopedFilePath, NGAccess ngAccess, String manifestIdentifier) {
    if (isBlank(scopedFilePath)) {
      throw new InvalidRequestException(
          format("File reference cannot be null or empty, manifest identifier: %s", manifestIdentifier));
    }
    FileReference fileReference = FileReference.of(
        scopedFilePath, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<FileStoreNodeDTO> manifestFile =
        fileStoreService.getWithChildrenByPath(fileReference.getAccountIdentifier(), fileReference.getOrgIdentifier(),
            fileReference.getProjectIdentifier(), fileReference.getPath(), true);
    if (!manifestFile.isPresent()) {
      throw new InvalidRequestException(
          format("File/Folder not found in File Store with path: [%s], scope: [%s], manifest identifier: [%s]",
              fileReference.getPath(), fileReference.getScope(), manifestIdentifier));
    }
    return manifestFile;
  }

  public Map<String, List<TasManifestFileContents>> getFileContentsForLocalStore(
      List<? extends ManifestOutcome> aggregatedManifestOutcomes, NGAccess ngAccess, LogCallback logCallback) {
    return aggregatedManifestOutcomes.stream()
        .filter(manifestOutcome -> ManifestStoreType.HARNESS.equals(manifestOutcome.getStore().getKind()))
        .collect(Collectors.toMap(ManifestOutcome::getIdentifier,
            manifestOutcome
            -> List.of(
                getFileContentsFromManifest(ngAccess, ((HarnessStore) manifestOutcome.getStore()).getFiles().getValue(),
                    manifestOutcome.getType(), manifestOutcome.getIdentifier(), logCallback))));
  }

  public TaskChainResponse prepareManifestFilesFetchTask(TasStepExecutor tasStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, TasStepPassThroughData tasStepPassThroughData) {
    StoreConfig storeConfig = tasStepPassThroughData.getTasManifestOutcome().getStore();
    tasStepPassThroughData.setShouldOpenFetchFilesStream(
        shouldOpenFetchFilesStream(tasStepPassThroughData.getShouldOpenFetchFilesStream()));
    if (tasStepPassThroughData.getShouldExecuteCustomFetch()) {
      return prepareCustomFetchManifestsTaskChainResponse(storeConfig, ambiance, stepElementParameters,
          tasStepPassThroughData.getManifestOutcomeList(), tasStepPassThroughData);
    }
    if (tasStepPassThroughData.getShouldExecuteGitStoreFetch()) {
      return prepareGitFetchTaskChainResponse(ambiance, stepElementParameters, tasStepPassThroughData, storeConfig);
    }
    return executeTasTask(ambiance, stepElementParameters, tasStepExecutor, tasStepPassThroughData,
        tasStepPassThroughData.getTasManifestOutcome());
  }

  protected TaskChainResponse prepareCustomFetchManifestsTaskChainResponse(StoreConfig storeConfig, Ambiance ambiance,
      StepElementParameters stepElementParameters, List<ManifestOutcome> manifestOutcomeList,
      TasStepPassThroughData tasStepPassThroughData) {
    LogCallback logCallback = cdStepHelper.getLogCallback(
        CfCommandUnitConstants.FetchCustomFiles, ambiance, tasStepPassThroughData.getShouldOpenFetchFilesStream());
    logCallback.saveExecutionLog(color(format("%nStarting Custom Fetch Files"), LogColor.White, LogWeight.Bold));

    String accountId = AmbianceUtils.getAccountId(ambiance);
    ParameterField<List<TaskSelectorYaml>> stepLevelSelectors = null;
    if (stepElementParameters.getSpec() instanceof TasCanaryAppSetupStepParameters) {
      stepLevelSelectors = ((TasCanaryAppSetupStepParameters) stepElementParameters.getSpec()).getDelegateSelectors();
    }
    List<TaskSelectorYaml> delegateSelectors = new ArrayList<>();

    if (!isEmpty(stepLevelSelectors.getValue())) {
      delegateSelectors.addAll(getParameterFieldValue(stepLevelSelectors));
    }

    CustomManifestSource customManifestSource = null;

    List<CustomManifestFetchConfig> fetchFilesList = new ArrayList<>();

    for (ManifestOutcome outcome : manifestOutcomeList) {
      if (ManifestStoreType.CUSTOM_REMOTE.equals(outcome.getStore().getKind())) {
        CustomRemoteStoreConfig store = (CustomRemoteStoreConfig) outcome.getStore();
        fetchFilesList.add(buildCustomManifestFetchConfig(outcome.getIdentifier(), true, false,
            Arrays.asList(store.getFilePath().getValue()), store.getExtractionScript().getValue(), accountId));
        if (!isEmpty(store.getDelegateSelectors().getValue())) {
          delegateSelectors.addAll(getParameterFieldValue(store.getDelegateSelectors()));
        }
      }
    }
    if (ManifestStoreType.CUSTOM_REMOTE.equals(storeConfig.getKind())) {
      TasManifestOutcome manifestOutcome = tasStepPassThroughData.getTasManifestOutcome();
      CustomRemoteStoreConfig customRemoteStoreConfig = (CustomRemoteStoreConfig) storeConfig;
      customManifestSource = CustomManifestSource.builder()
                                 .script(customRemoteStoreConfig.getExtractionScript().getValue())
                                 .filePaths(Arrays.asList(customRemoteStoreConfig.getFilePath().getValue()))
                                 .accountId(accountId)
                                 .build();

      if (!isEmpty(customRemoteStoreConfig.getDelegateSelectors().getValue())) {
        delegateSelectors.addAll(getParameterFieldValue(customRemoteStoreConfig.getDelegateSelectors()));
      }
      if (manifestOutcome.getVarsPaths().getValue() != null) {
        fetchFilesList.add(buildCustomManifestFetchConfig(
            manifestOutcome.getIdentifier(), true, true, manifestOutcome.getVarsPaths().getValue(), null, accountId));
      }
      if (manifestOutcome.getAutoScalerPath().getValue() != null) {
        fetchFilesList.add(buildCustomManifestFetchConfig(manifestOutcome.getIdentifier(), true, true,
            getParameterFieldValue(manifestOutcome.getAutoScalerPath()), null, accountId));
      }
    }

    CustomManifestValuesFetchParams customManifestValuesFetchRequest =
        CustomManifestValuesFetchParams.builder()
            .fetchFilesList(fetchFilesList)
            .activityId(ambiance.getStageExecutionId())
            .commandUnitName(CfCommandUnitConstants.FetchCustomFiles)
            .accountId(accountId)
            .shouldOpenLogStream(tasStepPassThroughData.getShouldOpenFetchFilesStream())
            .shouldCloseLogStream(tasStepPassThroughData.getShouldCloseFetchFilesStream())
            .customManifestSource(customManifestSource)
            .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.CUSTOM_MANIFEST_VALUES_FETCH_TASK_NG.name())
                                  .parameters(new Object[] {customManifestValuesFetchRequest})
                                  .build();

    String taskName = TaskType.CUSTOM_MANIFEST_VALUES_FETCH_TASK_NG.getDisplayName();

    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer, getCommandUnits(),
        taskName, TaskSelectorYaml.toTaskSelector(CollectionUtils.emptyIfNull(delegateSelectors)),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(tasStepPassThroughData)
        .build();
  }

  private CustomManifestFetchConfig buildCustomManifestFetchConfig(String identifier, boolean required,
      boolean defaultSource, List<String> filePaths, String script, String accountId) {
    return CustomManifestFetchConfig.builder()
        .key(identifier)
        .required(required)
        .defaultSource(defaultSource)
        .customManifestSource(
            CustomManifestSource.builder().script(script).filePaths(filePaths).accountId(accountId).build())
        .build();
  }

  protected TaskChainResponse getGitFetchFileTaskChainResponse(Ambiance ambiance,
      List<GitFetchFilesConfig> gitFetchFilesConfigs, StepElementParameters stepElementParameters,
      TasStepPassThroughData tasStepPassThroughData) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    GitFetchRequest gitFetchRequest = GitFetchRequest.builder()
                                          .gitFetchFilesConfigs(gitFetchFilesConfigs)
                                          .shouldOpenLogStream(tasStepPassThroughData.getShouldOpenFetchFilesStream())
                                          .closeLogStream(tasStepPassThroughData.getShouldCloseFetchFilesStream())
                                          .accountId(accountId)
                                          .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                                  .parameters(new Object[] {gitFetchRequest})
                                  .build();

    String taskName = TaskType.GIT_FETCH_NEXT_GEN_TASK.getDisplayName();
    ParameterField<List<TaskSelectorYaml>> stepLevelSelectors = null;
    if (stepElementParameters.getSpec() instanceof TasCanaryAppSetupStepParameters) {
      stepLevelSelectors = ((TasCanaryAppSetupStepParameters) stepElementParameters.getSpec()).getDelegateSelectors();
    }
    final TaskRequest taskRequest =
        prepareCDTaskRequest(ambiance, taskData, kryoSerializer, getCommandUnits(), taskName,
            TaskSelectorYaml.toTaskSelector(CollectionUtils.emptyIfNull(getParameterFieldValue(stepLevelSelectors))),
            stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(tasStepPassThroughData)
        .build();
  }

  private List<String> getCommandUnits() {
    return new ArrayList<>(Arrays.asList(CfCommandUnitConstants.FetchFiles, CfCommandUnitConstants.FetchCustomFiles,
        CfCommandUnitConstants.FetchGitFiles, CfCommandUnitConstants.VerifyManifests,
        CfCommandUnitConstants.CheckExistingApps, CfCommandUnitConstants.PcfSetup, CfCommandUnitConstants.Wrapup,
        CfCommandUnitConstants.Pcfplugin, CfCommandUnitConstants.Downsize, CfCommandUnitConstants.Upsize));
  }

  protected TaskChainResponse prepareGitFetchTaskChainResponse(Ambiance ambiance,
      StepElementParameters stepElementParameters, TasStepPassThroughData tasStepPassThroughData,
      StoreConfig storeConfig) {
    LogCallback logCallback = cdStepHelper.getLogCallback(
        CfCommandUnitConstants.FetchGitFiles, ambiance, tasStepPassThroughData.getShouldOpenFetchFilesStream());
    logCallback.saveExecutionLog(color(format("%nStarting Git Fetch Files"), LogColor.White, LogWeight.Bold));

    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        mapManifestsToGitFetchFileConfig(tasStepPassThroughData.getVarsManifestOutcomeList(),
            tasStepPassThroughData.getAutoScalerManifestOutcomeList(), ambiance);
    TasManifestOutcome tasManifestOutcome = tasStepPassThroughData.getTasManifestOutcome();

    if (ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
      if (TAS_MANIFEST.equals(tasManifestOutcome.getType()) && hasOnlyOne(gitStoreConfig.getPaths())) {
        String validationMessage = format("Vars Manifest YAML with Id [%s]", tasManifestOutcome.getIdentifier());
        gitFetchFilesConfigs.addAll(getManifestGitFetchFilesConfig(ambiance, tasManifestOutcome.getIdentifier(),
            tasManifestOutcome.getStore(), validationMessage, tasManifestOutcome));
      }
    }

    return getGitFetchFileTaskChainResponse(
        ambiance, gitFetchFilesConfigs, stepElementParameters, tasStepPassThroughData);
  }

  private boolean hasOnlyOne(ParameterField<List<String>> pathsParameter) {
    List<String> paths = getParameterFieldValue(pathsParameter);
    return isNotEmpty(paths) && paths.size() == 1;
  }

  public List<GitFetchFilesConfig> mapManifestsToGitFetchFileConfig(List<VarsManifestOutcome> aggregatedVarsManifests,
      List<AutoScalerManifestOutcome> aggregatedAutoScalerManifests, Ambiance ambiance) {
    List<GitFetchFilesConfig> gitFetchFilesConfigList =
        aggregatedVarsManifests.stream()
            .filter(varsManifestOutcome -> ManifestStoreType.isInGitSubset(varsManifestOutcome.getStore().getKind()))
            .map(varsManifestOutcome
                -> getGitFetchFilesConfig(ambiance, varsManifestOutcome.getStore(),
                    format("Vars YAML with Id [%s]", varsManifestOutcome.getIdentifier()), varsManifestOutcome))
            .collect(Collectors.toList());
    gitFetchFilesConfigList.addAll(
        aggregatedAutoScalerManifests.stream()
            .filter(autoScalerManifestOutcome
                -> ManifestStoreType.isInGitSubset(autoScalerManifestOutcome.getStore().getKind()))
            .map(autoScalerManifestOutcome
                -> getGitFetchFilesConfig(ambiance, autoScalerManifestOutcome.getStore(),
                    format("AutoScaler YAML with Id [%s]", autoScalerManifestOutcome.getIdentifier()),
                    autoScalerManifestOutcome))
            .collect(Collectors.toList()));
    return gitFetchFilesConfigList;
  }

  public GitFetchFilesConfig getGitFetchFilesConfig(
      Ambiance ambiance, StoreConfig store, String validationMessage, ManifestOutcome manifestOutcome) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = cdStepHelper.getConnector(connectorId, ambiance);
    cdStepHelper.validateManifest(store.getKind(), connectorDTO, validationMessage);

    List<String> gitFilePaths = getParameterFieldValue(gitStoreConfig.getPaths());

    return populateGitFetchFilesConfig(gitStoreConfig, manifestOutcome, manifestOutcome.getType(), connectorDTO,
        ambiance, manifestOutcome.getIdentifier(), gitFilePaths);
  }

  protected List<GitFetchFilesConfig> getManifestGitFetchFilesConfig(Ambiance ambiance, String identifier,
      StoreConfig store, String validationMessage, TasManifestOutcome tasManifestOutcome) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = cdStepHelper.getConnector(connectorId, ambiance);
    cdStepHelper.validateManifest(store.getKind(), connectorDTO, validationMessage);
    List<GitFetchFilesConfig> gitFetchFilesConfigList = new ArrayList<>();
    List<String> varsPaths = getParameterFieldValue(tasManifestOutcome.getVarsPaths());
    if (!isEmpty(varsPaths)) {
      gitFetchFilesConfigList.add(populateGitFetchFilesConfig(
          gitStoreConfig, tasManifestOutcome, TAS_VARS, connectorDTO, ambiance, identifier, varsPaths));
    }
    List<String> autoScalerPath = getParameterFieldValue(tasManifestOutcome.getAutoScalerPath());
    if (!isEmpty(autoScalerPath)) {
      gitFetchFilesConfigList.add(populateGitFetchFilesConfig(
          gitStoreConfig, tasManifestOutcome, TAS_AUTOSCALER, connectorDTO, ambiance, identifier, autoScalerPath));
    }
    gitFetchFilesConfigList.add(populateGitFetchFilesConfig(gitStoreConfig, tasManifestOutcome, TAS_MANIFEST,
        connectorDTO, ambiance, identifier, getParameterFieldValue(gitStoreConfig.getPaths())));
    return gitFetchFilesConfigList;
  }

  protected GitFetchFilesConfig populateGitFetchFilesConfig(GitStoreConfig gitStoreConfig,
      ManifestOutcome manifestOutcome, String manifestType, ConnectorInfoDTO connectorDTO, Ambiance ambiance,
      String identifier, List<String> gitFileValuesPaths) {
    if (isNotEmpty(gitFileValuesPaths)) {
      GitStoreDelegateConfig gitStoreDelegateConfig = cdStepHelper.getGitStoreDelegateConfig(
          gitStoreConfig, connectorDTO, manifestOutcome, gitFileValuesPaths, ambiance);
      return cdStepHelper.getGitFetchFilesConfigFromBuilder(identifier, manifestType, false, gitStoreDelegateConfig);
    }
    return null;
  }

  public TaskChainResponse executeTasTask(Ambiance ambiance, StepElementParameters stepElementParameters,
      TasStepExecutor tasStepExecutor, TasStepPassThroughData tasStepPassThroughData,
      ManifestOutcome tasManifestOutcome) {
    PcfManifestsPackage pcfManifestsPackage =
        getManifestFilesContents(ambiance, new HashMap<>(), tasStepPassThroughData.getCustomFetchContent(),
            tasStepPassThroughData.getLocalStoreFileMapContents(), tasStepPassThroughData.getManifestOutcomeList());

    Map<String,String> allFilesFetched = new HashMap<>();
    if(tasStepPassThroughData.getGitFetchFilesResultMap() != null && tasStepPassThroughData.getGitFetchFilesResultMap().values() != null) {
      for (FetchFilesResult entry : tasStepPassThroughData.getGitFetchFilesResultMap().values()) {
        if(entry.getFiles() != null) {
          entry.getFiles().stream().map(allFiles -> allFilesFetched.put(allFiles.getFilePath(), allFiles.getFileContent()));
        }
      }
    }
    if(tasStepPassThroughData.getLocalStoreFileMapContents() != null && tasStepPassThroughData.getLocalStoreFileMapContents().values() != null) {
      for (List<TasManifestFileContents> tasManifestFileContentsList : tasStepPassThroughData.getLocalStoreFileMapContents().values()) {
        for (TasManifestFileContents tasManifestFileContents : tasManifestFileContentsList) {
          for (int iterate = 0; iterate < tasManifestFileContents.getLocalStoreFetchFilesResult().getLocalStoreFilePaths().size(); iterate++) {
            allFilesFetched.put(tasManifestFileContents.getLocalStoreFetchFilesResult().getLocalStoreFilePaths().get(iterate),
                    tasManifestFileContents.getLocalStoreFetchFilesResult().getLocalStoreFileContents().get(iterate));
          }
        }
      }
    }

    List<UnitProgress> unitProgressList = Arrays.asList(UnitProgress.newBuilder()
            .setUnitName(CfCommandUnitConstants.FetchFiles)
            .setStatus(UnitStatus.SUCCESS)
            .setStartTime(System.currentTimeMillis() - 5)
            .setEndTime(System.currentTimeMillis())
            .build());

    if(tasStepPassThroughData.getRawScript() != null) {
      unitProgressList.add(0, UnitProgress.newBuilder()
              .setUnitName(CfCommandUnitConstants.FetchCommandScript)
              .setStatus(UnitStatus.SUCCESS)
              .setStartTime(System.currentTimeMillis() - 200)
              .setEndTime(System.currentTimeMillis() - 100)
              .build());
    }

    return tasStepExecutor.executeTasTask(tasManifestOutcome, ambiance, stepElementParameters,
        TasExecutionPassThroughData.builder()
            .applicationName(fetchTasApplicationName(pcfManifestsPackage))
            .infrastructure(tasStepPassThroughData.getInfrastructure())
            .zippedManifestId(tasStepPassThroughData.getZippedManifestFileId())
            .pcfManifestsPackage(pcfManifestsPackage)
            .repoRoot(tasStepPassThroughData.getRepoRoot())
            .cfCliVersion(tasStepPassThroughData.getTasManifestOutcome().getCfCliVersion())
            .pathsFromScript(tasStepPassThroughData.getPathsFromScript())
            .allFilesFetched(allFilesFetched)
            .build(),
        tasStepPassThroughData.getShouldOpenFetchFilesStream(),
        UnitProgressData.builder()
            .unitProgresses(unitProgressList)
            .build());
  }

  public PcfManifestsPackage getManifestFilesContents(Ambiance ambiance,
      Map<String, FetchFilesResult> gitFetchFilesResultMap,
      Map<String, Collection<CustomSourceFile>> customFetchContent,
      Map<String, List<TasManifestFileContents>> localStoreFetchFilesResultMap,
      List<ManifestOutcome> manifestOutcomes) {
    PcfManifestsPackage pcfManifestsPackage = PcfManifestsPackage.builder().variableYmls(new ArrayList<>()).build();
    for (ManifestOutcome manifest : manifestOutcomes) {
      String identifier = manifest.getIdentifier();
      if (isNotEmpty(gitFetchFilesResultMap) && gitFetchFilesResultMap.containsKey(identifier)) {
        FetchFilesResult gitFetchFilesResult = gitFetchFilesResultMap.get(identifier);
        if (!isNull(gitFetchFilesResult)) {
          if (manifest.getType().equals(TAS_MANIFEST)) {
            GitStoreConfig gitStoreConfig = (GitStoreConfig) manifest.getStore();
            for (GitFile file : gitFetchFilesResult.getFiles()) {
              if (getParameterFieldValue(gitStoreConfig.getPaths()).get(0).equals(file.getFilePath())) {
                addToPcfManifestPackageByType(pcfManifestsPackage, List.of(file.getFileContent()), TAS_MANIFEST);
              } else {
                List<String> varsPaths = ((TasManifestOutcome) manifest).getVarsPaths().getValue();
                if (!isEmpty(varsPaths) && varsPaths.contains(file.getFilePath())) {
                  addToPcfManifestPackageByType(pcfManifestsPackage, List.of(file.getFileContent()), TAS_VARS);
                } else {
                  addToPcfManifestPackageByType(pcfManifestsPackage, List.of(file.getFileContent()), TAS_AUTOSCALER);
                }
              }
            }
          } else {
            addToPcfManifestPackageByType(pcfManifestsPackage,
                gitFetchFilesResult.getFiles().stream().map(GitFile::getFileContent).collect(Collectors.toList()),
                manifest.getType());
          }
        }
      } else if (isNotEmpty(customFetchContent) && customFetchContent.containsKey(identifier)) {
        Collection<CustomSourceFile> customSourceFiles = customFetchContent.get(identifier);
        for (CustomSourceFile customSourceFile : customSourceFiles) {
          if (manifest.getType().equals(TAS_MANIFEST)) {
            CustomRemoteStoreConfig customRemoteStoreConfig = (CustomRemoteStoreConfig) manifest.getStore();
            if (customSourceFile.getFilePath().equals(customRemoteStoreConfig.getFilePath().getValue())) {
              addToPcfManifestPackageByType(
                  pcfManifestsPackage, List.of(customSourceFile.getFileContent()), TAS_MANIFEST);
            } else {
              List<String> varsPaths = ((TasManifestOutcome) manifest).getVarsPaths().getValue();
              if (!isEmpty(varsPaths) && varsPaths.contains(customSourceFile.getFilePath())) {
                addToPcfManifestPackageByType(pcfManifestsPackage, List.of(customSourceFile.getFileContent()), TAS_VARS);
              } else {
                addToPcfManifestPackageByType(
                    pcfManifestsPackage, List.of(customSourceFile.getFileContent()), TAS_AUTOSCALER);
              }
            }
          } else {
            addToPcfManifestPackageByType(
                pcfManifestsPackage, List.of(customSourceFile.getFileContent()), manifest.getType());
          }
        }
      } else if (isNotEmpty(localStoreFetchFilesResultMap) && localStoreFetchFilesResultMap.containsKey(identifier)) {
        List<TasManifestFileContents> localStoreValuesFileContent = localStoreFetchFilesResultMap.get(identifier);
        for (TasManifestFileContents tasManifestFileContent : localStoreValuesFileContent) {
          addToPcfManifestPackageByType(pcfManifestsPackage,
              tasManifestFileContent.getLocalStoreFetchFilesResult().getLocalStoreFileContents(),
              tasManifestFileContent.getManifestType());
        }
      }
    }
    return resolveExpressionsInManifests(ambiance, pcfManifestsPackage);
  }

  public PcfManifestsPackage resolveExpressionsInManifests(Ambiance ambiance, PcfManifestsPackage pcfManifestsPackage) {
    CDExpressionResolveFunctor cdExpressionResolveFunctor =
        new CDExpressionResolveFunctor(engineExpressionService, ambiance);
    PcfManifestsPackage resolvedPcfManifestsPackage = PcfManifestsPackage.builder().build();
    if (!isNull(pcfManifestsPackage.getAutoscalarManifestYml())) {
      resolvedPcfManifestsPackage.setAutoscalarManifestYml((String) ExpressionEvaluatorUtils.updateExpressions(
          pcfManifestsPackage.getAutoscalarManifestYml(), cdExpressionResolveFunctor));
    }
    if (!isEmpty(pcfManifestsPackage.getVariableYmls())) {
      List<String> resolvedVarsYaml = new ArrayList<>();
      for (String varsYaml : pcfManifestsPackage.getVariableYmls()) {
        resolvedVarsYaml.add((String) ExpressionEvaluatorUtils.updateExpressions(varsYaml, cdExpressionResolveFunctor));
      }
      resolvedPcfManifestsPackage.setVariableYmls(resolvedVarsYaml);
    }
    if (!isNull(pcfManifestsPackage.getManifestYml())) {
      resolvedPcfManifestsPackage.setManifestYml((String) ExpressionEvaluatorUtils.updateExpressions(
          pcfManifestsPackage.getManifestYml(), cdExpressionResolveFunctor));
    }
    return resolvedPcfManifestsPackage;
  }

  public void addToPcfManifestPackageByType(
      PcfManifestsPackage pcfManifestsPackage, List<String> fileContents, String manifestType) {
    switch (manifestType) {
      case TAS_AUTOSCALER:
        if (!isNull(pcfManifestsPackage.getAutoscalarManifestYml()) || fileContents.size() > 1) {
          throw new UnsupportedOperationException("Only one AutoScaler Yml is supported");
        }
        if (!fileContents.isEmpty()) {
          pcfManifestsPackage.setAutoscalarManifestYml(fileContents.get(0));
        }
        break;
      case TAS_VARS:
        if (isNull(pcfManifestsPackage.getVariableYmls())) {
          pcfManifestsPackage.setVariableYmls(new ArrayList<>());
        }
        if (!fileContents.isEmpty()) {
          pcfManifestsPackage.getVariableYmls().addAll(fileContents);
        }
        break;
      case TAS_MANIFEST:
        if (!isNull(pcfManifestsPackage.getManifestYml()) || fileContents.size() > 1) {
          throw new UnsupportedOperationException("Only one Tas Manifest Yml is supported");
        }
        if (!fileContents.isEmpty()) {
          pcfManifestsPackage.setManifestYml(fileContents.get(0));
        }
        break;
      default:
        throw new UnsupportedOperationException(format("Unsupported Manifest type: %s", manifestType));
    }
  }

  public void shouldExecuteStoreFetch(TasStepPassThroughData tasStepPassThroughData) {
    for (ManifestOutcome manifestOutcome : tasStepPassThroughData.getManifestOutcomeList()) {
      if (ManifestStoreType.CUSTOM_REMOTE.equals(manifestOutcome.getStore().getKind())) {
        tasStepPassThroughData.setShouldExecuteCustomFetch(true);
      } else if (ManifestStoreType.isInGitSubset(manifestOutcome.getStore().getKind())) {
        tasStepPassThroughData.setShouldExecuteGitStoreFetch(true);
      } else if (ManifestStoreType.HARNESS.equals(manifestOutcome.getStore().getKind())) {
        tasStepPassThroughData.setShouldExecuteHarnessStoreFetch(true);
      } else {
        throw new InvalidRequestException(
            format("Manifest store type: %s not supported yet", manifestOutcome.getStore().getKind()));
      }
    }
  }

  public String fetchTasApplicationName(PcfManifestsPackage pcfManifestsPackage) {
    String appName = null;
    Map<String, Object> applicationYamlMap = getApplicationYamlMap(pcfManifestsPackage.getManifestYml());
    String name = (String) applicationYamlMap.get(NAME_MANIFEST_YML_ELEMENT);
    if (isBlank(name)) {
      throw new InvalidArgumentsException(Pair.of("Manifest", "contains no application name"));
    }

    boolean hasVarFiles = isNotEmpty(pcfManifestsPackage.getVariableYmls());
    if (!hasVarFiles) {
      appName = name;
    } else {
      appName = finalizeSubstitution(pcfManifestsPackage, name);
    }
    return appName;
  }

  String finalizeSubstitution(PcfManifestsPackage pcfManifestsPackage, String name) {
    String varName;
    String appName;
    Matcher m = Pattern.compile("\\(\\(([^)]+)\\)\\)").matcher(name);
    List<String> varFiles = pcfManifestsPackage.getVariableYmls();
    while (m.find()) {
      varName = m.group(1);
      for (int i = varFiles.size() - 1; i >= 0; i--) {
        Object value = getVariableValue(varFiles.get(i), varName);
        if (value != null) {
          String val = value.toString();
          if (isNotBlank(val)) {
            name = name.replace("((" + varName + "))", val);
            break;
          }
        }
      }
    }
    appName = name;
    return appName;
  }

  @VisibleForTesting
  Map<String, Object> getApplicationYamlMap(String applicationManifestYmlContent) {
    Map<String, Object> yamlMap;
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      yamlMap = (Map<String, Object>) mapper.readValue(applicationManifestYmlContent, Map.class);
    } catch (Exception e) {
      throw new UnexpectedException("failed to get application Yaml Map", e);
    }

    List<Map> applicationsMaps = (List<Map>) yamlMap.get(APPLICATION_YML_ELEMENT);
    if (isEmpty(applicationsMaps)) {
      throw new InvalidArgumentsException(Pair.of("Manifest", "contains no application config"));
    }

    // Always assume, 1st app is main application being deployed.
    Map application = applicationsMaps.get(0);
    Map<String, Object> applicationConfigMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    applicationConfigMap.putAll(application);
    return applicationConfigMap;
  }

  public Object getVariableValue(String content, String key) {
    try {
      Map<String, Object> map = null;
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      map = mapper.readValue(content, Map.class);
      return map.get(key);
    } catch (Exception e) {
      throw new UnexpectedException("Failed while trying to substitute vars yml value", e);
    }
  }

  public Integer fetchMaxCountFromManifest(PcfManifestsPackage pcfManifestsPackage) {
    Map<String, Object> applicationYamlMap = getApplicationYamlMap(pcfManifestsPackage.getManifestYml());
    Map<String, Object> treeMap = generateCaseInsensitiveTreeMap(applicationYamlMap);
    Object maxCount = fetchInstanceCountFromWebProcess(treeMap);

    String maxVal;
    if (maxCount instanceof Integer) {
      maxVal = maxCount.toString();
    } else {
      maxVal = (String) maxCount;
    }

    if (isBlank(maxVal)) {
      return DEFAULT_INSTANCE_COUNT;
    }

    if (maxVal.contains("((") && maxVal.contains("))")) {
      if (isEmpty(pcfManifestsPackage.getVariableYmls())) {
        throw new InvalidRequestException(
            "No Valid Variable file Found, please verify var file is present and has valid structure");
      }
      maxVal = finalizeSubstitution(pcfManifestsPackage, maxVal);
    }
    return Integer.parseInt(maxVal);
  }

  private Map<String, Object> generateCaseInsensitiveTreeMap(Map<String, Object> map) {
    Map<String, Object> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    treeMap.putAll(map);
    return treeMap;
  }

  public Object fetchInstanceCountFromWebProcess(Map<String, Object> treeMap) {
    Object maxCount = null;
    Map<String, Object> webProcess = null;
    if (treeMap.containsKey(PROCESSES_MANIFEST_YML_ELEMENT)) {
      Object processes = treeMap.get(PROCESSES_MANIFEST_YML_ELEMENT);
      if (processes instanceof ArrayList<?>) {
        try {
          webProcess =
              ((ArrayList<Map<String, Object>>) processes)
                  .stream()
                  .filter(process -> {
                    if (!isNull(process) && process.containsKey(PROCESSES_TYPE_MANIFEST_YML_ELEMENT)) {
                      Object p = process.get(PROCESSES_TYPE_MANIFEST_YML_ELEMENT);
                      return (p instanceof String) && (p.toString().equals(WEB_PROCESS_TYPE_MANIFEST_YML_ELEMENT));
                    }
                    return false;
                  })
                  .findFirst()
                  .orElse(null);
          if (webProcess != null) {
            maxCount = webProcess.get(INSTANCE_MANIFEST_YML_ELEMENT);
          }
        } catch (Exception e) {
          log.warn("Unable to parse processes info in the manifest: {}", e.getMessage());
        }
      }
    }
    if (isNull(maxCount) && isNull(webProcess)) {
      maxCount = treeMap.get(INSTANCE_MANIFEST_YML_ELEMENT);
    }
    return maxCount;
  }

  public List<String> getRouteMaps(String applicationManifestYmlContent, List<String> additionalRoutesFromStep) {
    Map<String, Object> applicationConfigMap = getApplicationYamlMap(applicationManifestYmlContent);
    if (applicationConfigMap.containsKey(NO_ROUTE_MANIFEST_YML_ELEMENT)
        && (boolean) applicationConfigMap.get(NO_ROUTE_MANIFEST_YML_ELEMENT)) {
      return emptyList();
    }
    // fetch Routes element from application config
    List<String> allRoutes = new ArrayList<>();
    try {
      Object routeMaps = applicationConfigMap.get(ROUTES_MANIFEST_YML_ELEMENT);
      if (routeMaps != null) {
        allRoutes.addAll(((List<Map<String, String>>) routeMaps)
                             .stream()
                             .map(route -> route.get(ROUTE_MANIFEST_YML_ELEMENT))
                             .collect(Collectors.toList()));
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Invalid Route Format In Manifest");
    }
    if (!isNull(additionalRoutesFromStep)) {
      allRoutes.addAll(additionalRoutesFromStep);
    }
    return allRoutes;
  }
}