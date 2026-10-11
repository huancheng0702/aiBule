import Foundation
import UIKit
import UniformTypeIdentifiers

@objc public class CoreFilePickerNative: NSObject {

  static var activeDelegates: [PickerDelegate] = []

  // MARK: - Public API

  public static func pickFile(
    mimeTypes: [String],
    callback: @escaping (String?, String?, Int64, String?) -> Void
  ) {
    DispatchQueue.main.async {
      let utTypes = resolveUTTypes(mimeTypes)
      let picker = UIDocumentPickerViewController(forOpeningContentTypes: utTypes, asCopy: true)
      
      let delegate = PickerDelegate(isMultiPicker: false, pickFileCallback: callback, multiFileCallback: nil)
      activeDelegates.append(delegate)
      picker.delegate = delegate
      picker.allowsMultipleSelection = false

      present(picker, delegate: delegate)
    }
  }

  public static func pickFiles(
    mimeTypes: [String],
    callback: @escaping (String?) -> Void
  ) {
    DispatchQueue.main.async {
      let utTypes = resolveUTTypes(mimeTypes)
      let picker = UIDocumentPickerViewController(forOpeningContentTypes: utTypes, asCopy: true)
      
      let delegate = PickerDelegate(isMultiPicker: true, pickFileCallback: nil, multiFileCallback: callback)
      activeDelegates.append(delegate)
      picker.delegate = delegate
      picker.allowsMultipleSelection = true

      present(picker, delegate: delegate)
    }
  }



  private static func resolveUTTypes(_ mimeTypes: [String]) -> [UTType] {
    for mime in mimeTypes {
      if mime == "*/*" { return [.data] }
      if let utType = UTType(mimeType: mime) { return [utType] }
    }
    return [.data]
  }

  private static func present(_ picker: UIDocumentPickerViewController, delegate: PickerDelegate) {
    var keyWindow: UIWindow? = nil
    if #available(iOS 13.0, *) {
      keyWindow = UIApplication.shared.connectedScenes
        .filter({$0.activationState == .foregroundActive})
        .compactMap({$0 as? UIWindowScene})
        .first?.windows
        .filter({$0.isKeyWindow}).first
    }
    if keyWindow == nil {
      keyWindow = UIApplication.shared.windows.first(where: { $0.isKeyWindow })
    }
    
    guard let rootVC = keyWindow?.rootViewController else {
      delegate.documentPickerWasCancelled(picker)
      return
    }
    
    var topVC = rootVC
    while let presentedVC = topVC.presentedViewController {
      topVC = presentedVC
    }
    
    topVC.present(picker, animated: true)
  }
}

class PickerDelegate: NSObject, UIDocumentPickerDelegate {
  let isMultiPicker: Bool
  let pickFileCallback: ((String?, String?, Int64, String?) -> Void)?
  let multiFileCallback: ((String?) -> Void)?

  init(isMultiPicker: Bool,
       pickFileCallback: ((String?, String?, Int64, String?) -> Void)?,
       multiFileCallback: ((String?) -> Void)?) {
    self.isMultiPicker = isMultiPicker
    self.pickFileCallback = pickFileCallback
    self.multiFileCallback = multiFileCallback
    super.init()
  }

  func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
    if isMultiPicker {
      handleMultiPicked(urls)
    } else {
      handleSinglePicked(urls)
    }
    removeSelf()
  }

  func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
    if isMultiPicker {
      multiFileCallback?(nil)
    } else {
      pickFileCallback?(nil, nil, 0, nil)
    }
    removeSelf()
  }

  private func removeSelf() {
    CoreFilePickerNative.activeDelegates.removeAll(where: { $0 === self })
  }

  private func handleSinglePicked(_ urls: [URL]) {
    guard let url = urls.first else {
      pickFileCallback?(nil, nil, 0, nil)
      return
    }

    url.startAccessingSecurityScopedResource()
    defer { url.stopAccessingSecurityScopedResource() }

    let fileName = url.lastPathComponent
    var fileSize: Int64 = 0
    if let attrs = try? FileManager.default.attributesOfItem(atPath: url.path) {
      fileSize = (attrs[.size] as? Int64) ?? 0
    }
    let mimeType = UTType(filenameExtension: url.pathExtension)?.preferredMIMEType ?? ""

    let tempDir = NSTemporaryDirectory() + "upload_picker/"
    try? FileManager.default.createDirectory(atPath: tempDir, withIntermediateDirectories: true)
    let destURL = URL(fileURLWithPath: tempDir + fileName)

    do {
      if FileManager.default.fileExists(atPath: destURL.path) {
        try FileManager.default.removeItem(at: destURL)
      }
      try FileManager.default.copyItem(at: url, to: destURL)
      pickFileCallback?(destURL.path, fileName, fileSize, mimeType)
    } catch {
      print("[CoreFilePickerNative] copy error: \(error)")
      pickFileCallback?(nil, nil, 0, nil)
    }
  }

  private func handleMultiPicked(_ urls: [URL]) {
    var results: [[String: Any]] = []
    let tempDir = NSTemporaryDirectory() + "upload_picker/"
    try? FileManager.default.createDirectory(atPath: tempDir, withIntermediateDirectories: true)

    for url in urls {
      url.startAccessingSecurityScopedResource()
      defer { url.stopAccessingSecurityScopedResource() }

      let fileName = url.lastPathComponent
      var fileSize: Int64 = 0
      if let attrs = try? FileManager.default.attributesOfItem(atPath: url.path) {
        fileSize = (attrs[.size] as? Int64) ?? 0
      }
      let mimeType = UTType(filenameExtension: url.pathExtension)?.preferredMIMEType ?? ""
      let destURL = URL(fileURLWithPath: tempDir + fileName)

      if let _ = try? FileManager.default.copyItem(at: url, to: destURL) {
        results.append([
          "filePath": destURL.path,
          "fileName": fileName,
          "fileSize": fileSize,
          "mimeType": mimeType
        ])
      }
    }

    if results.isEmpty {
      multiFileCallback?(nil)
    } else if let jsonData = try? JSONSerialization.data(withJSONObject: results),
              let jsonStr = String(data: jsonData, encoding: .utf8) {
      multiFileCallback?(jsonStr)
    } else {
      multiFileCallback?(nil)
    }
  }
}
