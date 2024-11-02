var exec = require('cordova/exec');

var AzureBlobStoragePlugin = {
    uploadDeviceFiles: function(files, sasToken, containerName, successCallback, errorCallback) {
        var params = {
            files: files,
            sasToken: sasToken,
            containerName: containerName
        };
        
        exec(successCallback, errorCallback, 'AzureBlobStoragePlugin', 'uploadDeviceFiles', [params]);
    }
};

module.exports = AzureBlobStoragePlugin;
