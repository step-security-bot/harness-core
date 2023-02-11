/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cistatus.service.bitbucket;

import static java.lang.String.format;

import io.harness.cistatus.StatusCreationResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.git.GitClientHelper;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Singleton
@Slf4j
public class BitbucketServiceImpl implements BitbucketService {
  private static final String STATE = "state";
  private static final String MERGED = "merged";
  private static final String ERROR = "error";
  private static final String SHA = "sha";

  @Override
  public boolean sendStatus(BitbucketConfig bitbucketConfig, String userName, String token,
      List<EncryptedDataDetail> encryptionDetails, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap) {
    log.info("Sending status {} for sha {}", bodyObjectMap.get(STATE), sha);

    try {
      Response<StatusCreationResponse> statusCreationResponseResponse;

      if (!GitClientHelper.isBitBucketSAAS(bitbucketConfig.getBitbucketUrl())) {
        statusCreationResponseResponse =
            getBitbucketClient(bitbucketConfig, encryptionDetails)
                .createOnPremStatus(getHeaderWithCredentials(token, userName), sha, bodyObjectMap)
                .execute();
      } else {
        statusCreationResponseResponse =
            getBitbucketClient(bitbucketConfig, encryptionDetails)
                .createStatus(getHeaderWithCredentials(token, userName), owner, repo, sha, bodyObjectMap)
                .execute();
      }

      if (!statusCreationResponseResponse.isSuccessful()) {
        log.error("Failed to send status for bitbucket url {} and sha {} error {}, message {}",
            bitbucketConfig.getBitbucketUrl(), sha, statusCreationResponseResponse.errorBody().string(),
            statusCreationResponseResponse.message());
      }

      return statusCreationResponseResponse.isSuccessful();

    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Failed to send status for Bitbucket url %s and sha %s ", bitbucketConfig.getBitbucketUrl(), sha), e);
    }
  }

  @Override
  public JSONObject mergePR(BitbucketConfig bitbucketConfig, String token, String userName, String org, String repoSlug,
      String prNumber, boolean deleteSourceBranch, String ref, boolean isSaaS) {
    String authToken = getHeaderWithCredentials(token, userName);
    JSONObject responseObj;
    if (isSaaS) {
      responseObj = mergeSaaSPR(bitbucketConfig, authToken, org, repoSlug, prNumber, deleteSourceBranch);
    } else {
      responseObj = mergeOnPremPR(bitbucketConfig, authToken, org, repoSlug, prNumber, ref, deleteSourceBranch);
    }
    return responseObj;
  }

  private JSONObject mergeOnPremPR(BitbucketConfig bitbucketConfig, String authToken, String org, String repoSlug,
      String prNumber, String ref, boolean deleteSourceBranch) {
    JSONObject responseObj = new JSONObject();
    Map<String, Object> parameters = new HashMap<>();
    String url = bitbucketConfig.getBitbucketUrl();
    try {
      Response<Object> mergePRResponse = getBitbucketClient(bitbucketConfig, null)
                                             .mergeOnPremPR(authToken, org, repoSlug, prNumber, parameters)
                                             .execute();
      if (mergePRResponse.isSuccessful()) {
        responseObj.put(SHA, getOnPremMergeCommit(mergePRResponse));
        responseObj.put(MERGED, true);
        if (deleteSourceBranch) {
          // if merge is successful, delete source branch
          boolean isBranchDeleted = deleteRef(bitbucketConfig, authToken, org, repoSlug, ref);
          if (!isBranchDeleted) {
            log.error(
                "Error encountered when deleting source branch {} of the pull request {}. URL {}", ref, prNumber, url);
            // Not failing the merge for failure to delete source branch
          }
        }
      } else {
        log.error("Failed to merge PR for Bitbucket Server. URL {} and PR number {}. Response {} ", url, prNumber,
            mergePRResponse.errorBody());
        responseObj.put(ERROR, getErrorMessage(mergePRResponse));
        responseObj.put("code", mergePRResponse.code());
        responseObj.put(MERGED, false);
      }
    } catch (Exception e) {
      log.error("Failed to merge PR for Bitbucket Server. URL {} and PR number {} ", url, prNumber, e);
      responseObj.put(MERGED, false);
      responseObj.put(ERROR, e.getMessage());
    }
    return responseObj;
  }

  private Object getErrorMessage(Response<Object> mergePRResponse) {
    JSONObject errObject = null;
    try {
      errObject = new JSONObject(mergePRResponse.errorBody().string());
      return ((JSONObject) ((JSONArray) errObject.get("errors")).get(0)).get("message");
    } catch (Exception e) {
      log.error("Failed to get error message from merge response. Error {}", e.getMessage());
      return "Failed to get error message from merge response";
    }
  }

  private Object getOnPremMergeCommit(Response<Object> mergePRResponse) {
    return (
        (LinkedHashMap) ((LinkedHashMap) ((LinkedHashMap) mergePRResponse.body()).get("properties")).get("mergeCommit"))
        .get("id");
  }

  private JSONObject mergeSaaSPR(BitbucketConfig bitbucketConfig, String authToken, String org, String repoSlug,
      String prNumber, boolean deleteSourceBranch) {
    JSONObject responseObj = new JSONObject();
    Map<String, Object> parameters = new HashMap<>();
    if (deleteSourceBranch) {
      parameters.put("close_source_branch", true);
    }
    try {
      Response<Object> mergePRResponse = getBitbucketClient(bitbucketConfig, null)
                                             .mergeSaaSPR(authToken, org, repoSlug, prNumber, parameters)
                                             .execute();
      if (mergePRResponse.isSuccessful()) {
        responseObj.put(SHA, getSaaSMergeCommit(mergePRResponse));
        responseObj.put(MERGED, true);
      } else {
        log.error("Failed to merge PR for Bitbucket Cloud. URL {} and PR number {}. Response {} ",
            bitbucketConfig.getBitbucketUrl(), prNumber, mergePRResponse.errorBody());
        responseObj.put(ERROR, getErrorMessage(mergePRResponse));
        responseObj.put("code", mergePRResponse.code());
        responseObj.put(MERGED, false);
      }
    } catch (Exception e) {
      log.error("Failed to merge PR for Bitbucket Cloud. URL {} and PR number {} ", bitbucketConfig.getBitbucketUrl(),
          prNumber, e);
      responseObj.put(MERGED, false);
      responseObj.put(ERROR, e.getMessage());
    }
    return responseObj;
  }

  private Object getSaaSMergeCommit(Response<Object> mergePRResponse) {
    return ((LinkedHashMap) ((LinkedHashMap) mergePRResponse.body()).get("merge_commit")).get("hash");
  }

  @Override
  public Boolean deleteRef(BitbucketConfig bitbucketConfig, String authToken, String org, String repoSlug, String ref) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("name", ref);
    parameters.put("dryRun", false);
    try {
      Response<Object> response =
          getBitbucketClient(bitbucketConfig, null).deleteOnPremRef(authToken, org, repoSlug, parameters).execute();

      if (response.isSuccessful()) {
        return true;
      }
    } catch (Exception e) {
      log.error(
          "Failed to delete ref for Bitbucket Server. URL {} and ref {} ", bitbucketConfig.getBitbucketUrl(), ref, e);
    }
    return false;
  }

  @VisibleForTesting
  public BitbucketRestClient getBitbucketClient(
      BitbucketConfig bitbucketConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      String bitbucketUrl = bitbucketConfig.getBitbucketUrl();
      Preconditions.checkNotNull(bitbucketUrl, "Bitbucket api url is null");
      if (!bitbucketUrl.endsWith("/")) {
        bitbucketUrl = bitbucketUrl + "/";
      }
      Retrofit retrofit = new Retrofit.Builder()
                              .baseUrl(bitbucketUrl)
                              .addConverterFactory(JacksonConverterFactory.create())
                              .client(Http.getUnsafeOkHttpClient(bitbucketUrl))
                              .build();
      return retrofit.create(BitbucketRestClient.class);
    } catch (InvalidRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(
          "Failed to post commit status request to bitbucket server :" + bitbucketConfig.getBitbucketUrl(), e);
    }
  }

  private String getHeaderWithCredentials(String token, String userName) {
    return "Basic " + Base64.encodeBase64String(format("%s:%s", userName, token).getBytes(StandardCharsets.UTF_8));
  }
}
