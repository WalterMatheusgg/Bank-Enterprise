package dev.nexusbank.transfer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/transfers") public class TransferController {
 private final TransferService service;private final TransferRepository repo;
 public TransferController(TransferService service,TransferRepository repo){this.service=service;this.repo=repo;}
 public record TransferRequest(@NotNull UUID fromAccountId,@NotNull UUID toAccountId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount){}
 public record TransferView(UUID id,UUID fromAccountId,UUID toAccountId,BigDecimal amount,Instant createdAt){
  static TransferView of(BankTransfer t){return new TransferView(t.id,t.fromAccountId,t.toAccountId,t.amount,t.createdAt);}
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public TransferView create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody TransferRequest req){
  return TransferView.of(service.execute(key,req.fromAccountId(),req.toAccountId(),req.amount()));
 }
 @GetMapping("/{id}") public TransferView get(@PathVariable UUID id){return TransferView.of(repo.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,"Transfer not found")));}
}
