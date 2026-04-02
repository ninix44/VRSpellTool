package org.vmstudio.vrspelltool.core.client.voice;

import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SpellDictionary {
    private SpellDictionary() {
    }

    public static void initialize() {
        SpellConfigStore.initialize();
    }

    public static void reload() {
        SpellConfigStore.reload();
    }

    public static void reloadIfChanged() {
        SpellConfigStore.reloadIfChanged();
    }

    public static String grammarJson() {
        return SpellConfigStore.getGrammarJson();
    }

    public static List<SpellPattern> spells() {
        return SpellConfigStore.getPatterns();
    }

    public static @Nullable SpellPattern findPattern(String spellId) {
        for (SpellPattern pattern : spells()) {
            if (pattern.candidate().id().equals(spellId)) {
                return pattern;
            }
        }
        return null;
    }

    public static @Nullable SpellPattern detectBySpeechFragment(String text) {
        String normalized = normalize(text);
        for (SpellPattern pattern : spells()) {
            for (String keyword : pattern.candidate().startKeywords()) {
                if (normalized.contains(keyword)) {
                    return pattern;
                }
            }
        }
        return null;
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('\u0451', '\u0435')
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static double score(String heard, String expected) {
        String normalizedHeard = normalize(heard);
        String normalizedExpected = normalize(expected);

        if (normalizedHeard.equals(normalizedExpected)) {
            return 1.0D;
        }

        String compactHeard = normalizedHeard.replace(" ", "");
        String compactExpected = normalizedExpected.replace(" ", "");
        if (compactHeard.equals(compactExpected)) {
            return 0.98D;
        }

        int distance = levenshteinDistance(compactHeard, compactExpected);
        int maxLength = Math.max(compactHeard.length(), compactExpected.length());
        if (maxLength == 0) {
            return 1.0D;
        }

        return 1.0D - ((double) distance / (double) maxLength);
    }

    public static double bestScore(String heard, List<String> expectedVariants) {
        double best = 0.0D;
        for (String expected : expectedVariants) {
            best = Math.max(best, score(heard, expected));
        }
        return best;
    }

    static SpellPattern spell(SpellConfigStore.SpellDefinition definition) {
        List<String> normalizedFullForms = definition.fullForms().stream()
                .map(SpellDictionary::normalize)
                .distinct()
                .toList();
        List<List<String>> normalizedSequences = definition.sequences().stream()
                .map(sequence -> sequence.stream().map(SpellDictionary::normalize).toList())
                .toList();

        var normalizedStepVariants = new java.util.LinkedHashMap<String, List<String>>();
        definition.stepVariants().forEach((key, values) -> normalizedStepVariants.put(
                normalize(key),
                values.stream().map(SpellDictionary::normalize).distinct().toList()
        ));

        SpellCandidate candidate = new SpellCandidate(
                definition.id(),
                definition.displayName(),
                Set.copyOf(normalizedFullForms),
                Set.copyOf(normalizedFullForms),
                definition.startKeywords().stream().map(SpellDictionary::normalize).distinct().toList(),
                definition.allowedGestures().stream().map(String::toUpperCase).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                definition.particleStyle(),
                definition.hapticStrength()
        );

        return new SpellPattern(candidate, normalizedFullForms, normalizedSequences, normalizedStepVariants);
    }

    private static int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            char leftChar = left.charAt(i - 1);

            for (int j = 1; j <= right.length(); j++) {
                int cost = leftChar == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    public record SpellPattern(SpellCandidate candidate,
                               List<String> fullForms,
                               List<List<String>> sequences,
                               java.util.Map<String, List<String>> stepVariants) {
    }
}
