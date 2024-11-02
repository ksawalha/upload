package com.yourdomain.azureblobstorage;

import org.apache.cordova.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

public class AzureBlobStoragePlugin extends CordovaPlugin {

    private static final int CHUNK_SIZE = 4 * 1024 * 1024; // 4 MB

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (action.equals("uploadDeviceFiles")) {
            try {
                JSONArray filesArray = args.getJSONArray(0);
                for (int i = 0; i < filesArray.length(); i++) {
                    JSONObject fileObj = filesArray.getJSONObject(i);
                    String path = fileObj.getString("path");
                    String name = fileObj.getString("name");

                    uploadFileInChunks(path, name, callbackContext);
                }
            } catch (JSONException e) {
                callbackContext.error("Error parsing JSON: " + e.getMessage());
            }
            return true;
        } else {
            return false;
        }
    }

    private void uploadFileInChunks(String filePath, String fileName, CallbackContext callbackContext) {
        try {
            // Configure Azure Storage connection string or SAS token
            String connectionString = "YOUR_AZURE_STORAGE_CONNECTION_STRING"; // Or your SAS token
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference("your-container-name");

            File file = new File(filePath);
            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                int start = chunkIndex * CHUNK_SIZE;
                int size = (int) Math.min(CHUNK_SIZE, fileSize - start);
                byte[] fileContent = readChunk(file, start, size);

                // Generate a unique block ID
                String blockId = UUID.randomUUID().toString();
                CloudBlockBlob blockBlob = container.getBlockBlobReference(fileName);
                blockBlob.uploadBlock(blockId, new ByteArrayInputStream(fileContent), fileContent.length);
            }

            // Commit the blocks to finalize the blob
            blockBlob.commitBlockList(blockIds);

            callbackContext.success("File uploaded in chunks successfully");
        } catch (Exception e) {
            callbackContext.error("Error uploading file: " + e.getMessage());
        }
    }

    private byte[] readChunk(File file, int start, int size) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[size];
            fis.skip(start);
            int bytesRead = fis.read(buffer, 0, size);
            if (bytesRead > 0) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        }
    }
}
