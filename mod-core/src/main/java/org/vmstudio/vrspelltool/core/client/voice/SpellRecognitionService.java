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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SpellRecognitionService {
    private static final float SAMPLE_RATE = 16000.0F;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final int AUDIO_BUFFER_SIZE = 2048;
    private static final double FULL_FORM_THRESHOLD = 0.70D;
    private static final double STEP_THRESHOLD = 0.62D;
    private static final long SEQUENCE_TIMEOUT_NANOS = 2_600_000_000L;
    private static final long FRAGMENT_WINDOW_NANOS = 2_200_000_000L;
    private static final int MAX_FRAGMENT_COUNT = 4;
    private static final int MAX_STEP_SUFFIX_WORDS = 3;

    private final ConcurrentLinkedQueue<SpellRecognitionSnapshot> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<SequenceState> sequenceStates = new ArrayList<>();
    private final ArrayDeque<RecognizedFragment> recentCompletedFragments = new ArrayDeque<>();

    private volatile String statusMessage = "Voice debug idle";
    private volatile double lastAudioLevel;
    private Thread workerThread;
    private TargetDataLine microphone;
    private Model model;
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
            spellRecognizer = new Recognizer(model, SAMPLE_RATE, SpellDictionary.grammarJson());

            sequenceStates.clear();
            for (SpellDictionary.SpellPattern pattern : SpellDictionary.spells()) {
                sequenceStates.add(new SequenceState(pattern));
            }

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(AUDIO_FORMAT);
            microphone.start();

            running.set(true);
            workerThread = new Thread(this::captureLoop, "VRSpellTool-Speech");
            workerThread.setDaemon(true);
            workerThread.start();
            statusMessage = "Voice debug active. Listening with offline model: " + modelPath.getFileName() + " | ordered syllable spell mode enabled";
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
        sequenceStates.clear();
        recentCompletedFragments.clear();

        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
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
        byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

        try {
            while (running.get()) {
                int read = microphone.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    continue;
                }
                lastAudioLevel = computeAudioLevel(buffer, read);
                processSpellRecognizer(buffer, read);
            }
        } catch (Exception exception) {
            statusMessage = "Voice debug stopped: " + cleanMessage(exception);
        } finally {
            stop();
        }
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
            if (completed) {
                spellRecognizer.reset();
            }
            return;
        }

        SpellRecognitionSnapshot fullMatch = tryFullForm(text);
        if (fullMatch != null) {
            queue.offer(fullMatch);
            resetAllSequences();
            recentCompletedFragments.clear();
            spellRecognizer.reset();
            return;
        }

        SpellRecognitionSnapshot sequenceMatch = advanceSequences(text);
        if (sequenceMatch != null) {
            queue.offer(sequenceMatch);
            resetAllSequences();
            recentCompletedFragments.clear();
            spellRecognizer.reset();
            return;
        }

        if (completed) {
            SpellRecognitionSnapshot fragmentMatch = tryBestSpellFromRecentFragments(text);
            if (fragmentMatch != null) {
                queue.offer(fragmentMatch);
                resetAllSequences();
                recentCompletedFragments.clear();
                spellRecognizer.reset();
                return;
            }
        }

        if (completed) {
            spellRecognizer.reset();
        }
    }

    private SpellRecognitionSnapshot tryFullForm(String text) {
        SpellCandidate bestCandidate = null;
        double bestScore = 0.0D;

        for (SpellDictionary.SpellPattern pattern : SpellDictionary.spells()) {
            double score = SpellDictionary.bestScore(text, pattern.fullForms());
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = pattern.candidate();
            }
        }

        if (bestCandidate == null || bestScore < FULL_FORM_THRESHOLD) {
            return null;
        }

        return new SpellRecognitionSnapshot(
                bestCandidate.displayName(),
                false,
                true,
                bestCandidate,
                bestScore
        );
    }

    private SpellRecognitionSnapshot advanceSequences(String text) {
        long now = System.nanoTime();
        for (SequenceState state : sequenceStates) {
            state.expireIfTimedOut(now);

            for (List<String> sequence : state.pattern.sequences()) {
                int matchedSteps = state.progressFor(sequence);
                if (matchedSteps >= sequence.size()) {
                    state.reset(sequence);
                    continue;
                }

                double score = scoreSequenceStep(text, sequence.get(matchedSteps));
                if (score >= STEP_THRESHOLD) {
                    int nextStep = matchedSteps + 1;
                    state.update(sequence, nextStep, now);
                    if (nextStep >= sequence.size()) {
                        state.reset(sequence);
                        return new SpellRecognitionSnapshot(
                                state.pattern.candidate().displayName(),
                                false,
                                true,
                                state.pattern.candidate(),
                                score
                        );
                    }
                } else if (matchedSteps == 0) {
                } else {
                    state.reset(sequence);
                    double restartScore = scoreSequenceStep(text, sequence.get(0));
                    if (restartScore >= STEP_THRESHOLD) {
                        state.update(sequence, 1, now);
                    }
                }
            }
        }

        return null;
    }

    private SpellRecognitionSnapshot tryBestSpellFromRecentFragments(String text) {
        rememberCompletedFragment(text);

        List<String> fragments = recentCompletedFragmentTexts();
        if (fragments.size() < 2) {
            return null;
        }

        SpellDictionary.SpellPattern bestPattern = null;
        double bestScore = 0.0D;
        for (SpellDictionary.SpellPattern pattern : SpellDictionary.spells()) {
            for (int count = 2; count <= fragments.size(); count++) {
                String combined = String.join(" ", fragments.subList(fragments.size() - count, fragments.size()));
                double score = SpellDictionary.bestScore(combined, pattern.fullForms());
                if (score > bestScore) {
                    bestScore = score;
                    bestPattern = pattern;
                }
            }
        }

        if (bestPattern == null || bestScore < FULL_FORM_THRESHOLD) {
            return null;
        }

        return new SpellRecognitionSnapshot(
                bestPattern.candidate().displayName(),
                false,
                true,
                bestPattern.candidate(),
                bestScore
        );
    }

    private double scoreSequenceStep(String heard, String expected) {
        List<String> variants = new ArrayList<>();
        variants.add(expected);

        if ("avada".equals(expected)) {
            variants.add("ava da");
        } else if ("kedavra".equals(expected)) {
            variants.add("kidavra");
            variants.add("ki davra");
            variants.add("davra");
            variants.add("cadaver");
        } else if ("kidavra".equals(expected)) {
            variants.add("kedavra");
            variants.add("ki davra");
            variants.add("davra");
            variants.add("cadaver");
        } else if ("cru".equals(expected) || "kru".equals(expected)) {
            variants.add("crew");
            variants.add("croo");
            variants.add("kru");
            variants.add("cru");
            variants.add("cre");
            variants.add("cryu");
        } else if ("lu".equals(expected) || "lyu".equals(expected) || "lou".equals(expected)) {
            variants.add("lu");
            variants.add("lyu");
            variants.add("lou");
            variants.add("loo");
            variants.add("лю");
        } else if ("mos".equals(expected) || "moss".equals(expected)) {
            variants.add("mos");
            variants.add("moss");
            variants.add("moz");
            variants.add("mass");
            variants.add("мосс");
            variants.add("мос");
        } else if ("blue".equals(expected)) {
            variants.add("blue");
            variants.add("blu");
        } else if ("cio".equals(expected) || "tsio".equals(expected) || "sio".equals(expected)) {
            variants.add("cio");
            variants.add("tsio");
            variants.add("sio");
            variants.add("zio");
            variants.add("tso");
            variants.add("see oh");
            variants.add("shi o");
            variants.add("shio");
            variants.add("chio");
            variants.add("she oh");
            variants.add("see o");
            variants.add("chi o");
            variants.add("seo");
            variants.add("zee oh");
            variants.add("zio");
            variants.add("shi");
            variants.add("chio");
            variants.add("tchio");
            variants.add("cheeo");
        } else if ("chio".equals(expected)) {
            variants.add("chio");
            variants.add("chi o");
            variants.add("cio");
            variants.add("shio");
            variants.add("tchio");
        } else if ("tso".equals(expected)) {
            variants.add("tso");
            variants.add("zo");
            variants.add("so");
        } else if ("see".equals(expected)) {
            variants.add("si");
            variants.add("sii");
            variants.add("shi");
            variants.add("see");
        } else if ("oh".equals(expected) || "o".equals(expected)) {
            variants.add("o");
            variants.add("oh");
        } else if ("expel".equals(expected) || "expelli".equals(expected) || "expeli".equals(expected) || "ekspel".equals(expected)) {
            variants.add("expel");
            variants.add("expelli");
            variants.add("expeli");
            variants.add("ekspel");
            variants.add("ex pel");
            variants.add("spell");
        } else if ("ex".equals(expected)) {
            variants.add("ex");
            variants.add("x");
        } else if ("pel".equals(expected)) {
            variants.add("pel");
            variants.add("spell");
        } else if ("liar".equals(expected) || "yar".equals(expected)) {
            variants.add("liar");
            variants.add("yar");
            variants.add("li ar");
            variants.add("lee ar");
            variants.add("lar");
        } else if ("the".equals(expected)) {
            variants.add("the");
            variants.add("da");
        } else if ("ar".equals(expected)) {
            variants.add("ar");
            variants.add("are");
        } else if ("armus".equals(expected)) {
            variants.add("armus");
            variants.add("yar mus");
            variants.add("ar mus");
            variants.add("armis");
        } else if ("mus".equals(expected)) {
            variants.add("mus");
            variants.add("mes");
            variants.add("muse");
        } else if ("sempra".equals(expected)) {
            variants.add("sempra");
            variants.add("sem pra");
        }

        double bestScore = SpellDictionary.bestScore(heard, variants);
        List<String> suffixes = extractSuffixCandidates(heard);
        for (String suffix : suffixes) {
            bestScore = Math.max(bestScore, SpellDictionary.bestScore(suffix, variants));
        }
        return bestScore;
    }

    private void resetAllSequences() {
        for (SequenceState state : sequenceStates) {
            state.resetAll();
        }
    }

    private void rememberCompletedFragment(String text) {
        long now = System.nanoTime();
        pruneCompletedFragments(now);
        if (recentCompletedFragments.size() >= MAX_FRAGMENT_COUNT) {
            recentCompletedFragments.removeFirst();
        }
        recentCompletedFragments.addLast(new RecognizedFragment(text, now));
    }

    private List<String> recentCompletedFragmentTexts() {
        long now = System.nanoTime();
        pruneCompletedFragments(now);
        List<String> fragments = new ArrayList<>();
        for (RecognizedFragment fragment : recentCompletedFragments) {
            fragments.add(fragment.text());
        }
        return fragments;
    }

    private void pruneCompletedFragments(long now) {
        while (!recentCompletedFragments.isEmpty()
                && now - recentCompletedFragments.peekFirst().timestampNanos() > FRAGMENT_WINDOW_NANOS) {
            recentCompletedFragments.removeFirst();
        }
    }

    private List<String> extractSuffixCandidates(String text) {
        List<String> suffixes = new ArrayList<>();
        String[] words = text.split(" ");
        if (words.length <= 1) {
            return suffixes;
        }

        int maxWords = Math.min(words.length, MAX_STEP_SUFFIX_WORDS);
        for (int count = 1; count <= maxWords; count++) {
            int start = words.length - count;
            suffixes.add(String.join(" ", java.util.Arrays.copyOfRange(words, start, words.length)));
        }
        return suffixes;
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

    private record RecognizedFragment(String text, long timestampNanos) {
    }

    private static final class SequenceState {
        private final SpellDictionary.SpellPattern pattern;
        private final List<SequenceProgress> progresses = new ArrayList<>();

        private SequenceState(SpellDictionary.SpellPattern pattern) {
            this.pattern = pattern;
            for (List<String> sequence : pattern.sequences()) {
                progresses.add(new SequenceProgress(sequence));
            }
        }

        private int progressFor(List<String> sequence) {
            for (SequenceProgress progress : progresses) {
                if (progress.sequence.equals(sequence)) {
                    return progress.stepIndex;
                }
            }
            return 0;
        }

        private void update(List<String> sequence, int stepIndex, long now) {
            for (SequenceProgress progress : progresses) {
                if (progress.sequence.equals(sequence)) {
                    progress.stepIndex = stepIndex;
                    progress.lastUpdateNanos = now;
                    return;
                }
            }
        }

        private void reset(List<String> sequence) {
            for (SequenceProgress progress : progresses) {
                if (progress.sequence.equals(sequence)) {
                    progress.stepIndex = 0;
                    progress.lastUpdateNanos = 0L;
                    return;
                }
            }
        }

        private void resetAll() {
            for (SequenceProgress progress : progresses) {
                progress.stepIndex = 0;
                progress.lastUpdateNanos = 0L;
            }
        }

        private void expireIfTimedOut(long now) {
            for (SequenceProgress progress : progresses) {
                if (progress.stepIndex > 0 && now - progress.lastUpdateNanos > SEQUENCE_TIMEOUT_NANOS) {
                    progress.stepIndex = 0;
                    progress.lastUpdateNanos = 0L;
                }
            }
        }
    }

    private static final class SequenceProgress {
        private final List<String> sequence;
        private int stepIndex;
        private long lastUpdateNanos;

        private SequenceProgress(List<String> sequence) {
            this.sequence = sequence;
        }
    }
}
