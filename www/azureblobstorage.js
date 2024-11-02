var AzureBlobStorage = {
    /**
     * Upload files from device storage to Azure Blob Storage.
     * @param {Array} files - Array of file objects containing path and name.
     * @param {Function} successCallback - Callback function invoked on successful upload.
     * @param {Function} errorCallback - Callback function invoked on upload error.
     */
    uploadDeviceFiles: function(files, successCallback, errorCallback) {
        // Check if files are provided
        if (!Array.isArray(files) || files.length === 0) {
            errorCallback("No files provided for upload.");
            return;
        }

        // Construct an array of file objects
        var fileArray = files.map(function(file) {
            return {
                path: file.path, // The local file path
                name: file.name  // The file name
            };
        });

        // Call the native plugin
        cordova.exec(successCallback, errorCallback, 'AzureBlobStoragePlugin', 'uploadDeviceFiles', [fileArray]);
    }
};

// Export the AzureBlobStorage object for external use
if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
    module.exports = AzureBlobStorage;
} else {
    window.AzureBlobStorage = AzureBlobStorage;
}
