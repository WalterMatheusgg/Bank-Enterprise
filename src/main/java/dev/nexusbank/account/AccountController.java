package dev.nexusbank.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final BankAccountRepository repository;

    public AccountController(BankAccountRepository repository) {
        this.repository = repository;
    }

    public record CreateAccount(@NotBlank @Size(max = 160) String ownerName) { }

    public record AccountView(UUID id, String ownerName, String currency, BigDecimal balance) {
        static AccountView of(BankAccount a) {
            return new AccountView(a.id, a.ownerName, a.currency, a.balance);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@Valid @RequestBody CreateAccount request) {
        return AccountView.of(repository.save(
            new BankAccount(request.ownerName().trim(), "BRL", BigDecimal.ZERO)));
    }

    @GetMapping
    public Page<AccountView> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination: size 1-100");
        }
        return repository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
            .map(AccountView::of);
    }

    @GetMapping("/{id}")
    public AccountView get(@PathVariable UUID id) {
        return AccountView.of(repository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")));
    }
}
