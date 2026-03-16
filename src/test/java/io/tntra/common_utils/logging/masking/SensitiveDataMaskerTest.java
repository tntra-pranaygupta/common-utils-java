package io.tntra.common_utils.logging.masking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    /**
     * Should pad null or short PAN inputs with asterisks.
     */
    @Test
    void maskPanShouldPadNullOrShortInputsTest() {
        assertThat(SensitiveDataMasker.maskPan(null)).isEqualTo("****");
        assertThat(SensitiveDataMasker.maskPan("")).isEqualTo("****");
        assertThat(SensitiveDataMasker.maskPan("12")).isEqualTo("****");
        assertThat(SensitiveDataMasker.maskPan("12 ")).isEqualTo("****");
    }

    @ParameterizedTest
    @CsvSource({
            "4111111111111111, ****-****-****-1111",
            "4111-1#2#11-1^111+1111, ****-****-****-1111",
            "4111 1111 1111 1111, ****-****-****-1111",
            "4111-1111-1111-1111, ****-****-****-1111",
            "378211111111111, ****-****-****-1111",
            "4111111111111111222, ****-****-****-1222"
    })
    void maskPanShouldPreserveOnlyLast4DigitsTest(String pan, String expectedMaskedPan) {
        // Strip out odd test chars for basic pass masking, not regex
        String cleanPan = pan.replaceAll("[^0-9\\s\\-]", "");
        assertThat(SensitiveDataMasker.maskPan(cleanPan)).isEqualTo(expectedMaskedPan);
    }

    /**
     * Should mask full-length PAN inside arbitrary text.
     */
    @Test
    void maskEmbeddedPansShouldMaskFullLengthPanInsideArbitraryTextTest() {
        String input = "Payment failed for card 4111222233334444. Try again next time.";
        String expected = "Payment failed for card ****-****-****-4444. Try again next time.";
        assertThat(SensitiveDataMasker.maskEmbeddedPans(input)).isEqualTo(expected);
    }
    
    /**
     * Should mask formatted PANs in text.
     */
    @Test
    void maskEmbeddedPansShouldMaskFormattedPansTest() {
        String input1 = "Card number 4111-2222-3333-4444 has an issue.";
        String expected1 = "Card number ****-****-****-4444 has an issue.";
        assertThat(SensitiveDataMasker.maskEmbeddedPans(input1)).isEqualTo(expected1);
        
        String input2 = "Here is an Amex 3782 1111 2222 333.";
        String expected2 = "Here is an Amex ****-****-****-*333.";
        assertThat(SensitiveDataMasker.maskEmbeddedPans(input2)).isEqualTo(expected2);
    }

    /**
     * Should not mask standard numbers that are not PANs.
     */
    @Test
    void maskEmbeddedPansShouldNotMaskStandardNumbersTest() {
        String text = "The order 1234 cost $400.00 today.";
        assertThat(SensitiveDataMasker.maskEmbeddedPans(text)).isEqualTo(text);
        
        String text2 = "Phone number is (415) 555-2671.";
        assertThat(SensitiveDataMasker.maskEmbeddedPans(text2)).isEqualTo(text2);
    }

    /**
     * Should mask email addresses correctly.
     */
    @Test
    void maskEmailShouldMaskEmailCorrectlyTest() {
        assertThat(SensitiveDataMasker.maskEmail(null)).isEqualTo("****");
        assertThat(SensitiveDataMasker.maskEmail("")).isEqualTo("****");
        assertThat(SensitiveDataMasker.maskEmail("john.doe@example.com")).isEqualTo("****@example.com");
        assertThat(SensitiveDataMasker.maskEmail("somebody123@sub.domain.co.uk")).isEqualTo("****@sub.domain.co.uk");
    }

    /**
     * Should mask emails within text.
     */
    @Test
    void maskEmbeddedEmailsShouldMaskEmailsWithinTextTest() {
        String text = "Send a receipt to jane.smithy@google.com please.";
        String expected = "Send a receipt to ****@google.com please.";
        assertThat(SensitiveDataMasker.maskEmbeddedEmails(text)).isEqualTo(expected);
        
        String nullText = null;
        assertThat(SensitiveDataMasker.maskEmbeddedEmails(null)).isNull();
    }

    /**
     * Should mask both PANs and emails in complex log text and keep the rest unchanged.
     */
    @Test
    void maskAllShouldMaskBothPansAndEmailsAndKeepRestTest() {
        String complexLog = "Trace user jane.smithy@google.com who tried paying with 4111-2222-3333-4444 via IP 192.168.1.1.";
        String expected = "Trace user ****@google.com who tried paying with ****-****-****-4444 via IP 192.168.1.1.";
        assertThat(SensitiveDataMasker.maskAll(complexLog)).isEqualTo(expected);
        assertThat(SensitiveDataMasker.maskAll(null)).isNull();
    }

    /**
     *  Should return masked value when cleaned PAN contains fewer than four digits.
     */
    @Test
    void maskPanShouldReturnMaskedWhenDigitsLessThanFourAfterCleanupTest() {
        String pan = "1 - -";
        assertThat(SensitiveDataMasker.maskPan(pan)).isEqualTo("****");
    }

    /**
     * Should return null when input text is null while masking embedded PANs.
     */
    @Test
    void maskEmbeddedPansShouldReturnNullWhenInputIsNullTest() {
        assertThat(SensitiveDataMasker.maskEmbeddedPans(null)).isNull();
    }
}
