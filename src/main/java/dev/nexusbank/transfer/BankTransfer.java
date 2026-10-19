package dev.nexusbank.transfer;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="bank_transfers") public class BankTransfer {
 @Id public UUID id;
 @Column(name="idempotency_key",nullable=false,unique=true) public String idempotencyKey;
 @Column(name="from_account_id",nullable=false) public UUID fromAccountId;
 @Column(name="to_account_id",nullable=false) public UUID toAccountId;
 @Column(nullable=false,precision=19,scale=2) public BigDecimal amount;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected BankTransfer(){}
 public BankTransfer(String key,UUID from,UUID to,BigDecimal amount){id=UUID.randomUUID();idempotencyKey=key;fromAccountId=from;toAccountId=to;this.amount=amount;createdAt=Instant.now();}
}
