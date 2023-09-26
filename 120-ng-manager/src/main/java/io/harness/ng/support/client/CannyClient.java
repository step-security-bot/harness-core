/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.support.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.ng.support.dto.CannyBoardsResponseDTO;
import io.harness.ng.support.dto.CannyPostResponseDTO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class CannyClient {
  private CannyConfig cannyConfig;

  public static final String CANNY_BASE_URL = "https://canny.io";
  public static final String ID_NODE = "id";
  public static final String NAME_NODE = "name";
  public static final String BOARDS_NODE = "boards";
  public static final String ADMIN_BOARD_NAME = "Test Board - only admins can see";

  public static final ObjectMapper objectMapper = new ObjectMapper();
  public OkHttpClient okHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();

  @Inject
  public CannyClient(@Named("cannyApiConfiguration") CannyConfig cannyConfig) {
    this.cannyConfig = cannyConfig;
  }

  public CannyBoardsResponseDTO getBoards() {
    try (Response response = getCannyBoardsResponse()) {
      if (!response.isSuccessful()) {
        String bodyString = (null != response.body()) ? response.body().string() : null;
        log.error("Failed to retrieve boards from Canny. Response body: {}", bodyString);
        throw new UnexpectedException("Failed to retrieve boards from Canny. Response body: " + bodyString);
      }

      JsonNode jsonResponse = objectMapper.readTree(response.body().byteStream());
      JsonNode boardsNode = jsonResponse.get(BOARDS_NODE);
      List<CannyBoardsResponseDTO.Board> boardsList = new ArrayList<>();

      for (JsonNode boardNode : boardsNode) {
        String id = boardNode.get(ID_NODE).asText();
        String name = boardNode.get(NAME_NODE).asText();

        // ------------------------------------------------------------------------------------------------
        // Canny doesn't provide a way to check if a board is admin view only
        // TODO: A support ticket has been raised at Canny to add this feature, this section can be removed once this
        // is handled as this is the only admin-only board.
        if (Objects.equals(name, ADMIN_BOARD_NAME)) {
          continue;
        }
        // ------------------------------------------------------------------------------------------------

        CannyBoardsResponseDTO.Board board = CannyBoardsResponseDTO.Board.builder().name(name).id(id).build();

        boardsList.add(board);
      }

      return CannyBoardsResponseDTO.builder().boards(boardsList).build();

    } catch (Exception e) {
      log.error("Exception occurred while fetching boards at getBoards(): {}", e);
      throw new UnexpectedException("Exception occurred while fetching boards from Canny: ", e);
    }
  }
  public CannyPostResponseDTO createPost(String emailId, String name, String title, String details, String boardId) {
    try {
      String authorId = getPostCreationAuthorId(emailId, name);

      Response response = createCannyPost(authorId, boardId, title, details);
      if (!response.isSuccessful()) {
        log.error("Request to canny failed trying to create post. Response body: {}", response.body().string());
        throw new UnexpectedException(
            "Request to canny failed trying to create post. Response body: " + response.body().string());
      }

      String postId = objectMapper.readTree(response.body().byteStream()).get("id").asText();
      Response postDetailsResponse = retrieveCannyPostDetails(postId);
      if (!postDetailsResponse.isSuccessful()) {
        log.error("Request to canny failed trying to retrieve post details. Response body: {}",
            postDetailsResponse.body().string());
        throw new UnexpectedException("Request to canny failed trying to retrieve post details. Response body: "
            + postDetailsResponse.body().string());
      }
      String postUrl = objectMapper.readTree(postDetailsResponse.body().byteStream()).get("url").asText();
      return CannyPostResponseDTO.builder().postURL(postUrl).message("Post created successfully").build();

    } catch (Exception e) {
      log.error("Exception occurred while creating post at createPost()", e);
      throw new UnexpectedException("Exception occurred while creating post at createPost(): ", e);
    }
  }

  public String getPostCreationAuthorId(String emailId, String name) {
    try {
      Response response = retrieveCannyUser(emailId);
      JsonNode jsonResponse = objectMapper.readTree(response.body().byteStream());

      // if user doesn't exist on canny(checked through email since it is a unique entity on harness), create user
      if (!response.isSuccessful() && response.code() != 500
          && jsonResponse.get("error").asText().equals("invalid email")) {
        // use harness emailId as unique Identifier for canny
        Response createUserResponse = createCannyUser(emailId, emailId, name);

        if (!createUserResponse.isSuccessful()) {
          log.error("Request to canny failed trying to create user at getPostCreationAuthorId(). Response body: {}",
              response.body().string());
          throw new UnexpectedException(
              "Request to canny failed trying to create user at getPostCreationAuthorId(). Response body: "
              + response.body().string());
        }

        JsonNode createUserResponseJson = objectMapper.readTree(createUserResponse.body().byteStream());
        JsonNode id = createUserResponseJson.get("id");

        return id.asText();

      } else {
        // user was successfully retrieved
        return jsonResponse.get("id").asText();
      }
    } catch (Exception e) {
      log.error("Exception occurred while retrieving user at getPostCreationAuthorId(): {}", e);
      throw new UnexpectedException("Exception occurred while retrieving user at getPostCreationAuthorId(): ", e);
    }
  }

  public Response getCannyBoardsResponse() {
    try {
      String jsonRequestBody = "{\"apiKey\":\"" + cannyConfig.token + "\"}";
      RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonRequestBody);
      Request request = new Request.Builder().url(CANNY_BASE_URL + "/api/v1/boards/list").post(requestBody).build();

      return okHttpClient.newCall(request).execute();

    } catch (Exception e) {
      log.error("Exception occurred while making call to canny to fetch boards: {}", e);
      throw new UnexpectedException("Exception occurred while making call to canny to fetch boards: ", e);
    }
  }
  public Response retrieveCannyUser(String emailId) {
    try {
      FormBody.Builder formBodyBuilder = new FormBody.Builder().add("apiKey", cannyConfig.token).add("email", emailId);
      Request request =
          new Request.Builder().url(CANNY_BASE_URL + "/api/v1/users/retrieve").post(formBodyBuilder.build()).build();

      return okHttpClient.newCall(request).execute();

    } catch (Exception e) {
      log.error("Exception occurred while retrieving user from canny at retrieveCannyUser(): {}", e);
      throw new UnexpectedException("Exception occurred while retrieving user from canny at retrieveCannyUser(): ", e);
    }
  }

  public Response createCannyUser(String userId, String email, String name) {
    try {
      FormBody.Builder formBodyBuiler = new FormBody.Builder()
                                            .add("apiKey", cannyConfig.token)
                                            .add("userID", userId)
                                            .add("email", email)
                                            .add("name", name);

      Request request = new Request.Builder()
                            .url(CANNY_BASE_URL + "/api/v1/users/create_or_update")
                            .post(formBodyBuiler.build())
                            .build();

      return okHttpClient.newCall(request).execute();

    } catch (Exception e) {
      log.error("Exception occurred while creating canny user at createCannyUser() : {}", e.getMessage(), e);
      throw new UnexpectedException("Exception occurred while creating canny user at createCannyUser() : ", e);
    }
  }

  public Response createCannyPost(String authorId, String boardId, String title, String details) {
    try {
      FormBody.Builder formBodyBuilder = new FormBody.Builder()
                                             .add("apiKey", cannyConfig.token)
                                             .add("authorID", authorId)
                                             .add("boardID", boardId)
                                             .add("details", details)
                                             .add("title", title);

      Request request =
          new Request.Builder().url(CANNY_BASE_URL + "/api/v1/posts/create").post(formBodyBuilder.build()).build();
      return okHttpClient.newCall(request).execute();
    } catch (Exception e) {
      log.error(
          "Exception occurred while making createPost request to Canny at createCannyPost(): {}", e.getMessage(), e);
      throw new UnexpectedException(
          "Exception occurred while making createPost request to Canny at createCannyPost(): ", e);
    }
  }

  public Response retrieveCannyPostDetails(String postId) {
    try {
      FormBody.Builder formBodyBuilder = new FormBody.Builder().add("apiKey", cannyConfig.token).add("id", postId);

      Request request =
          new Request.Builder().url(CANNY_BASE_URL + "/api/v1/posts/retrieve").post(formBodyBuilder.build()).build();
      return okHttpClient.newCall(request).execute();
    } catch (Exception e) {
      log.error(
          "Exception occurred while making retrieveCannyPostDetails request to Canny at retrieveCannyPostDetails(): {}",
          e.getMessage(), e);
      throw new UnexpectedException(
          "Exception occurred while making retrieveCannyPostDetails request to Canny at retrieveCannyPostDetails(): ",
          e);
    }
  }
}
