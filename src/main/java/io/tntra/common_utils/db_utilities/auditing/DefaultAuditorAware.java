package io.tntra.common_utils.db_utilities.auditing;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Default auditor resolution strategy.
 *
 * <p>If Spring Security is on the classpath and a user is authenticated, uses the
 * authenticated principal name; otherwise falls back to {@code "system"}.</p>
 */
public class DefaultAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return resolveFromSpringSecurity()
                .or(() -> Optional.of("system"));
    }

    private Optional<String> resolveFromSpringSecurity() {
        try {

            Class<?> holderClass = Class.forName(
                    "org.springframework.security.core.context.SecurityContextHolder"
            );

            Object context = holderClass
                    .getMethod("getContext")
                    .invoke(null);

            Object authentication = context.getClass()
                    .getMethod("getAuthentication")
                    .invoke(context);

            if (authentication == null) {
                return Optional.empty();
            }

            Object isAuthenticated = authentication.getClass()
                    .getMethod("isAuthenticated")
                    .invoke(authentication);

            if (!(isAuthenticated instanceof Boolean authenticated) || !authenticated) {
                return Optional.empty();
            }

            Object nameObj = authentication.getClass()
                    .getMethod("getName")
                    .invoke(authentication);

            if (!(nameObj instanceof String name) || name.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(name);

        } catch (ClassNotFoundException ex) {
            return Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return Optional.empty();
        }
    }
}