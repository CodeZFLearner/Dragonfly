package com.zff.dismantle.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration context for title generation.
 *
 * @see TitleGenerator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitleContext {

    /**
     * Maximum length of the generated title.
     */
    @Builder.Default
    private int maxLength = 60;

    /**
     * Minimum length of the generated title.
     */
    @Builder.Default
    private int minLength = 5;

    /**
     * Preferred language code (e.g., "en", "zh").
     */
    private String language;

    /**
     * Whether to include ellipsis for truncated titles.
     */
    @Builder.Default
    private boolean truncateWithEllipsis = true;

    /**
     * Whether to preserve original casing.
     */
    @Builder.Default
    private boolean preserveCasing = true;

    /**
     * Keywords to prioritize in the title.
     */
    private String[] keywords;

    /**
     * Creates a default context.
     *
     * @return default TitleContext
     */
    public static TitleContext defaults() {
        return TitleContext.builder().build();
    }

    /**
     * Creates a context for short titles (e.g., UI display).
     *
     * @return TitleContext with maxLength=40
     */
    public static TitleContext forShortTitle() {
        return TitleContext.builder()
                .maxLength(40)
                .build();
    }

    /**
     * Creates a context for descriptive titles (e.g., summaries).
     *
     * @return TitleContext with maxLength=100
     */
    public static TitleContext forDescriptiveTitle() {
        return TitleContext.builder()
                .maxLength(100)
                .build();
    }
}
