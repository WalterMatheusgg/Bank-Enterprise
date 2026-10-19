package dev.nexusbank.account;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="bank_accounts") public class BankAccount {
 @Id public UUID id;
 @Column(name="owner_name",nullable=false) public String ownerName;
 @Column(nullable=false,length=3) public String currency;
 @Column(nullable=false,precision=19,scale=2) public BigDecimal balance;
 @Version public long version;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected BankAccount(){}
 public BankAccount(String ownerName,String currency,BigDecimal openingBalance){
  this.id=UUID.randomUUID();this.ownerName=ownerName;this.currency=currency;this.balance=openingBalance;this.createdAt=Instant.now();
 }
}
