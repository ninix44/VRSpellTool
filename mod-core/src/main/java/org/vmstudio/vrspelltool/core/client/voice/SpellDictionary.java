package org.vmstudio.vrspelltool.core.client.voice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class SpellDictionary {
    private SpellDictionary() {
    }

    private static final List<SpellPattern> SPELLS = List.of(
            spell(
                    "avada_kedavra",
                    "Avada Kedavra",
                    List.of(
                            "avada kedavra",
                            "avada kadavra",
                            "avada kedabra",
                            "avada kidavra",
                            "ava da kedavra",
                            "ava da kidavra",
                            "a bada cadaver",
                            "about a cadaver",
                            "avadakidavra",
                            "\u0430\u0432\u0430\u0434\u0430 \u043a\u0435\u0434\u0430\u0432\u0440\u0430",
                            "\u0430\u0432\u0430\u0434\u0430 \u043a\u0438\u0434\u0430\u0432\u0440\u0430"
                    ),
                    List.of(
                            List.of("avada", "kedavra"),
                            List.of("ava", "da", "kedavra"),
                            List.of("avada", "kidavra"),
                            List.of("ava", "da", "ki", "davra"),
                            List.of("avada", "ki", "davra"),
                            List.of("avada", "ki davra"),
                            List.of("ava", "da", "kidavra"),
                            List.of("a", "bada", "cadaver"),
                            List.of("about", "a", "cadaver")
                    )
            ),
            spell(
                    "crucio",
                    "Crucio",
                    List.of(
                            "crucio",
                            "crusio",
                            "crushio",
                            "krucio",
                            "krutso",
                            "krusio",
                            "crew see oh",
                            "crew cio",
                            "cru cio",
                            "kru cio",
                            "cru shi o",
                            "cru see oh",
                            "kru see oh",
                            "cru she oh",
                            "crew shio",
                            "kru shio",
                            "cru zio",
                            "crew seo",
                            "kru seo",
                            "kru chio",
                            "cru chio",
                            "kru tso",
                            "cru tso",
                            "kru sio",
                            "kru zio",
                            "kru cio",
                            "cru seo",
                            "kru seo",
                            "\u043a\u0440\u0443\u0446\u0438\u043e",
                            "\u043a\u0440\u0443\u0447\u0438\u043e"
                    ),
                    List.of(
                            List.of("cru", "cio"),
                            List.of("kru", "cio"),
                            List.of("cru", "tsio"),
                            List.of("kru", "tsio"),
                            List.of("cru", "sio"),
                            List.of("crew", "see", "oh"),
                            List.of("crew", "cio"),
                            List.of("kru", "cio"),
                            List.of("cru", "shi", "o"),
                            List.of("crew", "shio"),
                            List.of("kru", "shio"),
                            List.of("cru", "zio"),
                            List.of("crew", "seo"),
                            List.of("kru", "chio"),
                            List.of("cru", "chio"),
                            List.of("kru", "tso"),
                            List.of("cru", "tso"),
                            List.of("kru", "sio"),
                            List.of("kru", "zio")
                    )
            ),
            spell(
                    "expelliarmus",
                    "Expelliarmus",
                    List.of(
                            "expelliarmus",
                            "expeliarmus",
                            "ekspelyarmus",
                            "expelli armus",
                            "expel yar mus",
                            "expeli ar mus",
                            "ex pel liar mus",
                            "spell the armus",
                            "expel the armus",
                            "\u044d\u043a\u0441\u043f\u0435\u043b\u043b\u0438\u0430\u0440\u043c\u0443\u0441",
                            "\u044d\u043a\u0441\u043f\u0435\u043b\u044f\u0440\u043c\u0443\u0441"
                    ),
                    List.of(
                            List.of("expel", "liar", "mus"),
                            List.of("expelli", "armus"),
                            List.of("ekspel", "yar", "mus"),
                            List.of("expel", "yar", "mus"),
                            List.of("expeli", "armus"),
                            List.of("ex", "pel", "liar", "mus"),
                            List.of("spell", "the", "armus"),
                            List.of("expel", "the", "armus"),
                            List.of("expel", "ar", "mus")
                    )
            ),
            spell(
                    "lumos",
                    "Lumos",
                    List.of(
                            "lumos",
                            "lumoss",
                            "lumas",
                            "loomos",
                            "lyumos",
                            "lu mos",
                            "lou moss",
                            "blue moss",
                            "\u043b\u044e\u043c\u043e\u0441",
                            "\u043b\u044e\u043c\u0443\u0441"
                    ),
                    List.of(
                            List.of("lu", "mos"),
                            List.of("lyu", "mos"),
                            List.of("lou", "moss"),
                            List.of("blue", "moss"),
                            List.of("\u043b\u044e", "\u043c\u043e\u0441")
                    )
            ),
            spell(
                    "sectumsempra",
                    "Sectumsempra",
                    List.of(
                            "sectumsempra",
                            "sektumsempra",
                            "\u0441\u0435\u043a\u0442\u0443\u043c\u0441\u0435\u043c\u043f\u0440\u0430"
                    ),
                    List.of(
                            List.of("sectum", "sempra"),
                            List.of("sektum", "sempra"),
                            List.of("sek", "tum", "sem", "pra"),
                            List.of("\u0441\u0435\u043a\u0442\u0443\u043c", "\u0441\u0435\u043c\u043f\u0440\u0430")
                    )
            )
    );

    public static String grammarJson() {
        List<String> entries = new ArrayList<>();
        for (SpellPattern spell : SPELLS) {
            entries.addAll(spell.fullForms());
            for (List<String> sequence : spell.sequences()) {
                entries.addAll(sequence);
            }
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(entries.get(i))).append('"');
        }
        builder.append(",\"[unk]\"]");
        return builder.toString();
    }

    public static List<SpellPattern> spells() {
        return SPELLS;
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

    private static SpellPattern spell(String id, String displayName, List<String> fullForms, List<List<String>> sequences) {
        SpellCandidate candidate = new SpellCandidate(
                id,
                displayName,
                fullForms.stream().map(SpellDictionary::normalize).collect(Collectors.toUnmodifiableSet()),
                fullForms.stream().map(SpellDictionary::normalize).collect(Collectors.toUnmodifiableSet())
        );

        return new SpellPattern(
                candidate,
                fullForms.stream().map(SpellDictionary::normalize).toList(),
                sequences.stream()
                        .map(sequence -> sequence.stream().map(SpellDictionary::normalize).toList())
                        .toList()
        );
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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

    public record SpellPattern(SpellCandidate candidate, List<String> fullForms, List<List<String>> sequences) {
    }
}
