/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.records;

import io.harness.timescaledb.tables.TimeBucketListCdStatus;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.TableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TimeBucketListCdStatusRecord
    extends TableRecordImpl<TimeBucketListCdStatusRecord> implements Record2<String, Long> {
  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.time_bucket_list_cd_status.status</code>.
   */
  public TimeBucketListCdStatusRecord setStatus(String value) {
    set(0, value);
    return this;
  }

  /**
   * Getter for <code>public.time_bucket_list_cd_status.status</code>.
   */
  public String getStatus() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.time_bucket_list_cd_status.startts</code>.
   */
  public TimeBucketListCdStatusRecord setStartts(Long value) {
    set(1, value);
    return this;
  }

  /**
   * Getter for <code>public.time_bucket_list_cd_status.startts</code>.
   */
  public Long getStartts() {
    return (Long) get(1);
  }

  // -------------------------------------------------------------------------
  // Record2 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row2<String, Long> fieldsRow() {
    return (Row2) super.fieldsRow();
  }

  @Override
  public Row2<String, Long> valuesRow() {
    return (Row2) super.valuesRow();
  }

  @Override
  public Field<String> field1() {
    return TimeBucketListCdStatus.TIME_BUCKET_LIST_CD_STATUS.STATUS;
  }

  @Override
  public Field<Long> field2() {
    return TimeBucketListCdStatus.TIME_BUCKET_LIST_CD_STATUS.STARTTS;
  }

  @Override
  public String component1() {
    return getStatus();
  }

  @Override
  public Long component2() {
    return getStartts();
  }

  @Override
  public String value1() {
    return getStatus();
  }

  @Override
  public Long value2() {
    return getStartts();
  }

  @Override
  public TimeBucketListCdStatusRecord value1(String value) {
    setStatus(value);
    return this;
  }

  @Override
  public TimeBucketListCdStatusRecord value2(Long value) {
    setStartts(value);
    return this;
  }

  @Override
  public TimeBucketListCdStatusRecord values(String value1, Long value2) {
    value1(value1);
    value2(value2);
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached TimeBucketListCdStatusRecord
   */
  public TimeBucketListCdStatusRecord() {
    super(TimeBucketListCdStatus.TIME_BUCKET_LIST_CD_STATUS);
  }

  /**
   * Create a detached, initialised TimeBucketListCdStatusRecord
   */
  public TimeBucketListCdStatusRecord(String status, Long startts) {
    super(TimeBucketListCdStatus.TIME_BUCKET_LIST_CD_STATUS);

    setStatus(status);
    setStartts(startts);
  }
}
