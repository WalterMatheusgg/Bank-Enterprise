package dev.nexusbank.ledger;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="ledger_entries") public class LedgerEntry {
 @Id public UUID id;
 @Column(name="transfer_id",nullable=false) public UUID transferId;
 @Column(name="account_id",nullable=false) public UUID accountId;
 @Column(name="entry_type",nullable=false) public String entryType;
 @Column(nullable=false,precision=19,scale=2) public BigDecimal amount;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected LedgerEntry(){}
 public LedgerEntry(UUID transferId,UUID accountId,String type,BigDecimal amount){id=UUID.randomUUID();this.transferId=transferId;this.accountId=accountId;entryType=type;this.amount=amount;createdAt=Instant.now();}
}
