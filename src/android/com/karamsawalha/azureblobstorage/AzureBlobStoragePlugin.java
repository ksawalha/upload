package com.yourdomain.azureblobstorage;

import org.apache.cordova.*;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
            // Create BlobServiceClient
            String connectionString = "YOUR_AZURE_STORAGE_CONNECTION_STRING"; // Update with your connection string
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("your-container-name");

            // Ensure the container exists
            containerClient.createIfNotExists();

            // Open the file for reading
            File file = new File(filePath);
            long fileSize = file.length();
            int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
            List<String> blockIds = new ArrayList<>();

            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                int start = chunkIndex * CHUNK_SIZE;
                int size = (int) Math.min(CHUNK_SIZE, fileSize - start);
                byte[] fileContent = readChunk(file, start, size);

                // Generate a unique block ID
                String blockId = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
                blockIds.add(blockId);

                // Upload the block
                BlobClient blobClient = containerClient.getBlobClient(fileName);
                blobClient.getBlockBlobClient().stageBlock(blockId, new ByteArrayInputStream(fileContent), fileContent.length);
            }

            // Commit the blocks to finalize the blob
            blobClient.getBlockBlobClient().commitBlockList(blockIds);

            callbackContext.success("File uploaded in chunks successfully");
        } catch (Exception e) {
            callbackContext.error("Error uploading file: " + e.getMessage());
        }
    }

    private byte[] readChunk(File file, int start, int size) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[size];
            fis.skip(start);
            int bytesRead = fis.read(buffer, 0, size);
            if (bytesRead > 0) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                return chunk;
            }
            return new byte[0]; // Return empty byte array if nothing read
        }
    }
}
