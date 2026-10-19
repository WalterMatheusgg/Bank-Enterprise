package dev.nexusbank.deposit;

import dev.nexusbank.account.BankAccountRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepositService {
    private final BankAccountRepository accounts;
    private final DepositRepository deposits;

    public DepositService(BankAccountRepository accounts, DepositRepository deposits) {
        this.accounts = accounts;
        this.deposits = deposits;
    }

    @Transactional
    public BankDeposit credit(String key, UUID accountId, BigDecimal amount) {
        if (key == null || key.isBlank() || key.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() > 19) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        var existing = deposits.findByIdempotencyKey(key);
        if (existing.isPresent()) return verifySame(existing.get(), accountId, amount);

        // Lock serializes concurrent credits to this account, then recheck for replay.
        var account = accounts.findLockedById(accountId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        existing = deposits.findByIdempotencyKey(key);
        if (existing.isPresent()) return verifySame(existing.get(), accountId, amount);

        account.balance = account.balance.add(amount);
        if (account.balance.precision() > 19 || account.balance.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account balance limit exceeded");
        }
        return deposits.saveAndFlush(new BankDeposit(key, accountId, amount));
    }

    private BankDeposit verifySame(BankDeposit previous, UUID accountId, BigDecimal amount) {
        if (!previous.accountId.equals(accountId) || previous.amount.compareTo(amount) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used for another deposit");
        }
        return previous;
    }
}
