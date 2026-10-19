package dev.nexusbank.ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface LedgerRepository extends JpaRepository<LedgerEntry,UUID>{List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID id);}
