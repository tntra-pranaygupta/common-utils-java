package io.tntra.common_utils.db_utilities.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseRepositoryTest {
    /**
     * Should verify that BaseRepository extends JpaRepository.
     */
    @Test
    void baseRepositoryShouldExtendJpaRepositoryTest() {

        Class<?>[] interfaces = RepoTestRepository.class.getInterfaces();

        boolean found = false;

        for (Class<?> i : interfaces) {
            if (i.equals(BaseRepository.class)) {
                found = true;
            }
        }

        assertThat(found).isTrue();
    }
}
