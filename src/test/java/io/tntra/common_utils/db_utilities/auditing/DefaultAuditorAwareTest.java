package io.tntra.common_utils.db_utilities.auditing;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class DefaultAuditorAwareTest {
    private final DefaultAuditorAware auditorAware = new DefaultAuditorAware();

    /**
     * Should return 'system' as auditor when security context is not available.
     */
    @Test
    void shouldReturnSystemWhenSecurityContextNotAvailableTest() {
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo("system");
    }

    /**
     * Should return 'system' when authentication is null.
     */
    @Test
    void shouldReturnSystemWhenAuthenticationIsNullTest() {

        SecurityContext context = mock(SecurityContext.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {

            holder.when(SecurityContextHolder::getContext).thenReturn(context);
            when(context.getAuthentication()).thenReturn(null);

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            assertThat(auditor).contains("system");
        }
    }

    /**
     * Should return 'system' when authentication is not authenticated.
     */
    @Test
    void shouldReturnSystemWhenUserNotAuthenticatedTest() {

        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {

            holder.when(SecurityContextHolder::getContext).thenReturn(context);
            when(context.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            assertThat(auditor).contains("system");
        }
    }

    /**
     * Should return 'system' when username is blank.
     */
    @Test
    void shouldReturnSystemWhenUsernameIsBlankTest() {

        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {

            holder.when(SecurityContextHolder::getContext).thenReturn(context);
            when(context.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("");

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            assertThat(auditor).contains("system");
        }
    }

    /**
     * Should return authenticated username.
     */
    @Test
    void shouldReturnAuthenticatedUsernameTest() {

        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {

            holder.when(SecurityContextHolder::getContext).thenReturn(context);
            when(context.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("pranay");

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            assertThat(auditor).contains("pranay");
        }
    }

    /**
     * Should return 'system' when runtime exception occurs.
     */
    @Test
    void shouldReturnSystemWhenRuntimeExceptionOccursTest() {

        try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {

            holder.when(SecurityContextHolder::getContext)
                    .thenThrow(new RuntimeException("Failure"));

            Optional<String> auditor = auditorAware.getCurrentAuditor();

            assertThat(auditor).contains("system");
        }
    }
}
