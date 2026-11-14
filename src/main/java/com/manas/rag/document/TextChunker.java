package com.manas.rag.document;

import java.util.ArrayList;
import java.util.List;

/**
 * Step 5: cuts a page of text into pieces of about {@code chunkWords} words.
 * Each piece repeats the last {@code overlapWords} words of the one before it, so a
 * sentence that falls on a boundary is still whole in at least one piece.
 */
public class TextChunker {

    private final int chunkWords;
    private final int overlapWords;

    public TextChunker(int chunkWords, int overlapWords) {
        if (chunkWords <= 0) {
            throw new IllegalArgumentException("chunkWords must be positive");
        }
        if (overlapWords < 0 || overlapWords >= chunkWords) {
            throw new IllegalArgumentException("overlapWords must be between 0 and chunkWords - 1");
        }
        this.chunkWords = chunkWords;
        this.overlapWords = overlapWords;
    }

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String[] words = text.trim().split("\\s+");
        int step = chunkWords - overlapWords;
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + chunkWords, words.length);
            chunks.add(String.join(" ", List.of(words).subList(start, end)));
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}
