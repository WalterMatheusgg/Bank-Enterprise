package dev.nexusbank.transfer;
import dev.nexusbank.account.*;
import dev.nexusbank.ledger.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service public class TransferService {
 private final BankAccountRepository accounts;private final TransferRepository transfers;private final LedgerRepository ledger;
 public TransferService(BankAccountRepository accounts,TransferRepository transfers,LedgerRepository ledger){this.accounts=accounts;this.transfers=transfers;this.ledger=ledger;}
 @Transactional public BankTransfer execute(String key,UUID from,UUID to,BigDecimal amount){
  if(key==null||key.isBlank()||key.length()>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Idempotency-Key");
  if(from.equals(to))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Source and destination must differ");
  if(amount==null||amount.signum()<=0||amount.scale()>2||amount.precision()>19)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid amount");
  var existing=transfers.findByIdempotencyKey(key);
  if(existing.isPresent())return verifySameRequest(existing.get(),from,to,amount);
  // Consistent lock ordering avoids two opposite transfers deadlocking.
  UUID first=from.compareTo(to)<0?from:to; UUID second=first.equals(from)?to:from;
  BankAccount a=accounts.findLockedById(first).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found"));
  BankAccount b=accounts.findLockedById(second).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found"));
  // Recheck after locks: another transaction might have committed this key while waiting.
  existing=transfers.findByIdempotencyKey(key);
  if(existing.isPresent())return verifySameRequest(existing.get(),from,to,amount);
  BankAccount source=a.id.equals(from)?a:b;BankAccount target=a.id.equals(to)?a:b;
  if(!source.currency.equals(target.currency))throw new ResponseStatusException(HttpStatus.CONFLICT,"Different currencies");
  if(source.balance.compareTo(amount)<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient funds");
  source.balance=source.balance.subtract(amount);target.balance=target.balance.add(amount);
  var transfer=transfers.saveAndFlush(new BankTransfer(key,from,to,amount));
  ledger.save(new LedgerEntry(transfer.id,from,"DEBIT",amount));
  ledger.save(new LedgerEntry(transfer.id,to,"CREDIT",amount));
  return transfer;
 }
 private BankTransfer verifySameRequest(BankTransfer old,UUID from,UUID to,BigDecimal amount){
  if(!old.fromAccountId.equals(from)||!old.toAccountId.equals(to)||old.amount.compareTo(amount)!=0)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key already used for another request");
  return old;
 }
}
