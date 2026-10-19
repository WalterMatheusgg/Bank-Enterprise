package dev.nexusbank.account;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1/accounts") public class AccountController {
 private final BankAccountRepository repo;
 public AccountController(BankAccountRepository repo){this.repo=repo;}
 public record CreateAccount(@NotBlank String ownerName) {}
 public record AccountView(UUID id,String ownerName,String currency,BigDecimal balance){
   static AccountView of(BankAccount a){return new AccountView(a.id,a.ownerName,a.currency,a.balance);}
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public AccountView create(@Valid @RequestBody CreateAccount dto){
  var name=dto.ownerName().trim();if(name.length()>160)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Owner name too long");
  return AccountView.of(repo.save(new BankAccount(name,"BRL",BigDecimal.ZERO)));
 }
 @GetMapping("/{id}") public AccountView get(@PathVariable UUID id){return AccountView.of(repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found")));}
}
