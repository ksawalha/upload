package com.karamsawalha.azureblobstorage;

import org.apache.cordova.*;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AzureBlobStoragePlugin extends CordovaPlugin {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private CallbackContext currentCallbackContext;
    private JSONArray currentArgs;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (action.equals("uploadDeviceFiles")) {
            if (ContextCompat.checkSelfPermission(cordova.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                currentCallbackContext = callbackContext;
                currentArgs = args;

                // Request permission
                ActivityCompat.requestPermissions(cordova.getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                // Permission already granted, proceed with file upload
                handleFileUpload(args, callbackContext);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the file upload
                handleFileUpload(currentArgs, currentCallbackContext);
            } else {
                // Permission denied
                currentCallbackContext.error("Storage permission denied");
            }
        }
    }

    private void handleFileUpload(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONArray filesArray = args.getJSONArray(0);
            for (int i = 0; i < filesArray.length(); i++) {
                JSONObject fileObj = filesArray.getJSONObject(i);
                String filePath = fileObj.getString("path");
                String fileName = fileObj.getString("name");

                byte[] fileContent = readFileFromDevice(filePath);
                uploadToAzure(fileContent, fileName, callbackContext);
            }
        } catch (JSONException e) {
            callbackContext.error("Error parsing JSON: " + e.getMessage());
        } catch (IOException e) {
            callbackContext.error("Error reading file: " + e.getMessage());
        }
    }

    private byte[] readFileFromDevice(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileContent = new byte[(int) file.length()];
            fis.read(fileContent);
            return fileContent;
        }
    }

    private void uploadToAzure(byte[] fileContent, String fileName, CallbackContext callbackContext) {
        try {
            // Configure Azure Storage connection string or SAS token
            String connectionString = "YOUR_AZURE_STORAGE_CONNECTION_STRING"; // Or your SAS token
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference("your-container-name");

            CloudBlockBlob blockBlob = container.getBlockBlobReference(fileName);
            blockBlob.uploadFromByteArray(fileContent, 0, fileContent.length);

            callbackContext.success("File uploaded successfully");
        } catch (Exception e) {
            callbackContext.error("Error uploading file to Azure: " + e.getMessage());
        }
    }
}
