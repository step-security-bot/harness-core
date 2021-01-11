package io.harness.serializer.kryo;

import io.harness.aws.AwsAccessKeyCredential;
import io.harness.aws.AwsConfig;
import io.harness.aws.CrossAccountAccess;
import io.harness.azure.AzureEnvironmentType;
import io.harness.azure.model.AzureAppServiceConfiguration;
import io.harness.azure.model.AzureAppServiceConnectionStringType;
import io.harness.azure.model.AzureVMData;
import io.harness.azure.model.SubscriptionData;
import io.harness.azure.model.VirtualMachineScaleSetData;
import io.harness.container.ContainerInfo;
import io.harness.deployment.InstanceDetails;
import io.harness.ecs.EcsContainerDetails;
import io.harness.git.UsernamePasswordAuthRequest;
import io.harness.git.model.AuthInfo;
import io.harness.git.model.ChangeType;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.CommitResult;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.git.model.GitFileChange;
import io.harness.git.model.GitRepositoryType;
import io.harness.git.model.PushResultGit;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCreateMetaResponse;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.jira.JiraField;
import io.harness.jira.JiraIssueType;
import io.harness.jira.JiraProjectData;
import io.harness.jira.JiraStatus;
import io.harness.jira.JiraStatusCategory;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.ImageDetails;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.k8s.model.OidcGrantType;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pcf.model.ManifestType;
import io.harness.provision.TfVarScriptRepositorySource;
import io.harness.provision.TfVarSource;
import io.harness.provision.TfVarSource.TfVarSourceType;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoRegistrar;
import io.harness.shell.*;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;

import software.wings.settings.SettingVariableTypes;

import com.amazonaws.SdkClientException;
import com.amazonaws.internal.SdkInternalList;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.CapacityReservationSpecificationResponse;
import com.amazonaws.services.ec2.model.CapacityReservationTargetResponse;
import com.amazonaws.services.ec2.model.CpuOptions;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.HibernationOptions;
import com.amazonaws.services.ec2.model.IamInstanceProfile;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceIpv6Address;
import com.amazonaws.services.ec2.model.InstanceMetadataOptionsResponse;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAssociation;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAttachment;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.LicenseConfiguration;
import com.amazonaws.services.ec2.model.Monitoring;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.ProductCode;
import com.amazonaws.services.ec2.model.StateReason;
import com.amazonaws.services.ec2.model.Tag;
import com.esotericsoftware.kryo.Kryo;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.monitoring.v3.model.BucketOptions;
import com.google.api.services.monitoring.v3.model.Distribution;
import com.google.api.services.monitoring.v3.model.Exponential;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.api.services.monitoring.v3.model.Metric;
import com.google.api.services.monitoring.v3.model.MonitoredResource;
import com.google.api.services.monitoring.v3.model.Point;
import com.google.api.services.monitoring.v3.model.TimeInterval;
import com.google.api.services.monitoring.v3.model.TimeSeries;
import com.google.api.services.monitoring.v3.model.TypedValue;
import com.hazelcast.spi.exception.TargetNotMemberException;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.sumologic.client.SumoServerException;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.json.JSONException;

public class ApiServiceBeansKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Instance.class, 1001);
    kryo.register(SdkInternalList.class, 1002);
    kryo.register(InstanceBlockDeviceMapping.class, 1003);
    kryo.register(EbsInstanceBlockDevice.class, 1004);
    kryo.register(IamInstanceProfile.class, 1005);
    kryo.register(Monitoring.class, 1006);
    kryo.register(InstanceNetworkInterface.class, 1007);
    kryo.register(InstanceNetworkInterfaceAssociation.class, 1008);
    kryo.register(InstanceNetworkInterfaceAttachment.class, 1009);
    kryo.register(GroupIdentifier.class, 1010);
    kryo.register(InstancePrivateIpAddress.class, 1011);
    kryo.register(Placement.class, 1012);
    kryo.register(InstanceState.class, 1013);
    kryo.register(Tag.class, 1014);
    kryo.register(com.amazonaws.AbortedException.class, 1015);
    kryo.register(StateReason.class, 1016);
    kryo.register(SdkClientException.class, 1017);
    kryo.register(InstanceIpv6Address.class, 1018);
    kryo.register(ProductCode.class, 1019);
    kryo.register(Filter.class, 1020);
    kryo.register(Regions.class, 1021);
    kryo.register(CpuOptions.class, 1022);
    kryo.register(CapacityReservationSpecificationResponse.class, 1023);
    kryo.register(CapacityReservationTargetResponse.class, 1024);
    kryo.register(KubernetesClientException.class, 2000);
    kryo.register(JSONException.class, 2001);
    kryo.register(TargetNotMemberException.class, 2002);
    kryo.register(SumoServerException.class, 2003);
    kryo.register(ListTimeSeriesResponse.class, 2004);
    kryo.register(TimeSeries.class, 2005);
    kryo.register(Point.class, 2006);
    kryo.register(Metric.class, 2007);
    kryo.register(MonitoredResource.class, 2008);
    kryo.register(TimeInterval.class, 2009);
    kryo.register(TypedValue.class, 2010);
    kryo.register(ArrayMap.class, 2011);
    kryo.register(Distribution.class, 2012);
    kryo.register(BucketOptions.class, 2013);
    kryo.register(Exponential.class, 2014);
    kryo.register(BasicDBList.class, 2015);
    kryo.register(BasicDBObject.class, 2016);
    kryo.register(CommandExecutionStatus.class, 5037);
    kryo.register(EncryptionConfig.class, 5305);
    kryo.register(EncryptionType.class, 5123);
    kryo.register(EncryptedDataDetail.class, 5125);
    kryo.register(ImageDetails.class, 5151);
    kryo.register(AccessType.class, 5072);
    kryo.register(ContainerInfo.Status.class, 5076);
    kryo.register(ContainerInfo.class, 5075);
    kryo.register(ShellExecutionData.class, 5528);
    kryo.register(KerberosConfig.class, 5549);
    kryo.register(AuthenticationScheme.class, 5550);
    kryo.register(K8sPod.class, 7145);
    kryo.register(K8sContainer.class, 7146);
    kryo.register(AuditGlobalContextData.class, 7172);
    kryo.register(PurgeGlobalContextData.class, 7173);
    kryo.register(EcsContainerDetails.class, 7179);
    kryo.register(OidcGrantType.class, 7318);

    kryo.register(CEK8sDelegatePrerequisite.class, 7490);
    kryo.register(CEK8sDelegatePrerequisite.MetricsServerCheck.class, 7491);
    kryo.register(CEK8sDelegatePrerequisite.Rule.class, 7492);

    kryo.register(ElastiGroup.class, 1025);
    kryo.register(ElastiGroupCapacity.class, 1026);
    kryo.register(EncryptedRecordData.class, 1401);
    kryo.register(ManifestType.class, 1402);
    kryo.register(HibernationOptions.class, 1403);
    kryo.register(InstanceMetadataOptionsResponse.class, 1404);
    kryo.register(LicenseConfiguration.class, 1405);
    kryo.register(InstanceDetails.class, 1406);
    kryo.register(InstanceDetails.PCF.class, 1407);
    kryo.register(InstanceDetails.AWS.class, 1408);
    kryo.register(InstanceDetails.InstanceType.class, 1409);
    kryo.register(InstanceDetails.PHYSICAL_HOST.class, 1411);
    kryo.register(InstanceDetails.K8s.class, 1412);
    kryo.register(EncryptedDataParams.class, 1413);
    kryo.register(SubscriptionData.class, 1414);
    kryo.register(VirtualMachineScaleSetData.class, 1415);
    kryo.register(InstanceDetails.AZURE_VMSS.class, 1416);
    kryo.register(AzureVMData.class, 1417);

    kryo.register(SettingVariableTypes.class, 5131);
    kryo.register(ScriptType.class, 5253);

    kryo.register(GitFile.class, 5574);
    kryo.register(EncryptableSettingWithEncryptionDetails.class, 7258);
    kryo.register(HelmVersion.class, 7303);
    kryo.register(KubernetesClusterAuthType.class, 7317);
    kryo.register(IstioDestinationWeight.class, 7183);

    kryo.register(ChangeType.class, 5212);
    kryo.register(GitFileChange.class, 1418);
    kryo.register(CommitAndPushRequest.class, 1419);
    kryo.register(UsernamePasswordAuthRequest.class, 1420);
    kryo.register(AuthInfo.class, 1421);
    kryo.register(AuthInfo.AuthType.class, 1422);
    kryo.register(CommitAndPushResult.class, 1423);
    kryo.register(CommitResult.class, 1424);
    kryo.register(PushResultGit.class, 1425);
    kryo.register(PushResultGit.RefUpdate.class, 1426);
    kryo.register(FetchFilesResult.class, 1427);
    kryo.register(CommandExecutionData.class, 5035);
    kryo.register(GitRepositoryType.class, 5270);
    kryo.register(JiraAction.class, 5580);
    kryo.register(JiraCustomFieldValue.class, 7177);
    kryo.register(JiraProjectData.class, 7198);
    kryo.register(JiraIssueType.class, 7199);
    kryo.register(JiraField.class, 7200);
    kryo.register(JiraCreateMetaResponse.class, 7201);

    kryo.register(AwsConfig.class, 1428);
    kryo.register(CrossAccountAccess.class, 1429);
    kryo.register(AwsAccessKeyCredential.class, 1430);
    kryo.register(AzureEnvironmentType.class, 1436);

    kryo.register(JiraStatus.class, 1431);
    kryo.register(JiraStatusCategory.class, 1432);

    kryo.register(TfVarScriptRepositorySource.class, 1433);
    kryo.register(TfVarSource.class, 1434);
    kryo.register(TfVarSourceType.class, 1435);
    kryo.register(InstanceDetails.AZURE_WEBAPP.class, 1437);
    kryo.register(ExecuteCommandResponse.class, 1438);
    kryo.register(AzureAppServiceConnectionStringType.class, 1439);
    kryo.register(AzureAppServiceConfiguration.class, 1440);
  }
}
