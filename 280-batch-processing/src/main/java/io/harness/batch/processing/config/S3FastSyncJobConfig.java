/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.config;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.reader.SettingAttributeReader;
import io.harness.batch.processing.svcmetrics.BatchJobExecutionListener;
import io.harness.batch.processing.writer.S3FastSyncEventWriter;

import software.wings.beans.SettingAttribute;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class S3FastSyncJobConfig {
  private static final int BATCH_SIZE = 10;

  @Autowired private JobBuilderFactory jobBuilderFactory;
  @Autowired private StepBuilderFactory stepBuilderFactory;
  @Autowired private BatchJobExecutionListener batchJobExecutionListener;

  @Bean
  @Qualifier(value = "s3FastSyncJob")
  public Job s3FastSyncJob(JobBuilderFactory jobBuilderFactory, Step s3FastSyncStep) {
    return jobBuilderFactory.get(BatchJobType.SYNC_BILLING_REPORT_S3_FAST.name())
        .incrementer(new RunIdIncrementer())
        .listener(batchJobExecutionListener)
        .start(s3FastSyncStep)
        .build();
  }

  @Bean
  public Step s3FastSyncStep(StepBuilderFactory stepBuilderFactory, SettingAttributeReader settingAttributeReader) {
    return stepBuilderFactory.get("s3FastSyncStep")
        .<SettingAttribute, SettingAttribute>chunk(BATCH_SIZE)
        .reader(settingAttributeReader)
        .writer(s3FastSyncWriter())
        .build();
  }

  @Bean
  public ItemWriter<SettingAttribute> s3FastSyncWriter() {
    return new S3FastSyncEventWriter();
  }
}
