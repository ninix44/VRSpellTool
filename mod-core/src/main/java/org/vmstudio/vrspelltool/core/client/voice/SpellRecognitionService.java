package org.vmstudio.vrspelltool.core.client.voice;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SpellRecognitionService {
    private static final float SAMPLE_RATE = 16000.0F;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final ConcurrentLinkedQueue<SpellRecognitionSnapshot> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile String statusMessage = "Voice debug idle";
    private volatile double lastAudioLevel;
    private Thread workerThread;
    private TargetDataLine microphone;
    private Model model;
    private Recognizer recognizer;
    private Recognizer spellRecognizer;

    public void ensureRunning(Path gameDirectory) {
        if (running.get()) {
            return;
        }

        Path modelPath = resolveModelPath(gameDirectory);
        if (modelPath == null) {
            statusMessage = "Offline model not found. Put Vosk model into .minecraft/config/vrspelltool/model";
            return;
        }

        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            model = new Model(modelPath.toString());
            recognizer = new Recognizer(model, SAMPLE_RATE);
            spellRecognizer = new Recognizer(model, SAMPLE_RATE, SpellDictionary.grammarJson());

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(AUDIO_FORMAT);
            microphone.start();

            running.set(true);
            workerThread = new Thread(this::captureLoop, "VRSpellTool-Speech");
            workerThread.setDaemon(true);
            workerThread.start();
            statusMessage = "Voice debug active. Listening with offline model: " + modelPath.getFileName() + " | fuzzy spell matching enabled";
        } catch (Exception exception) {
            statusMessage = "Voice debug failed to start: " + cleanMessage(exception);
            stop();
        }
    }

    public SpellRecognitionSnapshot poll() {
        return queue.poll();
    }

    public String getStatusMessage() {
        return statusMessage + " | mic=" + Math.round(lastAudioLevel * 100.0D) + "%";
    }

    public void stop() {
        running.set(false);

        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }

        if (spellRecognizer != null) {
            spellRecognizer.close();
            spellRecognizer = null;
        }

        if (model != null) {
            model.close();
            model = null;
        }

        workerThread = null;
    }

    private void captureLoop() {
        byte[] buffer = new byte[4096];

        try {
            while (running.get()) {
                int read = microphone.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    continue;
                }
                lastAudioLevel = computeAudioLevel(buffer, read);

                processSpellRecognizer(buffer, read);

                boolean completed = recognizer.acceptWaveForm(buffer, read);
                if (!completed) {
                    continue;
                }

                String finalText = SpellDictionary.normalize(extractJsonField(recognizer.getResult(), "text"));
                if (finalText.isBlank()) {
                    continue;
                }

                SpellDictionary.SpellMatch match = SpellDictionary.match(finalText);
                queue.offer(new SpellRecognitionSnapshot(
                        finalText,
                        false,
                        false,
                        match == null ? null : match.candidate(),
                        match == null ? 0.0D : match.score()
                ));
            }
        } catch (Exception exception) {
            statusMessage = "Voice debug stopped: " + cleanMessage(exception);
        } finally {
            stop();
        }
    }

    private static Path resolveModelPath(Path gameDirectory) {
        Path configBase = gameDirectory.resolve("config").resolve("vrspelltool");
        Path[] candidates = new Path[]{
                configBase.resolve("model"),
                configBase.resolve("vosk-model-small-ru-0.22"),
                configBase.resolve("vosk-model-ru-0.42"),
                configBase.resolve("vosk-model-small-en-us-0.15"),
                configBase.resolve("vosk-model-en-us-0.22")
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("am"))) {
                return candidate;
            }
        }

        return null;
    }

    private static String extractJsonField(String json, String fieldName) {
        String quotedName = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(quotedName);
        if (keyIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', keyIndex + quotedName.length());
        if (colonIndex < 0) {
            return "";
        }

        int valueStart = json.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return "";
        }

        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            return "";
        }

        return json.substring(valueStart + 1, valueEnd);
    }

    private static String cleanMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return Objects.requireNonNullElse(message, exception.getClass().getSimpleName());
    }

    private void processSpellRecognizer(byte[] buffer, int read) {
        if (spellRecognizer == null) {
            return;
        }

        boolean completed = spellRecognizer.acceptWaveForm(buffer, read);
        String fieldName = completed ? "text" : "partial";
        String rawText = completed ? spellRecognizer.getResult() : spellRecognizer.getPartialResult();
        String text = SpellDictionary.normalize(extractJsonField(rawText, fieldName));
        if (text.isBlank() || "[unk]".equals(text)) {
            return;
        }

        SpellDictionary.SpellMatch match = SpellDictionary.match(text);
        if (match == null) {
            return;
        }

        queue.offer(new SpellRecognitionSnapshot(
                text,
                !completed,
                true,
                match.candidate(),
                match.score()
        ));

        if (completed) {
            spellRecognizer.reset();
        }
    }

    private static double computeAudioLevel(byte[] buffer, int read) {
        if (read < 2) {
            return 0.0D;
        }

        double sumSquares = 0.0D;
        int samples = 0;
        for (int i = 0; i + 1 < read; i += 2) {
            int sample = (buffer[i] & 0xFF) | (buffer[i + 1] << 8);
            if (sample > 32767) {
                sample -= 65536;
            }
            double normalized = sample / 32768.0D;
            sumSquares += normalized * normalized;
            samples++;
        }

        if (samples == 0) {
            return 0.0D;
        }

        return Math.min(1.0D, Math.sqrt(sumSquares / samples) * 3.0D);
    }
}
