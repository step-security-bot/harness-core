/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.anomalydetection.alerts.service.impl;

import static io.harness.notification.NotificationChannelType.EMAIL;
import static io.harness.notification.NotificationChannelType.MSTEAMS;
import static io.harness.notification.NotificationChannelType.SLACK;
import static io.harness.notification.dtos.NotificationChannelDTO.NotificationChannelDTOBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.slack.api.webhook.WebhookPayloads.payload;

import io.harness.batch.processing.anomalydetection.alerts.SlackMessageGenerator;
import io.harness.batch.processing.anomalydetection.alerts.service.itfc.AnomalyAlertsService;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;
import io.harness.ccm.anomaly.utility.AnomalyUtility;
import io.harness.ccm.commons.dao.notifications.CCMNotificationsDao;
import io.harness.ccm.commons.entities.anomaly.AnomalyData;
import io.harness.ccm.commons.entities.notifications.CCMNotificationChannel;
import io.harness.ccm.commons.entities.notifications.CCMNotificationSetting;
import io.harness.ccm.commons.entities.notifications.CCMPerspectiveNotificationChannelsDTO;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.entities.CESlackWebhook;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.PerspectiveAnomalyService;
import io.harness.eraro.ResponseMessage;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.Team;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.EmailChannel.EmailChannelBuilder;
import io.harness.notification.channeldetails.MSTeamChannel;
import io.harness.notification.channeldetails.MSTeamChannel.MSTeamChannelBuilder;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.channeldetails.SlackChannel;
import io.harness.notification.channeldetails.SlackChannel.SlackChannelBuilder;
import io.harness.notification.dtos.NotificationChannelDTO;
import io.harness.notification.notificationclient.NotificationResult;
import io.harness.notifications.NotificationResourceClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.webhook.WebhookResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

@Service
@Singleton
@Slf4j
public class AnomalyAlertsServiceImpl implements AnomalyAlertsService {
  @Autowired @Inject private AnomalyService anomalyService;
  @Autowired @Inject private CESlackWebhookService ceSlackWebhookService;
  @Autowired @Inject private AccountShardService accountShardService;
  @Autowired @Inject private SlackMessageGenerator slackMessageGenerator;
  @Autowired @Inject private Slack slack;

  // NG Anomaly alerts
  @Autowired private CEViewService viewService;
  @Autowired private PerspectiveAnomalyService perspectiveAnomalyService;
  @Autowired private CCMNotificationsDao notificationSettingsDao;
  @Autowired private NotificationResourceClient notificationResourceClient;

  int MAX_RETRY = 3;

  public void sendAnomalyDailyReport(String accountId, Instant date) {
    if (slack == null) {
      slack = Slack.getInstance();
    }
    try {
      checkAndSendDailyReport(accountId, date);
    } catch (Exception e) {
      log.error("Can't send daily report for account : {}, Exception: ", accountId, e);
    }
  }

  private void checkAndSendDailyReport(String accountId, Instant date) {
    checkNotNull(accountId);
    CESlackWebhook slackWebhook = ceSlackWebhookService.getByAccountId(accountId);
    if (slackWebhook == null) {
      log.warn("The Account with id={} has no associated communication channels to send anomaly alerts.", accountId);
      return;
    }
    if (!slackWebhook.isSendAnomalyAlerts()) {
      log.info("The Account with id={} has anomaly alerts turned off", accountId);
      return;
    }
    try {
      sendDailyReportViaSlack(slackWebhook, date);
    } catch (IOException | SlackApiException e) {
      log.error("Unable to send slack daily notification  for account : [{}] Exception : [{}]", accountId, e);
    }
  }

  private void sendDailyReportViaSlack(CESlackWebhook slackWebhook, Instant date)
      throws IOException, SlackApiException {
    String accountId = slackWebhook.getAccountId();
    List<AnomalyEntity> anomalies = anomalyService.list(accountId, date);
    List<LayoutBlock> layoutBlocks;
    if (!anomalies.isEmpty()) {
      layoutBlocks = slackMessageGenerator.generateDailyReport(anomalies);
      int count = 0;
      while (count < MAX_RETRY) {
        try {
          WebhookResponse response = slack.send(
              slackWebhook.getWebhookUrl(), payload(p -> p.text("Harness CE Anomaly Alert").blocks(layoutBlocks)));
          count++;
          if (response.getCode() == 200) {
            log.info("slack daily anomalies notification sent successfully for accountId : {} ", accountId);
            break;
          }
        } catch (Exception e) {
          log.error("could not send daily anomalies notification via slack for accountId : {} , Exception : {} ",
              accountId, e);
        }
      }
    }
  }

  // Anomaly alerts Next Gen
  public void sendNgAnomalyAlerts(String accountId, Instant date) {
    if (slack == null) {
      slack = Slack.getInstance();
    }
    try {
      checkAndSendNgAnomalyAlerts(accountId, date);
    } catch (Exception e) {
      log.error("Can't send anomaly alerts for account : {}, Exception: ", accountId, e);
    }
  }

  private void checkAndSendNgAnomalyAlerts(String accountId, Instant date) {
    checkNotNull(accountId);
    log.info("Getting notification Channels");
    List<CCMPerspectiveNotificationChannelsDTO> notificationSettings =
        listNotificationChannelsPerPerspective(accountId);
    log.info("Notification settings: {}", notificationSettings);
    notificationSettings.forEach(
        notificationSetting -> {
          try {
            checkAndSendAnomalyAlertsForPerspective(notificationSetting, accountId, date);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  private void checkAndSendAnomalyAlertsForPerspective(
      CCMPerspectiveNotificationChannelsDTO perspectiveNotificationSetting, String accountId, Instant date) throws IOException {
    log.info("Sending NG anomaly alerts -----");
    log.info("Perspective Notification settings: {}", perspectiveNotificationSetting);
    List<AnomalyData> perspectiveAnomalies = perspectiveAnomalyService.listPerspectiveAnomaliesForDate(
        accountId, perspectiveNotificationSetting.getPerspectiveId(), date);

    log.info("Perspective anomalies: {}", perspectiveAnomalies);

    if (perspectiveAnomalies.isEmpty()) {
      return;
    }

    List<CCMNotificationChannel> channels = perspectiveNotificationSetting.getChannels();
    List<String> eMails = getChannelUrls(channels, EMAIL);
    log.info("Emails: {}", eMails);
    List<String> slackWebHookUrls = getChannelUrls(channels, SLACK);
    log.info("Slack webhooks: {}", slackWebHookUrls);
    List<String> msTeamKeys = getChannelUrls(channels, MSTEAMS);

    NotificationChannelDTOBuilder emailChannelBuilder = NotificationChannelDTO.builder()
                                                  .accountId(accountId)
                                                  .emailRecipients(eMails)
                                                  .team(Team.OTHER)
                                                  .templateId("email_ccm_anomaly_alert")
                                                  .userGroups(Collections.emptyList());

    NotificationChannelDTOBuilder slackChannelBuilder = NotificationChannelDTO.builder()
                                                  .accountId(accountId)
                                                  .webhookUrls(slackWebHookUrls)
                                                  .team(Team.OTHER)
                                                  .templateId("slack_ccm_anomaly_alert")
                                                  .userGroups(Collections.emptyList());

//    MSTeamChannelBuilder msTeamChannelBuilder = MSTeamChannel.builder()
//                                                    .accountId(accountId)
//                                                    .msTeamKeys(msTeamKeys)
//                                                    .team(Team.OTHER)
//                                                    .templateId("template-id-here")
//                                                    .userGroups(Collections.emptyList());

    Map<String, String> templateData = new HashMap<>();
    templateData.put("perspective_name", perspectiveNotificationSetting.getPerspectiveName());
//    templateData.put("anomalies", slackMessageGenerator.getAnomalyDetailsTemplateString(perspectiveAnomalies.get(0)));
    templateData.put("perspective_url", "https://google.co.in");

    // Sending email alerts
    emailChannelBuilder.templateData(templateData);
    Call<RestResponse<NotificationResult>> call = notificationResourceClient.sendNotification(accountId, emailChannelBuilder.build());
    Request request = call.request();
    log.info("REQUEST: {}", request);
    if (request.body()!=null) {
      log.info("REQUEST body: {}", request.body().toString());
    }
    log.info("REQUEST headers: {}", request.headers());
    log.info("REQUEST isHttps: {}", request.isHttps());
    log.info("REQUEST method: {}", request.method());
    log.info("REQUEST url: {}", request.url());
    log.info("REQUEST toString: {}", request.toString());
    Response<RestResponse<NotificationResult>> response = call.execute();
    log.info("RESPONSE isSuccessful: {}", response.isSuccessful());
    log.info("RESPONSE code: {}", response.code());
    log.info("RESPONSE headers: {}", response.headers());
    log.info("RESPONSE message: {}", response.message());
    log.info("RESPONSE raw: {}", response.raw());
    if (response.body() != null && response.body().getResponseMessages() !=null) {
      log.info("RESPONSE responseMessages: {}", response.body().getResponseMessages());
      if (!response.body().getResponseMessages().isEmpty()) {
        for(ResponseMessage responseMessage: response.body().getResponseMessages()) {
          log.info("RESPONSE in responseMessages message: {}", responseMessage.getMessage());
        }
      }
    }
    if (response.errorBody() != null) {
      log.info("RESPONSE errorBody: {}", response.errorBody().string());
    }
    log.info("RESPONSE body: {}", response.body());

    Map<String, String> slackTemplateData = new HashMap<>();
    slackTemplateData.put("perspective_name", perspectiveNotificationSetting.getPerspectiveName());
    slackTemplateData.put("count_of_anomalies", String.valueOf(perspectiveAnomalies.size()));
    slackTemplateData.put("date", AnomalyUtility.convertInstantToDate2(date));
    StringBuilder anomaliesDetails = new StringBuilder();
    for(int i=0;  i < perspectiveAnomalies.size(); i++) {
      anomaliesDetails.append(slackMessageGenerator.getAnomalyDetailsTemplateString(perspectiveAnomalies.get(i)));
      if (i < perspectiveAnomalies.size() - 1) {
        anomaliesDetails.append(" \n\n");
      }
    }
    slackTemplateData.put("anomalies_details", anomaliesDetails.toString());

    // Sending slack alerts
    slackChannelBuilder.templateData(slackTemplateData);
    call = notificationResourceClient.sendNotification(accountId, slackChannelBuilder.build());
    request = call.request();
    log.info("REQUEST: {}", request);
    log.info("REQUEST body: {}", request.body());
    if (request.body()!=null) {
      log.info("REQUEST body: {}", request.body().toString());
    }
    log.info("REQUEST headers: {}", request.headers());
    log.info("REQUEST isHttps: {}", request.isHttps());
    log.info("REQUEST method: {}", request.method());
    log.info("REQUEST url: {}", request.url());
    log.info("REQUEST toString: {}", request.toString());
    response = call.execute();
    log.info("RESPONSE isSuccessful: {}", response.isSuccessful());
    log.info("RESPONSE code: {}", response.code());
    log.info("RESPONSE headers: {}", response.headers());
    log.info("RESPONSE message: {}", response.message());
    log.info("RESPONSE raw: {}", response.raw());
    if (response.body() != null && response.body().getResponseMessages() !=null) {
      log.info("RESPONSE responseMessages: {}", response.body().getResponseMessages());
      if (!response.body().getResponseMessages().isEmpty()) {
        for(ResponseMessage responseMessage: response.body().getResponseMessages()) {
          log.info("RESPONSE in responseMessages message: {}", responseMessage.getMessage());
        }
      }
    }
    if (response.errorBody() != null) {
      log.info("RESPONSE errorBody: {}", response.errorBody().string());
    }
    log.info("RESPONSE body: {}", response.body());
  }

  public List<CCMPerspectiveNotificationChannelsDTO> listNotificationChannelsPerPerspective(String accountId) {
    List<CCMNotificationSetting> notificationSettings = notificationSettingsDao.list(accountId);
    List<String> perspectiveIds =
        notificationSettings.stream().map(CCMNotificationSetting::getPerspectiveId).collect(Collectors.toList());
    Map<String, String> perspectiveIdToNameMapping =
        viewService.getPerspectiveIdToNameMapping(accountId, perspectiveIds);
    List<CCMPerspectiveNotificationChannelsDTO> perspectiveNotificationChannels = new ArrayList<>();
    notificationSettings.forEach(notificationSetting
        -> perspectiveNotificationChannels.add(
            CCMPerspectiveNotificationChannelsDTO.builder()
                .perspectiveId(notificationSetting.getPerspectiveId())
                .perspectiveName(perspectiveIdToNameMapping.get(notificationSetting.getPerspectiveId()))
                .channels(notificationSetting.getChannels())
                .build()));
    return perspectiveNotificationChannels;
  }

  private List<String> getChannelUrls(List<CCMNotificationChannel> channels, NotificationChannelType channelType) {
    List<CCMNotificationChannel> relevantChannels =
        channels.stream()
            .filter(channel -> channel.getNotificationChannelType() == channelType)
            .collect(Collectors.toList());
    List<String> channelUrls = new ArrayList<>();
    relevantChannels.forEach(channel -> channelUrls.addAll(channel.getChannelUrls()));
    return channelUrls;
  }
}
