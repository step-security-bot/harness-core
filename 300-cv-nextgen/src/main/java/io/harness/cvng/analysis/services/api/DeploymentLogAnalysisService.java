package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface DeploymentLogAnalysisService {
  void save(DeploymentLogAnalysis deploymentLogAnalysisDTO);

  List<DeploymentLogAnalysis> getAnalysisResults(String verificationTaskId);

  List<LogAnalysisClusterChartDTO> getLogAnalysisClusters(String accountId, String verificationJobInstanceId);

  PageResponse<LogAnalysisClusterDTO> getLogAnalysisResult(
      String accountId, String verificationJobInstanceId, Integer label, int pageNumber);
}
