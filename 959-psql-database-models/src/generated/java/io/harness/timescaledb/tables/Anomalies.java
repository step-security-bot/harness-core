/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Indexes;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.AnomaliesRecord;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Anomalies extends TableImpl<AnomaliesRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.anomalies</code>
   */
  public static final Anomalies ANOMALIES = new Anomalies();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<AnomaliesRecord> getRecordType() {
    return AnomaliesRecord.class;
  }

  /**
   * The column <code>public.anomalies.id</code>.
   */
  public final TableField<AnomaliesRecord, String> ID =
      createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.anomalies.accountid</code>.
   */
  public final TableField<AnomaliesRecord, String> ACCOUNTID =
      createField(DSL.name("accountid"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.anomalies.actualcost</code>.
   */
  public final TableField<AnomaliesRecord, Double> ACTUALCOST =
      createField(DSL.name("actualcost"), SQLDataType.DOUBLE.nullable(false), this, "");

  /**
   * The column <code>public.anomalies.expectedcost</code>.
   */
  public final TableField<AnomaliesRecord, Double> EXPECTEDCOST =
      createField(DSL.name("expectedcost"), SQLDataType.DOUBLE.nullable(false), this, "");

  /**
   * The column <code>public.anomalies.anomalytime</code>.
   */
  public final TableField<AnomaliesRecord, OffsetDateTime> ANOMALYTIME =
      createField(DSL.name("anomalytime"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), this, "");

  /**
   * The column <code>public.anomalies.timegranularity</code>.
   */
  public final TableField<AnomaliesRecord, String> TIMEGRANULARITY =
      createField(DSL.name("timegranularity"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.anomalies.note</code>.
   */
  public final TableField<AnomaliesRecord, String> NOTE = createField(DSL.name("note"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.clusterid</code>.
   */
  public final TableField<AnomaliesRecord, String> CLUSTERID =
      createField(DSL.name("clusterid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.clustername</code>.
   */
  public final TableField<AnomaliesRecord, String> CLUSTERNAME =
      createField(DSL.name("clustername"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.workloadname</code>.
   */
  public final TableField<AnomaliesRecord, String> WORKLOADNAME =
      createField(DSL.name("workloadname"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.workloadtype</code>.
   */
  public final TableField<AnomaliesRecord, String> WORKLOADTYPE =
      createField(DSL.name("workloadtype"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.namespace</code>.
   */
  public final TableField<AnomaliesRecord, String> NAMESPACE =
      createField(DSL.name("namespace"), SQLDataType.CLOB, this, "");

  /**

  /**
   * The column <code>public.anomalies.service</code>.
   */
  public final TableField<AnomaliesRecord, String> SERVICE =
      createField(DSL.name("service"), SQLDataType.CLOB, this, "");

  /**

   * The column <code>public.anomalies.servicename</code>.
   */

  public final TableField<AnomaliesRecord, String> SERVICENAME =
      createField(DSL.name("servicename"), SQLDataType.CLOB, this, "");

  /**

   * The column <code>public.anomalies.region</code>.
   */

  public final TableField<AnomaliesRecord, String> REGION = createField(DSL.name("region"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.gcpproduct</code>.
   */
  public final TableField<AnomaliesRecord, String> GCPPRODUCT =
      createField(DSL.name("gcpproduct"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.gcpskuid</code>.
   */
  public final TableField<AnomaliesRecord, String> GCPSKUID =
      createField(DSL.name("gcpskuid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.gcpskudescription</code>.
   */
  public final TableField<AnomaliesRecord, String> GCPSKUDESCRIPTION =
      createField(DSL.name("gcpskudescription"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.gcpproject</code>.
   */
  public final TableField<AnomaliesRecord, String> GCPPROJECT =
      createField(DSL.name("gcpproject"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.awsservice</code>.
   */
  public final TableField<AnomaliesRecord, String> AWSSERVICE =
      createField(DSL.name("awsservice"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.awsaccount</code>.
   */
  public final TableField<AnomaliesRecord, String> AWSACCOUNT =
      createField(DSL.name("awsaccount"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.awsinstancetype</code>.
   */
  public final TableField<AnomaliesRecord, String> AWSINSTANCETYPE =
      createField(DSL.name("awsinstancetype"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.awsusagetype</code>.
   */
  public final TableField<AnomaliesRecord, String> AWSUSAGETYPE =
      createField(DSL.name("awsusagetype"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.anomalyscore</code>.
   */
  public final TableField<AnomaliesRecord, Double> ANOMALYSCORE =
      createField(DSL.name("anomalyscore"), SQLDataType.DOUBLE, this, "");

  /**
   * The column <code>public.anomalies.reportedby</code>.
   */
  public final TableField<AnomaliesRecord, String> REPORTEDBY =
      createField(DSL.name("reportedby"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.feedback</code>.
   */
  public final TableField<AnomaliesRecord, String> FEEDBACK =
      createField(DSL.name("feedback"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.slackdailynotification</code>.
   */
  public final TableField<AnomaliesRecord, Boolean> SLACKDAILYNOTIFICATION =
      createField(DSL.name("slackdailynotification"),
          SQLDataType.BOOLEAN.defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

  /**
   * The column <code>public.anomalies.slackinstantnotification</code>.
   */
  public final TableField<AnomaliesRecord, Boolean> SLACKINSTANTNOTIFICATION =
      createField(DSL.name("slackinstantnotification"),
          SQLDataType.BOOLEAN.defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

  /**
   * The column <code>public.anomalies.slackweeklynotification</code>.
   */
  public final TableField<AnomaliesRecord, Boolean> SLACKWEEKLYNOTIFICATION =
      createField(DSL.name("slackweeklynotification"),
          SQLDataType.BOOLEAN.defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

  /**
   * The column <code>public.anomalies.newentity</code>.
   */
  public final TableField<AnomaliesRecord, Boolean> NEWENTITY = createField(
      DSL.name("newentity"), SQLDataType.BOOLEAN.defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

  /**
   * The column <code>public.anomalies.azuresubscriptionguid</code>.
   */
  public final TableField<AnomaliesRecord, String> AZURESUBSCRIPTIONGUID =
      createField(DSL.name("azuresubscriptionguid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.azureresourcegroup</code>.
   */
  public final TableField<AnomaliesRecord, String> AZURERESOURCEGROUP =
      createField(DSL.name("azureresourcegroup"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.azuremetercategory</code>.
   */
  public final TableField<AnomaliesRecord, String> AZUREMETERCATEGORY =
      createField(DSL.name("azuremetercategory"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.anomalies.notificationsent</code>.
   */
  public final TableField<AnomaliesRecord, Boolean> NOTIFICATIONSENT = createField(DSL.name("notificationsent"),
      SQLDataType.BOOLEAN.defaultValue(DSL.field("false", SQLDataType.BOOLEAN)), this, "");

  private Anomalies(Name alias, Table<AnomaliesRecord> aliased) {
    this(alias, aliased, null);
  }

  private Anomalies(Name alias, Table<AnomaliesRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.anomalies</code> table reference
   */
  public Anomalies(String alias) {
    this(DSL.name(alias), ANOMALIES);
  }

  /**
   * Create an aliased <code>public.anomalies</code> table reference
   */
  public Anomalies(Name alias) {
    this(alias, ANOMALIES);
  }

  /**
   * Create a <code>public.anomalies</code> table reference
   */
  public Anomalies() {
    this(DSL.name("anomalies"), null);
  }

  public <O extends Record> Anomalies(Table<O> child, ForeignKey<O, AnomaliesRecord> key) {
    super(child, key, ANOMALIES);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public List<Index> getIndexes() {
    return Arrays.<Index>asList(
        Indexes.ANOMALIES_ANOMALYTIME_IDX, Indexes.ANOMALIES_PKEY, Indexes.ANOMALY_ACCOUNTID_INDEX);
  }

  @Override
  public Anomalies as(String alias) {
    return new Anomalies(DSL.name(alias), this);
  }

  @Override
  public Anomalies as(Name alias) {
    return new Anomalies(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public Anomalies rename(String name) {
    return new Anomalies(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public Anomalies rename(Name name) {
    return new Anomalies(name, null);
  }
}
