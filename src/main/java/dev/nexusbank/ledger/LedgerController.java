package dev.nexusbank.ledger;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/accounts/{accountId}/ledger") public class LedgerController {
 private final LedgerRepository repository;
 public LedgerController(LedgerRepository repository){this.repository=repository;}
 public record EntryView(UUID id,UUID transferId,String entryType,BigDecimal amount,Instant createdAt){static EntryView of(LedgerEntry e){return new EntryView(e.id,e.transferId,e.entryType,e.amount,e.createdAt);}}
 @GetMapping public List<EntryView> entries(@PathVariable UUID accountId){return repository.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(EntryView::of).toList();}
}
