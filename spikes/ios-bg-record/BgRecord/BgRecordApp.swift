import AVFoundation
import SwiftUI
import UIKit

// 스파이크: 화면이 꺼진 채 녹음이 이어지는지 본다. 5초마다 조각 파일을 새로 쓰고 log.jsonl 에 남긴다.
final class Recorder {
    static let shared = Recorder()
    private let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    private var recorder: AVAudioRecorder?
    private var startedAt = Date()
    private var index = 0
    private var timer: Timer?

    func log(_ event: String, _ extra: [String: Any] = [:]) {
        var line = extra
        line["t"] = ISO8601DateFormatter().string(from: Date())
        line["event"] = event
        line["appState"] = UIApplication.shared.applicationState.rawValue
        guard let data = try? JSONSerialization.data(withJSONObject: line) else { return }
        let url = docs.appendingPathComponent("log.jsonl")
        if let h = try? FileHandle(forWritingTo: url) {
            h.seekToEndOfFile(); h.write(data); h.write("\n".data(using: .utf8)!); try? h.close()
        } else {
            try? (data + "\n".data(using: .utf8)!).write(to: url, options: .noFileProtection)
            try? FileManager.default.setAttributes([.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication], ofItemAtPath: url.path)
        }
    }

    func start() {
        try? FileManager.default.setAttributes([.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication], ofItemAtPath: docs.path)
        let nc = NotificationCenter.default
        nc.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { n in
            let type = n.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt ?? 99
            let opts = n.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            self.log("interruption", ["type": type == 1 ? "began" : "ended", "options": opts])
            if type == 0 { try? AVAudioSession.sharedInstance().setActive(true); self.rotate() }
        }
        nc.addObserver(forName: AVAudioSession.routeChangeNotification, object: nil, queue: .main) { n in
            self.log("routeChange", ["reason": n.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt ?? 99])
        }
        for (name, label) in [
            (UIApplication.didEnterBackgroundNotification, "didEnterBackground"),
            (UIApplication.willEnterForegroundNotification, "willEnterForeground"),
            (UIApplication.didBecomeActiveNotification, "didBecomeActive"),
            (UIApplication.willResignActiveNotification, "willResignActive"),
            (UIApplication.protectedDataWillBecomeUnavailableNotification, "protectedDataWillBecomeUnavailable"),
            (UIApplication.protectedDataDidBecomeAvailableNotification, "protectedDataDidBecomeAvailable"),
            (UIApplication.willTerminateNotification, "willTerminate"),
        ] {
            nc.addObserver(forName: name, object: nil, queue: .main) { _ in self.log(label) }
        }
        log("launch")
        AVAudioApplication.requestRecordPermission { granted in
            DispatchQueue.main.async {
                self.log("permission", ["granted": granted])
                guard granted else { return }
                do {
                    let s = AVAudioSession.sharedInstance()
                    try s.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
                    try s.setActive(true)
                } catch { self.log("sessionError", ["error": "\(error)"]); return }
                self.rotate()
                // 백그라운드에서도 메인 런루프는 오디오 세션이 살아 있는 동안 돈다. 안 돌면 조각 간격이 늘어나는 것으로 드러난다.
                self.timer = Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { _ in self.rotate() }
            }
        }
    }

    private func rotate() {
        if let r = recorder {
            let dur = r.currentTime
            r.stop()
            log("chunk", ["file": r.url.lastPathComponent, "createdAt": ISO8601DateFormatter().string(from: startedAt), "duration": dur])
        }
        index += 1
        let url = docs.appendingPathComponent(String(format: "chunk-%04d.m4a", index))
        let settings: [String: Any] = [AVFormatIDKey: kAudioFormatMPEG4AAC, AVSampleRateKey: 16000, AVNumberOfChannelsKey: 1]
        do {
            let r = try AVAudioRecorder(url: url, settings: settings)
            startedAt = Date()
            if !r.record() { log("recordFailed", ["file": url.lastPathComponent]) }
            try? FileManager.default.setAttributes([.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication], ofItemAtPath: url.path)
            recorder = r
        } catch { log("recorderError", ["error": "\(error)"]) }
    }
}

@main
struct BgRecordApp: App {
    init() { Recorder.shared.start() }
    var body: some Scene {
        WindowGroup { Text("녹음 중. 잠금 버튼을 눌러도 된다.").padding() }
    }
}
