package dev.nexusbank.ledger;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findTop100ByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
