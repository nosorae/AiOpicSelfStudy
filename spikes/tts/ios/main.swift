// 질문 문장을 AVSpeechSynthesizer 로 읽어 caf 파일로 쓴다.
// 사용: tts <질문 파일> <출력 폴더>
// 같은 소스를 macOS 와 iOS 시뮬레이터용으로 컴파일한다.
import AVFoundation

let args = CommandLine.arguments
let lines = try! String(contentsOfFile: args[1], encoding: .utf8)
    .split(separator: "\n").map(String.init)
let outDir = URL(fileURLWithPath: args[2])

let enUS = AVSpeechSynthesisVoice.speechVoices().filter { $0.language == "en-US" }
for v in enUS { print("voice\t\(v.identifier)\t\(v.name)\tquality=\(v.quality.rawValue)") }

// 기본 음성: 언어 코드만 줬을 때 엔진이 고르는 음성
var picks: [(String, AVSpeechSynthesisVoice)] = [("default", AVSpeechSynthesisVoice(language: "en-US")!)]
// 고품질: 설치된 en-US 음성 중 품질이 가장 높은 것 (Premium 3 > Enhanced 2)
if let best = enUS.max(by: { $0.quality.rawValue < $1.quality.rawValue }), best.quality != .default {
    picks.append(("hq", best))
} else {
    print("no enhanced/premium en-US voice installed")
}

let synth = AVSpeechSynthesizer()
for (tag, voice) in picks {
    for (i, text) in lines.enumerated() {
        let u = AVSpeechUtterance(string: text)
        u.voice = voice
        u.rate = AVSpeechUtteranceDefaultSpeechRate
        let url = outDir.appendingPathComponent("ios-\(tag)-q\(i + 1).caf")
        var file: AVAudioFile?
        let done = DispatchSemaphore(value: 0)
        synth.write(u) { buf in
            guard let pcm = buf as? AVAudioPCMBuffer else { return }
            if pcm.frameLength == 0 { done.signal(); return }  // 빈 버퍼가 끝 표시다
            if file == nil { file = try! AVAudioFile(forWriting: url, settings: pcm.format.settings) }
            try! file!.write(from: pcm)
        }
        while done.wait(timeout: .now()) == .timedOut { RunLoop.current.run(until: Date().addingTimeInterval(0.05)) }
        file = nil
        print("wrote\t\(url.lastPathComponent)\t\(voice.name)\t\(voice.identifier)\trate=\(u.rate)")
    }
}
