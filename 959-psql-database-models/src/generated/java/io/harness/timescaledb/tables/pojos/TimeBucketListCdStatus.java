/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.pojos;

import java.io.Serializable;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class TimeBucketListCdStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  private String status;
  private Long startts;

  public TimeBucketListCdStatus() {}

  public TimeBucketListCdStatus(TimeBucketListCdStatus value) {
    this.status = value.status;
    this.startts = value.startts;
  }

  public TimeBucketListCdStatus(String status, Long startts) {
    this.status = status;
    this.startts = startts;
  }

  /**
   * Getter for <code>public.time_bucket_list_cd_status.status</code>.
   */
  public String getStatus() {
    return this.status;
  }

  /**
   * Setter for <code>public.time_bucket_list_cd_status.status</code>.
   */
  public TimeBucketListCdStatus setStatus(String status) {
    this.status = status;
    return this;
  }

  /**
   * Getter for <code>public.time_bucket_list_cd_status.startts</code>.
   */
  public Long getStartts() {
    return this.startts;
  }

  /**
   * Setter for <code>public.time_bucket_list_cd_status.startts</code>.
   */
  public TimeBucketListCdStatus setStartts(Long startts) {
    this.startts = startts;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final TimeBucketListCdStatus other = (TimeBucketListCdStatus) obj;
    if (status == null) {
      if (other.status != null)
        return false;
    } else if (!status.equals(other.status))
      return false;
    if (startts == null) {
      if (other.startts != null)
        return false;
    } else if (!startts.equals(other.startts))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.status == null) ? 0 : this.status.hashCode());
    result = prime * result + ((this.startts == null) ? 0 : this.startts.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TimeBucketListCdStatus (");

    sb.append(status);
    sb.append(", ").append(startts);

    sb.append(")");
    return sb.toString();
  }
}
