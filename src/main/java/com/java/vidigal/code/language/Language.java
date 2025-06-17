package com.java.vidigal.code.language;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration of languages supported by DeepL, with ISO language codes.
 */
public enum Language {
    ENGLISH("EN"),
    ALBANIAN("SQ"),
    ARABIC("AR"),
    AZERBAIJANI("AZ"),
    RUSSIAN("RU"),
    CATALAN("CA"),
    CHINESE("ZH"),
    CZECH("CS"),
    DANISH("DA"),
    DUTCH("NL"),
    ESPERANTO("EO"),
    FINNISH("FI"),
    FRENCH("FR"),
    GERMAN("DE"),
    GREEK("EL"),
    HEBREW("HE"),
    HINDI("HI"),
    HUNGARIAN("HU"),
    INDONESIAN("ID"),
    IRISH("GA"),
    ITALIAN("IT"),
    JAPANESE("JA"),
    KOREAN("KO"),
    PERSIAN("FA"),
    POLISH("PL"),
    PORTUGUESE("PT"),
    SLOVAK("SK"),
    SPANISH("ES"),
    SWEDISH("SV"),
    TURKISH("TR"),
    UKRAINIAN("UK"),
    AUTO("AUTO");

    private static final Map<String, Language> CODE_TO_LANGUAGE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(lang -> lang.getCode().toLowerCase(), Function.identity()));
    private final String code;

    Language(String code) {
        this.code = code;
    }

    /**
     * Finds a Language by its code, case-insensitively.
     *
     * @param code The language code (e.g., "EN", "fr").
     * @return An Optional containing the Language, or empty if not found.
     */
    public static java.util.Optional<Language> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(CODE_TO_LANGUAGE_MAP.get(code.toLowerCase()));
    }

    /**
     * Returns the ISO language code.
     *
     * @return The language code string.
     */
    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
