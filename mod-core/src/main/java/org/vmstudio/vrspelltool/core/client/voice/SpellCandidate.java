package org.vmstudio.vrspelltool.core.client.voice;

import java.util.List;
import java.util.Set;

public record SpellCandidate(String id,
                             String displayName,
                             Set<String> phrases,
                             Set<String> grammarPhrases,
                             List<String> startKeywords,
                             Set<String> allowedGestures,
                             String particleStyle,
                             float hapticStrength) {
}
