package com.civicheat.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility functions for text normalization, tokenization, and Jaccard set similarity.
 */
public final class TextSimilarityUtils {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private TextSimilarityUtils() {
        // Utility class
    }

    /**
     * Normalizes text by converting to lowercase, stripping punctuation, and collapsing whitespace.
     *
     * @param text Input raw text
     * @return Normalized alphanumeric string
     */
    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String stripped = NON_ALPHANUMERIC.matcher(lower).replaceAll(" ");
        return MULTI_SPACE.matcher(stripped).replaceAll(" ").trim();
    }

    /**
     * Extracts a unique set of word tokens from raw text after normalization.
     *
     * @param text Input text
     * @return Set of unique word tokens
     */
    public static Set<String> tokenize(String text) {
        String normalized = normalizeText(text);
        if (normalized.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Computes the Jaccard similarity coefficient between two strings based on their token sets.
     * Jaccard index J(A, B) = |A ∩ B| / |A ∪ B|.
     *
     * @param text1 First text string
     * @param text2 Second text string
     * @return Similarity score between 0.0 (disjoint) and 1.0 (identical)
     */
    public static double jaccardSimilarity(String text1, String text2) {
        Set<String> set1 = tokenize(text1);
        Set<String> set2 = tokenize(text2);

        if (set1.isEmpty() && set2.isEmpty()) {
            return 1.0;
        }
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 1.0;
        }

        return (double) intersection.size() / (double) union.size();
    }
}
