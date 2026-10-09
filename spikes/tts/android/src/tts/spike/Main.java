package tts.spike;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** assets/questions.txt 의 줄마다 기본 en-US 음성으로 wav 파일을 만든다. 결과는 앱 외부 파일 폴더에 쓴다. */
public class Main extends Activity {
    private TextToSpeech tts;
    private int left;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) { Log.e("TTS", "init failed " + status); finish(); return; }
            int lang = tts.setLanguage(Locale.US);
            Log.i("TTS", "engine=" + tts.getDefaultEngine() + " setLanguage=" + lang + " voice=" + tts.getVoice().getName());
            List<String> lines = readLines();
            left = lines.size();
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {}
                @Override public void onError(String id) { Log.e("TTS", "error " + id); done(id); }
                @Override public void onDone(String id) { Log.i("TTS", "wrote " + id); done(id); }
            });
            for (int i = 0; i < lines.size(); i++) {
                String id = "android-default-q" + (i + 1);
                File out = new File(getExternalFilesDir(null), id + ".wav");
                tts.synthesizeToFile(lines.get(i), null, out, id);
            }
        });
    }

    private synchronized void done(String id) {
        if (--left == 0) { Log.i("TTS", "all done"); tts.shutdown(); finish(); }
    }

    private List<String> readLines() {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("questions.txt")))) {
            for (String l; (l = r.readLine()) != null; ) if (!l.isEmpty()) lines.add(l);
        } catch (Exception e) { throw new RuntimeException(e); }
        return lines;
    }
}
