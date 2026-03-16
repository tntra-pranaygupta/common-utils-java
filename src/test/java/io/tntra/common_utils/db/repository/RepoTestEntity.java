package io.tntra.common_utils.db.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RepoTestEntity {
    @Id
    private Long id;
}
