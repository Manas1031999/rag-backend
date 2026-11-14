package com.manas.rag.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class TextChunkerTest {

    private static String words(int from, int toExclusive) {
        return IntStream.range(from, toExclusive).mapToObj(i -> "w" + i).collect(Collectors.joining(" "));
    }

    @Test
    void blankTextGivesNoChunks() {
        TextChunker chunker = new TextChunker(10, 2);
        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("   \n ")).isEmpty();
    }

    @Test
    void shortTextIsOneChunkWithWhitespaceCollapsed() {
        assertThat(new TextChunker(10, 2).chunk("  hello \n\n world  ")).containsExactly("hello world");
    }

    @Test
    void chunksOverlapAndCoverEveryWord() {
        List<String> chunks = new TextChunker(10, 2).chunk(words(0, 25));

        assertThat(chunks).containsExactly(words(0, 10), words(8, 18), words(16, 25));
    }

    @Test
    void textEndingExactlyOnAChunkBoundaryHasNoTrailingDuplicate() {
        assertThat(new TextChunker(10, 2).chunk(words(0, 10))).containsExactly(words(0, 10));
        assertThat(new TextChunker(10, 2).chunk(words(0, 18))).containsExactly(words(0, 10), words(8, 18));
    }

    @Test
    void rejectsOverlapThatWouldNeverAdvance() {
        assertThatThrownBy(() -> new TextChunker(10, 10)).isInstanceOf(IllegalArgumentException.class);
    }
}
