import Foundation
import UIKit
import Cordova
import AzureStorageBlob

@objc(AzureBlobStoragePlugin) class AzureBlobStoragePlugin: CDVPlugin {

    let chunkSize: Int = 4 * 1024 * 1024 // 4 MB

    @objc(uploadDeviceFiles:)
    func uploadDeviceFiles(command: CDVInvokedUrlCommand) {
        // Expecting command.arguments[0] to be a dictionary with files array, SAS token, and container name
        guard let params = command.arguments[0] as? [String: Any],
              let filesArray = params["files"] as? [[String: Any]],
              let sasToken = params["sasToken"] as? String,
              let containerName = params["containerName"] as? String else {
            self.commandDelegate.send(CDVPluginResult(status: .error, messageAs: "Invalid parameters"), callbackId: command.callbackId)
            return
        }

        let callbackId = command.callbackId
        let dispatchGroup = DispatchGroup()
        var uploadResults: [String] = []
        var errorMessages: [String] = []

        for fileObj in filesArray {
            guard let filePath = fileObj["path"] as? String,
                  let fileName = fileObj["name"] as? String else { continue }

            dispatchGroup.enter()
            self.uploadFileInChunks(filePath: filePath, fileName: fileName, sasToken: sasToken, containerName: containerName) { success, error in
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
                self.commandDelegate.send(CDVPluginResult(status: .ok, messageAs: uploadResults), callbackId: callbackId)
            }
            if !errorMessages.isEmpty {
                self.commandDelegate.send(CDVPluginResult(status: .error, messageAs: errorMessages), callbackId: callbackId)
            }
        }
    }

    private func uploadFileInChunks(filePath: String, fileName: String, sasToken: String, containerName: String, completion: @escaping (Bool, Error?) -> Void) {
        // Create the BlobClient using the SAS token
        guard let blobClient = try? AzureStorageBlob.BlobClient(sasToken: sasToken) else {
            completion(false, NSError(domain: "BlobClient initialization failed", code: -1, userInfo: nil))
            return
        }
        let blob = blobClient.container(name: containerName).blob(name: fileName)

        let fileURL = URL(fileURLWithPath: filePath)
        do {
            let fileData = try Data(contentsOf: fileURL)
            let totalChunks = (fileData.count + chunkSize - 1) / chunkSize // Calculate total chunks
            var blockIDs: [String] = []
            var currentChunkIndex = 0

            while currentChunkIndex < totalChunks {
                let startIndex = currentChunkIndex * chunkSize
                let endIndex = min(startIndex + chunkSize, fileData.count)
                let chunk = fileData.subdata(in: startIndex..<endIndex)

                let blockID = UUID().uuidString.data(using: .utf8)?.base64EncodedString() ?? "" // Generate a unique block ID
                blockIDs.append(blockID)

                // Upload the chunk as a block
                try blob.upload(blockID: blockID, data: chunk)
                currentChunkIndex += 1
            }

            // Commit the blocks after all chunks are uploaded
            try blob.commitBlockList(blockIDs: blockIDs)
            completion(true, nil)
        } catch {
            completion(false, error)
        }
    }
}
