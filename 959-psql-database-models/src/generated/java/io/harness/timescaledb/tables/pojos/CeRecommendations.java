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
import java.time.OffsetDateTime;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class CeRecommendations implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private String namespace;
  private Double monthlycost;
  private Double monthlysaving;
  private String clustername;
  private String resourcetype;
  private String accountid;
  private Boolean isvalid;
  private OffsetDateTime lastprocessedat;
  private OffsetDateTime updatedat;
  private String jiraconnectorref;
  private String jiraissuekey;
  private String jirastatus;
  private String recommendationstate;
  private String governanceruleid;
  private String cloudprovider;
  private String servicenowconnectorref;
  private String servicenowissuekey;
  private String servicenowstatus;
  private String servicenowtickettype;

  public CeRecommendations() {}

  public CeRecommendations(CeRecommendations value) {
    this.id = value.id;
    this.name = value.name;
    this.namespace = value.namespace;
    this.monthlycost = value.monthlycost;
    this.monthlysaving = value.monthlysaving;
    this.clustername = value.clustername;
    this.resourcetype = value.resourcetype;
    this.accountid = value.accountid;
    this.isvalid = value.isvalid;
    this.lastprocessedat = value.lastprocessedat;
    this.updatedat = value.updatedat;
    this.jiraconnectorref = value.jiraconnectorref;
    this.jiraissuekey = value.jiraissuekey;
    this.jirastatus = value.jirastatus;
    this.recommendationstate = value.recommendationstate;
    this.governanceruleid = value.governanceruleid;
    this.cloudprovider = value.cloudprovider;
    this.servicenowconnectorref = value.servicenowconnectorref;
    this.servicenowissuekey = value.servicenowissuekey;
    this.servicenowstatus = value.servicenowstatus;
    this.servicenowtickettype = value.servicenowtickettype;
  }

  public CeRecommendations(String id, String name, String namespace, Double monthlycost, Double monthlysaving,
      String clustername, String resourcetype, String accountid, Boolean isvalid, OffsetDateTime lastprocessedat,
      OffsetDateTime updatedat, String jiraconnectorref, String jiraissuekey, String jirastatus,
      String recommendationstate, String governanceruleid, String cloudprovider, String servicenowconnectorref,
      String servicenowissuekey, String servicenowstatus, String servicenowtickettype) {
    this.id = id;
    this.name = name;
    this.namespace = namespace;
    this.monthlycost = monthlycost;
    this.monthlysaving = monthlysaving;
    this.clustername = clustername;
    this.resourcetype = resourcetype;
    this.accountid = accountid;
    this.isvalid = isvalid;
    this.lastprocessedat = lastprocessedat;
    this.updatedat = updatedat;
    this.jiraconnectorref = jiraconnectorref;
    this.jiraissuekey = jiraissuekey;
    this.jirastatus = jirastatus;
    this.recommendationstate = recommendationstate;
    this.governanceruleid = governanceruleid;
    this.cloudprovider = cloudprovider;
    this.servicenowconnectorref = servicenowconnectorref;
    this.servicenowissuekey = servicenowissuekey;
    this.servicenowstatus = servicenowstatus;
    this.servicenowtickettype = servicenowtickettype;
  }

  /**
   * Getter for <code>public.ce_recommendations.id</code>.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Setter for <code>public.ce_recommendations.id</code>.
   */
  public CeRecommendations setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.name</code>.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Setter for <code>public.ce_recommendations.name</code>.
   */
  public CeRecommendations setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.namespace</code>.
   */
  public String getNamespace() {
    return this.namespace;
  }

  /**
   * Setter for <code>public.ce_recommendations.namespace</code>.
   */
  public CeRecommendations setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.monthlycost</code>.
   */
  public Double getMonthlycost() {
    return this.monthlycost;
  }

  /**
   * Setter for <code>public.ce_recommendations.monthlycost</code>.
   */
  public CeRecommendations setMonthlycost(Double monthlycost) {
    this.monthlycost = monthlycost;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.monthlysaving</code>.
   */
  public Double getMonthlysaving() {
    return this.monthlysaving;
  }

  /**
   * Setter for <code>public.ce_recommendations.monthlysaving</code>.
   */
  public CeRecommendations setMonthlysaving(Double monthlysaving) {
    this.monthlysaving = monthlysaving;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.clustername</code>.
   */
  public String getClustername() {
    return this.clustername;
  }

  /**
   * Setter for <code>public.ce_recommendations.clustername</code>.
   */
  public CeRecommendations setClustername(String clustername) {
    this.clustername = clustername;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.resourcetype</code>.
   */
  public String getResourcetype() {
    return this.resourcetype;
  }

  /**
   * Setter for <code>public.ce_recommendations.resourcetype</code>.
   */
  public CeRecommendations setResourcetype(String resourcetype) {
    this.resourcetype = resourcetype;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.accountid</code>.
   */
  public String getAccountid() {
    return this.accountid;
  }

  /**
   * Setter for <code>public.ce_recommendations.accountid</code>.
   */
  public CeRecommendations setAccountid(String accountid) {
    this.accountid = accountid;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.isvalid</code>.
   */
  public Boolean getIsvalid() {
    return this.isvalid;
  }

  /**
   * Setter for <code>public.ce_recommendations.isvalid</code>.
   */
  public CeRecommendations setIsvalid(Boolean isvalid) {
    this.isvalid = isvalid;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.lastprocessedat</code>.
   */
  public OffsetDateTime getLastprocessedat() {
    return this.lastprocessedat;
  }

  /**
   * Setter for <code>public.ce_recommendations.lastprocessedat</code>.
   */
  public CeRecommendations setLastprocessedat(OffsetDateTime lastprocessedat) {
    this.lastprocessedat = lastprocessedat;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.updatedat</code>.
   */
  public OffsetDateTime getUpdatedat() {
    return this.updatedat;
  }

  /**
   * Setter for <code>public.ce_recommendations.updatedat</code>.
   */
  public CeRecommendations setUpdatedat(OffsetDateTime updatedat) {
    this.updatedat = updatedat;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.jiraconnectorref</code>.
   */
  public String getJiraconnectorref() {
    return this.jiraconnectorref;
  }

  /**
   * Setter for <code>public.ce_recommendations.jiraconnectorref</code>.
   */
  public CeRecommendations setJiraconnectorref(String jiraconnectorref) {
    this.jiraconnectorref = jiraconnectorref;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.jiraissuekey</code>.
   */
  public String getJiraissuekey() {
    return this.jiraissuekey;
  }

  /**
   * Setter for <code>public.ce_recommendations.jiraissuekey</code>.
   */
  public CeRecommendations setJiraissuekey(String jiraissuekey) {
    this.jiraissuekey = jiraissuekey;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.jirastatus</code>.
   */
  public String getJirastatus() {
    return this.jirastatus;
  }

  /**
   * Setter for <code>public.ce_recommendations.jirastatus</code>.
   */
  public CeRecommendations setJirastatus(String jirastatus) {
    this.jirastatus = jirastatus;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.recommendationstate</code>.
   */
  public String getRecommendationstate() {
    return this.recommendationstate;
  }

  /**
   * Setter for <code>public.ce_recommendations.recommendationstate</code>.
   */
  public CeRecommendations setRecommendationstate(String recommendationstate) {
    this.recommendationstate = recommendationstate;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.governanceruleid</code>.
   */
  public String getGovernanceruleid() {
    return this.governanceruleid;
  }

  /**
   * Setter for <code>public.ce_recommendations.governanceruleid</code>.
   */
  public CeRecommendations setGovernanceruleid(String governanceruleid) {
    this.governanceruleid = governanceruleid;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.cloudprovider</code>.
   */
  public String getCloudprovider() {
    return this.cloudprovider;
  }

  /**
   * Setter for <code>public.ce_recommendations.cloudprovider</code>.
   */
  public CeRecommendations setCloudprovider(String cloudprovider) {
    this.cloudprovider = cloudprovider;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.servicenowconnectorref</code>.
   */
  public String getServicenowconnectorref() {
    return this.servicenowconnectorref;
  }

  /**
   * Setter for <code>public.ce_recommendations.servicenowconnectorref</code>.
   */
  public CeRecommendations setServicenowconnectorref(String servicenowconnectorref) {
    this.servicenowconnectorref = servicenowconnectorref;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.servicenowissuekey</code>.
   */
  public String getServicenowissuekey() {
    return this.servicenowissuekey;
  }

  /**
   * Setter for <code>public.ce_recommendations.servicenowissuekey</code>.
   */
  public CeRecommendations setServicenowissuekey(String servicenowissuekey) {
    this.servicenowissuekey = servicenowissuekey;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.servicenowstatus</code>.
   */
  public String getServicenowstatus() {
    return this.servicenowstatus;
  }

  /**
   * Setter for <code>public.ce_recommendations.servicenowstatus</code>.
   */
  public CeRecommendations setServicenowstatus(String servicenowstatus) {
    this.servicenowstatus = servicenowstatus;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.servicenowtickettype</code>.
   */
  public String getServicenowtickettype() {
    return this.servicenowtickettype;
  }

  /**
   * Setter for <code>public.ce_recommendations.servicenowtickettype</code>.
   */
  public CeRecommendations setServicenowtickettype(String servicenowtickettype) {
    this.servicenowtickettype = servicenowtickettype;
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
    final CeRecommendations other = (CeRecommendations) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (namespace == null) {
      if (other.namespace != null)
        return false;
    } else if (!namespace.equals(other.namespace))
      return false;
    if (monthlycost == null) {
      if (other.monthlycost != null)
        return false;
    } else if (!monthlycost.equals(other.monthlycost))
      return false;
    if (monthlysaving == null) {
      if (other.monthlysaving != null)
        return false;
    } else if (!monthlysaving.equals(other.monthlysaving))
      return false;
    if (clustername == null) {
      if (other.clustername != null)
        return false;
    } else if (!clustername.equals(other.clustername))
      return false;
    if (resourcetype == null) {
      if (other.resourcetype != null)
        return false;
    } else if (!resourcetype.equals(other.resourcetype))
      return false;
    if (accountid == null) {
      if (other.accountid != null)
        return false;
    } else if (!accountid.equals(other.accountid))
      return false;
    if (isvalid == null) {
      if (other.isvalid != null)
        return false;
    } else if (!isvalid.equals(other.isvalid))
      return false;
    if (lastprocessedat == null) {
      if (other.lastprocessedat != null)
        return false;
    } else if (!lastprocessedat.equals(other.lastprocessedat))
      return false;
    if (updatedat == null) {
      if (other.updatedat != null)
        return false;
    } else if (!updatedat.equals(other.updatedat))
      return false;
    if (jiraconnectorref == null) {
      if (other.jiraconnectorref != null)
        return false;
    } else if (!jiraconnectorref.equals(other.jiraconnectorref))
      return false;
    if (jiraissuekey == null) {
      if (other.jiraissuekey != null)
        return false;
    } else if (!jiraissuekey.equals(other.jiraissuekey))
      return false;
    if (jirastatus == null) {
      if (other.jirastatus != null)
        return false;
    } else if (!jirastatus.equals(other.jirastatus))
      return false;
    if (recommendationstate == null) {
      if (other.recommendationstate != null)
        return false;
    } else if (!recommendationstate.equals(other.recommendationstate))
      return false;
    if (governanceruleid == null) {
      if (other.governanceruleid != null)
        return false;
    } else if (!governanceruleid.equals(other.governanceruleid))
      return false;
    if (cloudprovider == null) {
      if (other.cloudprovider != null)
        return false;
    } else if (!cloudprovider.equals(other.cloudprovider))
      return false;
    if (servicenowconnectorref == null) {
      if (other.servicenowconnectorref != null)
        return false;
    } else if (!servicenowconnectorref.equals(other.servicenowconnectorref))
      return false;
    if (servicenowissuekey == null) {
      if (other.servicenowissuekey != null)
        return false;
    } else if (!servicenowissuekey.equals(other.servicenowissuekey))
      return false;
    if (servicenowstatus == null) {
      if (other.servicenowstatus != null)
        return false;
    } else if (!servicenowstatus.equals(other.servicenowstatus))
      return false;
    if (servicenowtickettype == null) {
      if (other.servicenowtickettype != null)
        return false;
    } else if (!servicenowtickettype.equals(other.servicenowtickettype))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
    result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
    result = prime * result + ((this.namespace == null) ? 0 : this.namespace.hashCode());
    result = prime * result + ((this.monthlycost == null) ? 0 : this.monthlycost.hashCode());
    result = prime * result + ((this.monthlysaving == null) ? 0 : this.monthlysaving.hashCode());
    result = prime * result + ((this.clustername == null) ? 0 : this.clustername.hashCode());
    result = prime * result + ((this.resourcetype == null) ? 0 : this.resourcetype.hashCode());
    result = prime * result + ((this.accountid == null) ? 0 : this.accountid.hashCode());
    result = prime * result + ((this.isvalid == null) ? 0 : this.isvalid.hashCode());
    result = prime * result + ((this.lastprocessedat == null) ? 0 : this.lastprocessedat.hashCode());
    result = prime * result + ((this.updatedat == null) ? 0 : this.updatedat.hashCode());
    result = prime * result + ((this.jiraconnectorref == null) ? 0 : this.jiraconnectorref.hashCode());
    result = prime * result + ((this.jiraissuekey == null) ? 0 : this.jiraissuekey.hashCode());
    result = prime * result + ((this.jirastatus == null) ? 0 : this.jirastatus.hashCode());
    result = prime * result + ((this.recommendationstate == null) ? 0 : this.recommendationstate.hashCode());
    result = prime * result + ((this.governanceruleid == null) ? 0 : this.governanceruleid.hashCode());
    result = prime * result + ((this.cloudprovider == null) ? 0 : this.cloudprovider.hashCode());
    result = prime * result + ((this.servicenowconnectorref == null) ? 0 : this.servicenowconnectorref.hashCode());
    result = prime * result + ((this.servicenowissuekey == null) ? 0 : this.servicenowissuekey.hashCode());
    result = prime * result + ((this.servicenowstatus == null) ? 0 : this.servicenowstatus.hashCode());
    result = prime * result + ((this.servicenowtickettype == null) ? 0 : this.servicenowtickettype.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CeRecommendations (");

    sb.append(id);
    sb.append(", ").append(name);
    sb.append(", ").append(namespace);
    sb.append(", ").append(monthlycost);
    sb.append(", ").append(monthlysaving);
    sb.append(", ").append(clustername);
    sb.append(", ").append(resourcetype);
    sb.append(", ").append(accountid);
    sb.append(", ").append(isvalid);
    sb.append(", ").append(lastprocessedat);
    sb.append(", ").append(updatedat);
    sb.append(", ").append(jiraconnectorref);
    sb.append(", ").append(jiraissuekey);
    sb.append(", ").append(jirastatus);
    sb.append(", ").append(recommendationstate);
    sb.append(", ").append(governanceruleid);
    sb.append(", ").append(cloudprovider);
    sb.append(", ").append(servicenowconnectorref);
    sb.append(", ").append(servicenowissuekey);
    sb.append(", ").append(servicenowstatus);
    sb.append(", ").append(servicenowtickettype);

    sb.append(")");
    return sb.toString();
  }
}
