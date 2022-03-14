/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.helpers.ext.azure;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.microsoft.azure.keyvault.extensions.KeyVaultKeyResolver;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class AzureBlobADALAuthenticator {
  public static CloudBlockBlob getBlobClient(String connectionString, String continerName, String blobName)
      throws URISyntaxException, InvalidKeyException, StorageException {
    CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
    CloudBlobContainer container = blobClient.getContainerReference(continerName);
    return container.getBlockBlobReference(blobName);
  }

  public static KeyVaultKeyResolver getKeyResolverClient(String clientId, String clientKey) {
    return new KeyVaultKeyResolver(KeyVaultADALAuthenticator.getClient(clientId, clientKey));
  }
}
