package org.vmstudio.vrspelltool.core.client.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.vrspelltool.core.common.VisorSpellTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SpellConfigStore {
    private static final Logger LOGGER = LogManager.getLogger(VisorSpellTool.MOD_NAME + "-SpellConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = VisorAPI.CONFIG_PATH.resolve(VisorSpellTool.MOD_ID).resolve("spells.json");
    private static final String DEFAULT_RESOURCE = "/defaults/spells.json";
    private static final long RELOAD_CHECK_INTERVAL_MILLIS = 2000L;

    private static volatile State state = State.empty();
    private static volatile long nextReloadCheckAt;

    private SpellConfigStore() {
    }

    static synchronized void initialize() {
        if (!state.patterns().isEmpty()) {
            return;
        }
        load(false);
    }

    static synchronized void reload() {
        load(true);
    }

    static synchronized void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now < nextReloadCheckAt) {
            return;
        }
        nextReloadCheckAt = now + RELOAD_CHECK_INTERVAL_MILLIS;
        try {
            ensureConfigExists();
            long modified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            if (modified != state.lastModified()) {
                load(true);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to reload spell config", exception);
        }
    }

    static List<SpellDictionary.SpellPattern> getPatterns() {
        initialize();
        return state.patterns();
    }

    static String getGrammarJson() {
        initialize();
        return state.grammarJson();
    }

    static synchronized void save(SpellFile file) throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(file, writer);
        }
        load(true);
    }

    private static void load(boolean logReload) {
        try {
            ensureConfigExists();
            SpellFile file = readConfig();
            List<SpellDictionary.SpellPattern> patterns = file.spells().stream()
                    .map(SpellDictionary::spell)
                    .toList();
            String grammarJson = buildGrammarJson(patterns);
            long modified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            state = new State(file, patterns, grammarJson, modified);
            if (logReload) {
                LOGGER.info("Loaded {} spells from {}", patterns.size(), CONFIG_PATH);
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to load spell config from {}", CONFIG_PATH, exception);
            if (state.patterns().isEmpty()) {
                state = State.empty();
            }
        }
    }

    private static SpellFile readConfig() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            SpellFile file = GSON.fromJson(reader, SpellFile.class);
            if (file == null) {
                throw new JsonParseException("Spell config is empty");
            }
            return file.normalize();
        }
    }

    private static void ensureConfigExists() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        if (Files.exists(CONFIG_PATH)) {
            return;
        }

        try (InputStream inputStream = SpellConfigStore.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (inputStream != null) {
                Files.copy(inputStream, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }

        save(SpellFile.defaults());
    }

    private static String buildGrammarJson(List<SpellDictionary.SpellPattern> patterns) {
        List<String> entries = new java.util.ArrayList<>();
        for (SpellDictionary.SpellPattern spell : patterns) {
            entries.addAll(spell.fullForms());
            for (List<String> sequence : spell.sequences()) {
                entries.addAll(sequence);
            }
            for (List<String> variants : spell.stepVariants().values()) {
                entries.addAll(variants);
            }
            entries.addAll(spell.candidate().startKeywords());
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            String value = entries.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
            builder.append('"').append(value).append('"');
        }
        builder.append(",\"[unk]\"]");
        return builder.toString();
    }

    private record State(SpellFile file, List<SpellDictionary.SpellPattern> patterns, String grammarJson, long lastModified) {
        private static State empty() {
            return new State(SpellFile.defaults(), List.of(), "[\"[unk]\"]", Long.MIN_VALUE);
        }
    }

    record SpellFile(List<SpellDefinition> spells) {
        SpellFile normalize() {
            List<SpellDefinition> normalized = spells == null ? List.of() : spells.stream()
                    .filter(definition -> definition != null && definition.id() != null && definition.displayName() != null)
                    .map(SpellDefinition::normalize)
                    .toList();
            return new SpellFile(normalized);
        }

        static SpellFile defaults() {
            return new SpellFile(List.of());
        }
    }

    record SpellDefinition(String id,
                           String displayName,
                           List<String> fullForms,
                           List<List<String>> sequences,
                           Map<String, List<String>> stepVariants,
                           List<String> startKeywords,
                           List<String> allowedGestures,
                           String particleStyle,
                           float hapticStrength) {
        SpellDefinition normalize() {
            return new SpellDefinition(
                    id,
                    displayName,
                    fullForms == null ? List.of() : fullForms,
                    sequences == null ? List.of() : sequences,
                    stepVariants == null ? Collections.emptyMap() : new LinkedHashMap<>(stepVariants),
                    startKeywords == null ? List.of() : startKeywords,
                    allowedGestures == null ? List.of("FORWARD", "SIDE", "UP") : allowedGestures,
                    particleStyle == null || particleStyle.isBlank() ? "generic" : particleStyle,
                    hapticStrength <= 0.0F ? 0.11F : hapticStrength
            );
        }
    }
}
