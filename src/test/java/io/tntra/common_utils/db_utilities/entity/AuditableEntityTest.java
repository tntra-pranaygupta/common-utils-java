package io.tntra.common_utils.db_utilities.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditableEntityTest {
    /**
     * Should set and get audit fields correctly in the entity.
     */
    @Test
    void shouldSetAndGetAuditFieldsTest() {
        TestEntity entity = new TestEntity();
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy("tester");
        entity.setUpdatedBy("tester2");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getCreatedBy()).isEqualTo("tester");
        assertThat(entity.getUpdatedBy()).isEqualTo("tester2");
    }
}
