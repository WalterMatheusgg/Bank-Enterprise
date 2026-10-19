package dev.nexusbank.deposit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/deposits")
public class DepositController {
    private final DepositService service;
    private final DepositRepository repository;

    public DepositController(DepositService service, DepositRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    public record CreditRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2)
                                BigDecimal amount) { }

    public record DepositView(UUID id, UUID accountId, BigDecimal amount, Instant createdAt) {
        static DepositView of(BankDeposit deposit) {
            return new DepositView(deposit.id, deposit.accountId, deposit.amount, deposit.createdAt);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepositView credit(@PathVariable UUID accountId,
                              @RequestHeader("Idempotency-Key") String key,
                              @Valid @RequestBody CreditRequest request) {
        return DepositView.of(service.credit(key, accountId, request.amount()));
    }

    @GetMapping
    public List<DepositView> history(@PathVariable UUID accountId) {
        return repository.findTop100ByAccountIdOrderByCreatedAtDesc(accountId)
            .stream().map(DepositView::of).toList();
    }
}
