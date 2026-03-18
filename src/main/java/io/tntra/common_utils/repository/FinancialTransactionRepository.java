package io.tntra.common_utils.repository;

import io.tntra.common_utils.db_utilities.repository.BaseRepository;
import io.tntra.common_utils.entity.FinancialTransaction;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialTransactionRepository extends BaseRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByExternalId(String externalId);
    List<FinancialTransaction> findByStatus(String status);
}
