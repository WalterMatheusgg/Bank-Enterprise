package dev.nexusbank.transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface TransferRepository extends JpaRepository<BankTransfer,UUID>{Optional<BankTransfer> findByIdempotencyKey(String key);}
