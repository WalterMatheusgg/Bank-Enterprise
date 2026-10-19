package dev.nexusbank.deposit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositRepository extends JpaRepository<BankDeposit, UUID> {
    Optional<BankDeposit> findByIdempotencyKey(String key);
    List<BankDeposit> findTop100ByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
