package dev.nexusbank;
import dev.nexusbank.account.*;
import dev.nexusbank.transfer.*;
import dev.nexusbank.ledger.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest class BankIntegrationTest {
 @Autowired BankAccountRepository accounts;
 @Autowired TransferService service;
 @Autowired LedgerRepository ledger;
 @Test void transferMovesMoneyAndCreatesBalancedEntries(){
  var a=accounts.save(new BankAccount("Alice","BRL",new BigDecimal("100.00")));
  var b=accounts.save(new BankAccount("Bob","BRL",BigDecimal.ZERO));
  var t=service.execute("transfer-test-01",a.id,b.id,new BigDecimal("35.00"));
  assertEquals(new BigDecimal("65.00"),accounts.findById(a.id).orElseThrow().balance);
  assertEquals(new BigDecimal("35.00"),accounts.findById(b.id).orElseThrow().balance);
  assertEquals(2,ledger.findAll().stream().filter(e->e.transferId.equals(t.id)).count());
  assertEquals(t.id,service.execute("transfer-test-01",a.id,b.id,new BigDecimal("35.00")).id);
  assertEquals(new BigDecimal("65.00"),accounts.findById(a.id).orElseThrow().balance);
 }
 @Test void rejectsOverdraft(){
  var a=accounts.save(new BankAccount("Carol","BRL",BigDecimal.ZERO));
  var b=accounts.save(new BankAccount("Dave","BRL",BigDecimal.ZERO));
  assertThrows(ResponseStatusException.class,()->service.execute("transfer-overdraft",a.id,b.id,new BigDecimal("1.00")));
 }
}
