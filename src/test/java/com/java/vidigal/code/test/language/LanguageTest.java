package com.java.vidigal.code.test.language;

import com.java.vidigal.code.language.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Language} enum, verifying the correctness of predefined language constants
 * and their associated methods. Tests cover case-insensitive language code lookup using
 * {@link Language#fromCode}, handling of invalid, null, or blank codes, and retrieval of language codes
 * via {@link Language#getCode}. Uses JUnit parameterized tests for comprehensive validation.
 */
class LanguageTest {

    /**
     * Tests that {@link Language#fromCode} returns the correct {@link Language} enum constant for valid
     * language codes, regardless of case. Verifies that the returned {@link Optional} contains the
     * expected language.
     *
     * @param code             The language code to test (e.g., "EN", "fr").
     * @param expectedLanguage The expected {@link Language} enum constant (e.g., {@link Language#ENGLISH}).
     */
    @ParameterizedTest
    @CsvSource({
            "EN, ENGLISH",
            "en, ENGLISH",
            "FR, FRENCH",
            "fr, FRENCH",
            "DE, GERMAN",
            "de, GERMAN",
            "AUTO, AUTO"
    })
    @DisplayName("fromCode should find correct enum constant for valid codes (case-insensitive)")
    void fromCode_shouldFindCorrectEnum_forValidCodes(String code, Language expectedLanguage) {
        Optional<Language> result = Language.fromCode(code);
        assertTrue(result.isPresent(), "Expected language to be found for code: " + code);
        assertEquals(expectedLanguage, result.get(), "Expected language to match for code: " + code);
    }

    /**
     * Tests that {@link Language#fromCode} returns an empty {@link Optional} for invalid language codes.
     * Verifies that unrecognized codes do not map to any {@link Language} enum constant.
     *
     * @param invalidCode The invalid language code to test (e.g., "XX", "INVALID").
     */
    @ParameterizedTest
    @ValueSource(strings = {"XX", "INVALID", "123"})
    @DisplayName("fromCode should return empty Optional for invalid codes")
    void fromCode_shouldReturnEmpty_forInvalidCodes(String invalidCode) {
        Optional<Language> result = Language.fromCode(invalidCode);
        assertTrue(result.isEmpty(), "Expected Optional to be empty for invalid code: " + invalidCode);
    }

    /**
     * Tests that {@link Language#fromCode} returns an empty {@link Optional} for null or blank input.
     * Verifies that the method handles edge cases correctly by rejecting invalid inputs.
     */
    @Test
    @DisplayName("fromCode should return empty Optional for null or blank input")
    void fromCode_shouldReturnEmpty_forNullOrBlankInput() {
        assertTrue(Language.fromCode(null).isEmpty(), "Expected Optional to be empty for null input");
        assertTrue(Language.fromCode("").isEmpty(), "Expected Optional to be empty for empty string");
        assertTrue(Language.fromCode("  ").isEmpty(), "Expected Optional to be empty for blank string");
    }

    /**
     * Tests that {@link Language#getCode} returns the correct language code for each
     * {@link Language} enum constant. Verifies that the code matches the expected string
     * representation.
     */
    @Test
    @DisplayName("getCode should return the correct string code")
    void getCode_shouldReturnCorrectCode() {
        assertEquals("EN", Language.ENGLISH.getCode(), "Expected code 'EN' for ENGLISH");
        assertEquals("FR", Language.FRENCH.getCode(), "Expected code 'FR' for FRENCH");
        assertEquals("AUTO", Language.AUTO.getCode(), "Expected code 'AUTO' for AUTO");
    }
}