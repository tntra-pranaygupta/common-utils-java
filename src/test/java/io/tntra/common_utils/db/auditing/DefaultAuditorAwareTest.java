package io.tntra.common_utils.db.auditing;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DefaultAuditorAwareTest {
    private final DefaultAuditorAware auditorAware = new DefaultAuditorAware();

    @Test
    void shouldReturnSystemWhenSecurityContextNotAvailable() {
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("system");
    }
}
