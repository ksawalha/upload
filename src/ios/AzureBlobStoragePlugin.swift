import Foundation
import UIKit
import Cordova
import AzureStorageBlob

@objc(AzureBlobStoragePlugin) class AzureBlobStoragePlugin: CDVPlugin {

    let chunkSize: Int = 4 * 1024 * 1024 // 4 MB

    @objc(uploadDeviceFiles:)
    func uploadDeviceFiles(command: CDVInvokedUrlCommand) {
        let filesArray = command.arguments[0] as? [[String: Any]] ?? []
        let callbackId = command.callbackId
        
        let dispatchGroup = DispatchGroup()
        var uploadResults: [String] = []
        var errorMessages: [String] = []
        
        for fileObj in filesArray {
            guard let filePath = fileObj["path"] as? String,
                  let fileName = fileObj["name"] as? String else { continue }

            dispatchGroup.enter()
            self.uploadFileInChunks(filePath: filePath, fileName: fileName) { success, error in
                if success {
                    uploadResults.append("\(fileName) uploaded successfully.")
                } else if let error = error {
                    errorMessages.append("Error uploading \(fileName): \(error.localizedDescription)")
                }
                dispatchGroup.leave()
            }
        }
        
        dispatchGroup.notify(queue: .main) {
            if !uploadResults.isEmpty {
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK, messageAs: uploadResults), callbackId: callbackId)
            }
            if !errorMessages.isEmpty {
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorMessages), callbackId: callbackId)
            }
        }
    }

    private func uploadFileInChunks(filePath: String, fileName: String, completion: @escaping (Bool, Error?) -> Void) {
        let connectionString = "YOUR_AZURE_STORAGE_CONNECTION_STRING"
        let blobClient = try? AzureStorageBlob.BlobClient(connectionString: connectionString)
        let containerName = "your-container-name"
        let blob = blobClient?.container(name: containerName).blob(name: fileName)

        guard let fileURL = URL(string: filePath) else {
            completion(false, NSError(domain: "Invalid file path", code: -1, userInfo: nil))
            return
        }

        do {
            let fileData = try Data(contentsOf: fileURL)
            let totalChunks = (fileData.count + chunkSize - 1) / chunkSize // Calculate total chunks
            var blockIDs: [String] = []
            var currentChunkIndex = 0
            
            while currentChunkIndex < totalChunks {
                let startIndex = currentChunkIndex * chunkSize
                let endIndex = min(startIndex + chunkSize, fileData.count)
                let chunk = fileData[startIndex..<endIndex]
                
                let blockID = UUID().uuidString.base64Encoded // Generate a unique block ID
                blockIDs.append(blockID)

                // Upload the chunk as a block
                try blob?.upload(blockID: blockID, data: chunk)
                currentChunkIndex += 1
            }

            // Commit the blocks after all chunks are uploaded
            try blob?.commitBlockList(blockIDs: blockIDs)
            completion(true, nil)
        } catch {
            completion(false, error)
        }
    }
}