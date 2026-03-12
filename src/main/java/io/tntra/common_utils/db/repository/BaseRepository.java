package io.tntra.common_utils.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository to be extended by all Spring Data JPA repositories in services.
 *
 * <p>Platform note: keeping this type in the shared module lets us attach
 * common behavior later (soft deletes, tenant scoping, etc.) without forcing
 * a migration across every service repository interface.</p>
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
}

