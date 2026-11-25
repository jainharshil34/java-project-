package com.civicheat.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TextSimilarityUtilsTest {

    @Test
    @DisplayName("Normalize text strips punctuation, lowers case and trims whitespace")
    void normalizeText_standardBehavior() {
        String input = "  Huge Pot-Hole on 5th Avenue, dangerous!!!  ";
        String expected = "huge pot hole on 5th avenue dangerous";

        assertThat(TextSimilarityUtils.normalizeText(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Tokenize extracts unique word tokens")
    void tokenize_extractsTokens() {
        String input = "Garbage dump dump near the street light street";
        Set<String> tokens = TextSimilarityUtils.tokenize(input);

        assertThat(tokens).containsExactlyInAnyOrder("garbage", "dump", "near", "the", "street", "light");
    }

    @Test
    @DisplayName("Jaccard similarity on identical strings is 1.0")
    void jaccardSimilarity_identicalStrings_returnsOne() {
        String t1 = "Broken streetlight on Main Street";
        String t2 = "broken streetlight on main street";

        assertThat(TextSimilarityUtils.jaccardSimilarity(t1, t2)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Jaccard similarity on completely disjoint strings is 0.0")
    void jaccardSimilarity_disjointStrings_returnsZero() {
        String t1 = "Garbage pile in front of park";
        String t2 = "Water leakage overflowing sewer";

        assertThat(TextSimilarityUtils.jaccardSimilarity(t1, t2)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Jaccard similarity on partially overlapping strings")
    void jaccardSimilarity_partialOverlap() {
        // t1 tokens: {pothole, on, road} (size 3)
        // t2 tokens: {huge, pothole, on, road} (size 4)
        // intersection: {pothole, on, road} (size 3)
        // union: {huge, pothole, on, road} (size 4)
        // Jaccard = 3/4 = 0.75
        String t1 = "Pothole on road";
        String t2 = "Huge pothole on road";

        double similarity = TextSimilarityUtils.jaccardSimilarity(t1, t2);
        assertThat(similarity).isCloseTo(0.75, within(0.001));
    }

    @ParameterizedTest
    @CsvSource({
            "'', '', 1.0",
            "'  ', '', 1.0",
            "'some text', '', 0.0",
            "'', 'some text', 0.0"
    })
    @DisplayName("Jaccard similarity edge cases with empty and blank strings")
    void jaccardSimilarity_emptyStrings(String t1, String t2, double expected) {
        assertThat(TextSimilarityUtils.jaccardSimilarity(t1, t2)).isEqualTo(expected);
    }
}
