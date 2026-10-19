package dev.nexusbank.deposit;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_deposits")
public class BankDeposit {
    @Id public UUID id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    public String idempotencyKey;
    @Column(name = "account_id", nullable = false)
    public UUID accountId;
    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    protected BankDeposit() { }

    public BankDeposit(String key, UUID accountId, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = key;
        this.accountId = accountId;
        this.amount = amount;
        this.createdAt = Instant.now();
    }
}
