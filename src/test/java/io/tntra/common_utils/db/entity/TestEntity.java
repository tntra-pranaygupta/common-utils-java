package io.tntra.common_utils.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestEntity extends AuditableEntity{
    @Id
    private Long id;
}
