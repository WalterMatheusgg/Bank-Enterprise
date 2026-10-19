package dev.nexusbank.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/ledger")
public class LedgerController {
    private final LedgerRepository repository;

    public LedgerController(LedgerRepository repository) {
        this.repository = repository;
    }

    public record EntryView(UUID id, UUID transferId, String entryType,
                            BigDecimal amount, Instant createdAt) {
        static EntryView of(LedgerEntry entry) {
            return new EntryView(entry.id, entry.transferId, entry.entryType, entry.amount, entry.createdAt);
        }
    }

    @GetMapping
    public List<EntryView> entries(@PathVariable UUID accountId) {
        return repository.findTop100ByAccountIdOrderByCreatedAtDesc(accountId)
            .stream().map(EntryView::of).toList();
    }
}
