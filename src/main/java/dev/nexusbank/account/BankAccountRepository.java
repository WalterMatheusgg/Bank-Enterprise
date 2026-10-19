package dev.nexusbank.account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
public interface BankAccountRepository extends JpaRepository<BankAccount,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from BankAccount a where a.id = :id") Optional<BankAccount> findLockedById(UUID id);
}
