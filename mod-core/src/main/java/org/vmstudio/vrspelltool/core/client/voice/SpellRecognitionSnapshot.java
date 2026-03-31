package org.vmstudio.vrspelltool.core.client.voice;

public record SpellRecognitionSnapshot(String text, boolean partial, boolean spellCast, SpellCandidate candidate, double score) {
}
