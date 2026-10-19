package dev.nexusbank;

import dev.nexusbank.account.BankAccount;
import dev.nexusbank.account.BankAccountRepository;
import dev.nexusbank.deposit.DepositRepository;
import dev.nexusbank.deposit.DepositService;
import dev.nexusbank.ledger.LedgerRepository;
import dev.nexusbank.transfer.TransferService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankingWorkflowIntegrationTest {
    @Autowired BankAccountRepository accounts;
    @Autowired DepositRepository deposits;
    @Autowired DepositService credits;
    @Autowired TransferService transfers;
    @Autowired LedgerRepository ledger;
    @Autowired MockMvc mvc;

    @Test
    void depositIsAuditableIdempotentAndFundsAccount() {
        var account = accounts.save(new BankAccount("Deposit Test", "BRL", BigDecimal.ZERO));
        var key = "credit-" + UUID.randomUUID();
        var first = credits.credit(key, account.id, new BigDecimal("250.50"));
        var replay = credits.credit(key, account.id, new BigDecimal("250.50"));

        assertEquals(first.id, replay.id);
        assertEquals(0, accounts.findById(account.id).orElseThrow().balance.compareTo(new BigDecimal("250.50")));
        assertEquals(1, deposits.findAll().stream().filter(d -> d.idempotencyKey.equals(key)).count());
        assertThrows(ResponseStatusException.class,
            () -> credits.credit(key, account.id, new BigDecimal("250.51")));
    }

    @Test
    void realWorkflowCreditsThenTransfersAndRecordsDoubleEntry() {
        var source = accounts.save(new BankAccount("Source Test", "BRL", BigDecimal.ZERO));
        var target = accounts.save(new BankAccount("Target Test", "BRL", BigDecimal.ZERO));
        credits.credit("demo-credit-" + UUID.randomUUID(), source.id, new BigDecimal("100.00"));

        var transfer = transfers.execute("transfer-demo-" + UUID.randomUUID(),
            source.id, target.id, new BigDecimal("35.00"));

        assertEquals(0, accounts.findById(source.id).orElseThrow().balance.compareTo(new BigDecimal("65.00")));
        assertEquals(0, accounts.findById(target.id).orElseThrow().balance.compareTo(new BigDecimal("35.00")));
        var entries = ledger.findAll().stream().filter(e -> e.transferId.equals(transfer.id)).toList();
        assertEquals(2, entries.size());
        assertEquals(1, entries.stream().filter(e -> e.entryType.equals("DEBIT")).count());
        assertEquals(1, entries.stream().filter(e -> e.entryType.equals("CREDIT")).count());
    }

    @Test
    void publicDashboardAndProtectedApi() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/assets/app.js")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/accounts").with(httpBasic("admin", "dev-only-change-me")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void httpDepositValidatesRequestAndReplaysIdempotently() throws Exception {
        var account = accounts.save(new BankAccount("API Credit", "BRL", BigDecimal.ZERO));
        var key = "http-credit-" + UUID.randomUUID();
        var url = "/api/v1/accounts/" + account.id + "/deposits";
        var auth = httpBasic("admin", "dev-only-change-me");

        mvc.perform(post(url).with(auth).contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", key).content("{\"amount\":\"10.00\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").value(account.id.toString()));

        mvc.perform(post(url).with(httpBasic("admin", "dev-only-change-me"))
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", key)
            .content("{\"amount\":\"10.00\"}")).andExpect(status().isCreated());

        mvc.perform(post(url).with(httpBasic("admin", "dev-only-change-me"))
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", key)
            .content("{\"amount\":\"11.00\"}")).andExpect(status().isConflict());

        mvc.perform(post(url).with(httpBasic("admin", "dev-only-change-me"))
            .contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", "wrong-" + UUID.randomUUID())
            .content("{\"amount\":-5}")).andExpect(status().isBadRequest());

        mvc.perform(get(url).with(httpBasic("admin", "dev-only-change-me")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].accountId").value(account.id.toString()));
        assertEquals(0, accounts.findById(account.id).orElseThrow().balance.compareTo(new BigDecimal("10.00")));
    }
}
