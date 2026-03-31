package org.vmstudio.vrspelltool.core.client.voice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpellDictionary {
    private static final double MATCH_THRESHOLD = 0.38D;

    private static final List<SpellCandidate> SPELLS = List.of(
            spell("avada_kedavra", "Avada Kedavra",
                    phrases(
                            "\u0430\u0432\u0430\u0434\u0430 \u043a\u0435\u0434\u0430\u0432\u0440\u0430",
                            "\u0430\u0432\u0430\u0434\u0430 \u043a\u0438\u0434\u0430\u0432\u0440\u0430",
                            "avada kedavra",
                            "avada kadavra",
                            "avada kedabra",
                            "avada kedara",
                            "avadakidavra",
                            "avada kidavra",
                            "ava da kedavra",
                            "ava da",
                            "avadakedavra",
                            "avra kedavra",
                            "avadah kedavra",
                            "abada kedavra",
                            "avada",
                            "kedavra"
                    ),
                    grammar(
                            "avada kedavra",
                            "avada kadavra",
                            "avada kedabra",
                            "avada kedara",
                            "avadakidavra",
                            "avada kidavra",
                            "ava",
                            "da",
                            "keda",
                            "kedavra",
                            "vra",
                            "ava da",
                            "ava da kedavra",
                            "avadakedavra",
                            "avra kedavra",
                            "avadah kedavra",
                            "abada kedavra",
                            "\u0430\u0432\u0430\u0434\u0430 \u043a\u0435\u0434\u0430\u0432\u0440\u0430",
                            "\u0430\u0432\u0430\u0434\u0430 \u043a\u0438\u0434\u0430\u0432\u0440\u0430"
                    )),
            spell("crucio", "Crucio",
                    phrases(
                            "\u043a\u0440\u0443\u0446\u0438\u043e",
                            "\u043a\u0440\u0443\u0447\u0438\u043e",
                            "\u043a\u0440\u0443\u0446\u0438\u0430\u0442\u0443\u0441",
                            "\u043a\u0440\u0443\u0447\u0438\u0430\u0442\u0443\u0441",
                            "crucio",
                            "crusio",
                            "crushio",
                            "krucio",
                            "krutso",
                            "krusio",
                            "cru cio",
                            "croozio",
                            "cruciatus",
                            "cruciatos"
                    ),
                    grammar(
                            "crucio",
                            "crusio",
                            "crushio",
                            "krucio",
                            "krutso",
                            "krusio",
                            "cru",
                            "kru",
                            "cio",
                            "tsio",
                            "cru cio",
                            "croozio",
                            "cruciatus",
                            "cruciatos",
                            "\u043a\u0440\u0443\u0446\u0438\u043e",
                            "\u043a\u0440\u0443\u0447\u0438\u043e"
                    )),
            spell("expelliarmus", "Expelliarmus",
                    phrases(
                            "\u044d\u043a\u0441\u043f\u0435\u043b\u043b\u0438\u0430\u0440\u043c\u0443\u0441",
                            "\u044d\u043a\u0441\u043f\u0435\u043b\u0438\u0430\u0440\u043c\u0443\u0441",
                            "\u044d\u043a\u0441\u043f\u0435\u043b\u044f\u0440\u043c\u0443\u0441",
                            "expelliarmus",
                            "expeliarmus",
                            "ekspelyarmus",
                            "expel yar mus",
                            "expeliar mus",
                            "expelliarms",
                            "expelli armus",
                            "expellarmus"
                    ),
                    grammar(
                            "expelliarmus",
                            "expeliarmus",
                            "ekspelyarmus",
                            "expel",
                            "liar",
                            "armus",
                            "mus",
                            "expel yar mus",
                            "expeliar mus",
                            "expelli armus",
                            "expellarmus",
                            "\u044d\u043a\u0441\u043f\u0435\u043b\u043b\u0438\u0430\u0440\u043c\u0443\u0441",
                            "\u044d\u043a\u0441\u043f\u0435\u043b\u0438\u0430\u0440\u043c\u0443\u0441"
                    )),
            spell("lumos", "Lumos",
                    phrases(
                            "\u043b\u044e\u043c\u043e\u0441",
                            "\u043b\u044e\u043c\u0430\u0441",
                            "\u043b\u044e\u043c\u0443\u0441",
                            "lumos",
                            "lumoss",
                            "lumas",
                            "loomos",
                            "lyumos"
                    ),
                    grammar(
                            "lumos",
                            "lumas",
                            "loomos",
                            "lyumos",
                            "lu",
                            "lum",
                            "mos",
                            "\u043b\u044e\u043c\u043e\u0441",
                            "\u043b\u044e\u043c\u0430\u0441"
                    )),
            spell("sectumsempra", "Sectumsempra",
                    phrases(
                            "\u0441\u0435\u043a\u0442\u0443\u043c\u0441\u0435\u043c\u043f\u0440\u0430",
                            "\u0441\u0435\u043a\u0442\u0443\u043c\u0441\u0435\u043c\u043f\u0430\u0440\u0430",
                            "\u0441\u0435\u043a\u0442\u0443\u043c\u0441\u0435\u043c\u043f\u043e\u0440\u0430",
                            "sectumsempra",
                            "sektumsempra",
                            "sek tum sem pra",
                            "sectum sempra",
                            "sectumsempa"
                    ),
                    grammar(
                            "sectumsempra",
                            "sektumsempra",
                            "sek",
                            "tum",
                            "sem",
                            "pra",
                            "sek tum sem pra",
                            "sectum sempra",
                            "sectumsempa",
                            "\u0441\u0435\u043a\u0442\u0443\u043c\u0441\u0435\u043c\u043f\u0440\u0430"
                    ))
    );

    private SpellDictionary() {
    }

    public static SpellMatch match(String transcript) {
        String normalized = normalize(transcript);
        if (normalized.isBlank()) {
            return null;
        }

        SpellCandidate bestCandidate = null;
        double bestScore = 0.0D;

        for (SpellCandidate spell : SPELLS) {
            for (String phrase : spell.phrases()) {
                double score = score(normalized, phrase);
                if (score > bestScore) {
                    bestScore = score;
                    bestCandidate = spell;
                }
            }
        }

        if (bestCandidate == null || bestScore < MATCH_THRESHOLD) {
            return null;
        }

        return new SpellMatch(bestCandidate, bestScore);
    }

    public static String grammarJson() {
        StringBuilder builder = new StringBuilder("[");
        List<String> phrases = new ArrayList<>();
        for (SpellCandidate spell : SPELLS) {
            phrases.addAll(spell.grammarPhrases());
        }
        for (int i = 0; i < phrases.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(phrases.get(i))).append('"');
        }
        builder.append(",\"[unk]\"]");
        return builder.toString();
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('\u0451', '\u0435')
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static SpellCandidate spell(String id, String displayName, String[] phrases, String[] grammarPhrases) {
        return new SpellCandidate(
                id,
                displayName,
                java.util.Arrays.stream(phrases).map(SpellDictionary::normalize).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                java.util.Arrays.stream(grammarPhrases).map(SpellDictionary::normalize).collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }

    private static String[] phrases(String... values) {
        return values;
    }

    private static String[] grammar(String... values) {
        return values;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static double score(String transcript, String phrase) {
        if (transcript.equals(phrase)) {
            return 1.0D;
        }

        String compactTranscript = transcript.replace(" ", "");
        String compactPhrase = phrase.replace(" ", "");
        if (compactTranscript.equals(compactPhrase)) {
            return 0.97D;
        }

        if (transcript.contains(phrase) || phrase.contains(transcript)) {
            double ratio = (double) Math.min(transcript.length(), phrase.length())
                    / (double) Math.max(transcript.length(), phrase.length());
            return 0.80D + ratio * 0.20D;
        }

        if (compactTranscript.contains(compactPhrase) || compactPhrase.contains(compactTranscript)) {
            double ratio = (double) Math.min(compactTranscript.length(), compactPhrase.length())
                    / (double) Math.max(compactTranscript.length(), compactPhrase.length());
            return 0.78D + ratio * 0.18D;
        }

        int distance = levenshteinDistance(transcript, phrase);
        int maxLength = Math.max(transcript.length(), phrase.length());
        if (maxLength == 0) {
            return 1.0D;
        }
        double directScore = 1.0D - ((double) distance / (double) maxLength);

        int compactDistance = levenshteinDistance(compactTranscript, compactPhrase);
        int compactMaxLength = Math.max(compactTranscript.length(), compactPhrase.length());
        double compactScore = compactMaxLength == 0 ? 1.0D : 1.0D - ((double) compactDistance / (double) compactMaxLength);

        return Math.max(directScore, compactScore);
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

    public record SpellMatch(SpellCandidate candidate, double score) {
    }
}
