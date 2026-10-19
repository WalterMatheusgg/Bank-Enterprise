"use strict";

(() => {
  const $ = (id) => document.getElementById(id);
  const currency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
  const dateTime = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
  const state = { authorization: null, accounts: [], selected: null, mode: null, pendingKey: null };
  const loginScreen = $("login-screen");
  const app = $("app");
  const dialog = $("operation-dialog");
  const form = $("operation-form");
  const fields = $("dialog-fields");
  const toastEl = $("toast");
  let toastTimer;

  function basicHeader(username, password) {
    const bytes = new TextEncoder().encode(username + ":" + password);
    return "Basic " + btoa(Array.from(bytes, (v) => String.fromCharCode(v)).join(""));
  }

  async function api(path, init = {}) {
    if (!state.authorization) throw new Error("Faça login para continuar.");
    const response = await fetch(path, {
      ...init,
      cache: "no-store",
      credentials: "omit",
      headers: {
        Authorization: state.authorization,
        Accept: "application/json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...(init.headers || {})
      }
    });
    if (!response.ok) {
      if (response.status === 401) throw new Error("Credenciais inválidas ou sessão expirada.");
      const body = await response.json().catch(() => null);
      const messages = {
        400: "Confira os dados enviados.",
        403: "Operação não autorizada.",
        404: "Registro não encontrado.",
        409: "Operação não concluída: saldo, limite ou chave de idempotência em conflito."
      };
      throw new Error(body?.error || messages[response.status] || "Não foi possível concluir a operação.");
    }
    return response.status === 204 ? null : response.json();
  }

  function errorMessage(el, message) {
    el.textContent = message || "";
    el.hidden = !message;
  }

  function toast(message, error = false) {
    clearTimeout(toastTimer);
    toastEl.textContent = message;
    toastEl.classList.toggle("error", error);
    toastEl.hidden = false;
    toastTimer = setTimeout(() => { toastEl.hidden = true; }, 4500);
  }

  function money(value) {
    return currency.format(Number(value ?? 0));
  }

  function shortId(id) {
    return id ? id.slice(0, 8).toUpperCase() + "…" : "—";
  }

  function makeNode(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  }

  async function loadAccounts() {
    const response = await api("/api/v1/accounts?page=0&size=50");
    state.accounts = response.content || [];
    if (!state.accounts.some((a) => a.id === state.selected)) {
      state.selected = state.accounts[0]?.id || null;
    }
    $("stat-accounts").textContent = Number(response.totalElements || 0).toLocaleString("pt-BR");
    $("stat-balance").textContent = money(
      state.accounts.reduce((sum, account) => sum + Number(account.balance), 0)
    );
    $("accounts-total").textContent = String(response.totalElements || 0) + " conta(s) cadastrada(s)";
    renderAccounts();
    fillHistorySelect();
    await loadHistory();
  }

  function renderAccounts() {
    const body = $("accounts-body");
    body.replaceChildren();
    if (state.accounts.length === 0) {
      const row = makeNode("tr");
      const cell = makeNode("td", "empty-row", "Nenhuma conta cadastrada. Crie sua primeira conta para começar.");
      cell.colSpan = 5;
      row.append(cell);
      body.append(row);
      return;
    }
    for (const account of state.accounts) {
      const row = makeNode("tr");
      const owner = makeNode("td", "owner-cell");
      owner.append(makeNode("span", "account-icon", "▤"), document.createTextNode(account.ownerName));
      row.append(
        owner,
        makeNode("td", "mono", shortId(account.id)),
        makeNode("td", "", account.currency),
        makeNode("td", "balance-cell", money(account.balance))
      );
      const last = makeNode("td");
      const button = makeNode("button", "row-button", "Ver histórico");
      button.type = "button";
      button.addEventListener("click", async () => {
        state.selected = account.id;
        $("history-account").value = account.id;
        try {
          await loadHistory();
          $("movements").scrollIntoView({ behavior: "smooth" });
        } catch (e) { toast(e.message, true); }
      });
      last.append(button);
      row.append(last);
      body.append(row);
    }
  }

  function fillHistorySelect() {
    const select = $("history-account");
    select.replaceChildren();
    if (!state.accounts.length) {
      select.add(new Option("Nenhuma conta", ""));
      select.disabled = true;
      return;
    }
    select.disabled = false;
    for (const account of state.accounts) {
      select.add(new Option(account.ownerName + " · " + shortId(account.id), account.id));
    }
    select.value = state.selected;
  }

  async function loadHistory() {
    const list = $("activity-list");
    list.replaceChildren();
    if (!state.selected) {
      $("stat-transfers").textContent = "0";
      list.append(makeNode("div", "activity-empty", "Crie uma conta para começar a registrar operações."));
      return;
    }
    const id = encodeURIComponent(state.selected);
    const [ledger, deposits] = await Promise.all([
      api("/api/v1/accounts/" + id + "/ledger"),
      api("/api/v1/accounts/" + id + "/deposits")
    ]);
    $("stat-transfers").textContent = String(new Set(ledger.map((e) => e.transferId)).size);
    const entries = [
      ...ledger.map((entry) => ({ ...entry, kind: entry.entryType, label: entry.entryType === "CREDIT" ? "Transferência recebida" : "Transferência enviada" })),
      ...deposits.map((entry) => ({ ...entry, kind: "DEPOSIT", label: "Crédito demonstrativo" }))
    ].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 12);

    if (!entries.length) {
      list.append(makeNode("div", "activity-empty", "Ainda não há movimentações nesta conta."));
      return;
    }
    for (const entry of entries) {
      const positive = entry.kind !== "DEBIT";
      const row = makeNode("div", "activity-row");
      row.append(makeNode("span", "activity-indicator" + (positive ? " in" : ""), positive ? "↓" : "↑"));
      const description = makeNode("div", "description");
      description.append(makeNode("strong", "", entry.label));
      const when = Number.isNaN(Date.parse(entry.createdAt)) ? "Data indisponível" : dateTime.format(new Date(entry.createdAt));
      description.append(makeNode("small", "", when));
      row.append(description, makeNode("span", "activity-amount" + (positive ? " in" : ""),
        (positive ? "+ " : "− ") + money(entry.amount)));
      list.append(row);
    }
  }

  function textField(labelText, name, placeholder, type = "text") {
    const label = makeNode("label", "", labelText);
    label.htmlFor = "field-" + name;
    const input = document.createElement("input");
    input.id = label.htmlFor;
    input.name = name;
    input.type = type;
    input.placeholder = placeholder;
    input.required = true;
    if (type === "text") input.maxLength = name === "ownerName" ? 160 : 40;
    return [label, input];
  }

  function accountSelect(labelText, name) {
    const label = makeNode("label", "", labelText);
    label.htmlFor = "field-" + name;
    const select = document.createElement("select");
    select.id = label.htmlFor;
    select.name = name;
    select.required = true;
    for (const item of state.accounts) {
      select.add(new Option(item.ownerName + " (" + shortId(item.id) + ")", item.id));
    }
    return [label, select];
  }

  function openDialog(mode) {
    if (mode !== "account" && state.accounts.length < (mode === "transfer" ? 2 : 1)) {
      return toast(mode === "transfer" ? "Crie pelo menos duas contas para transferir." : "Crie uma conta primeiro.", true);
    }
    state.mode = mode;
    state.pendingKey = null;
    form.reset();
    fields.replaceChildren();
    errorMessage($("dialog-error"), null);
    const titles = { account: "Criar conta", deposit: "Adicionar crédito de teste", transfer: "Nova transferência" };
    const descriptions = {
      account: "Abra uma nova conta com saldo inicial de R$ 0,00.",
      deposit: "Adicione saldo fictício. A operação fica registrada para auditoria e não movimenta dinheiro real.",
      transfer: "Transfira saldo entre duas contas. O backend registra débito e crédito na mesma transação."
    };
    $("dialog-title").textContent = titles[mode];
    $("dialog-description").textContent = descriptions[mode];
    if (mode === "account") fields.append(...textField("Nome do titular", "ownerName", "Ex.: Empresa Exemplo Ltda."));
    if (mode === "deposit") {
      fields.append(...accountSelect("Conta de destino", "accountId"));
      fields.append(...textField("Valor em R$", "amount", "Ex.: 250.00"));
    }
    if (mode === "transfer") {
      fields.append(...accountSelect("Conta de origem", "fromAccountId"));
      fields.append(...accountSelect("Conta de destino", "toAccountId"));
      fields.append(...textField("Valor em R$", "amount", "Ex.: 80.00"));
      const second = form.elements.namedItem("toAccountId");
      if (second?.options.length > 1) second.selectedIndex = 1;
    }
    fields.addEventListener("input", () => { state.pendingKey = null; }, { once: true });
    dialog.showModal();
    fields.querySelector("input,select")?.focus();
  }

  function amount(value) {
    const normalized = String(value).trim().replace(",", ".");
    if (!/^(?:0|[1-9]\d{0,16})(?:\.\d{1,2})?$/.test(normalized) || Number(normalized) <= 0) {
      throw new Error("Informe um valor positivo com até duas casas decimais (ex.: 150,50).");
    }
    return Number(normalized);
  }

  function uniqueKey() {
    if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
    return "demo-" + Date.now() + "-" + Math.random().toString(36).slice(2);
  }

  async function saveOperation() {
    const data = new FormData(form);
    const mode = state.mode;
    if (!mode) return;
    let path, body, key = null;
    if (mode === "account") {
      const name = String(data.get("ownerName") || "").trim();
      if (!name || name.length > 160) throw new Error("Informe o nome do titular (até 160 caracteres).");
      path = "/api/v1/accounts";
      body = { ownerName: name };
    } else {
      key = state.pendingKey ||= uniqueKey();
      const value = amount(data.get("amount"));
      if (mode === "deposit") {
        path = "/api/v1/accounts/" + encodeURIComponent(data.get("accountId")) + "/deposits";
        body = { amount: value };
      } else {
        const from = String(data.get("fromAccountId"));
        const to = String(data.get("toAccountId"));
        if (from === to) throw new Error("Selecione contas diferentes para a transferência.");
        path = "/api/v1/transfers";
        body = { fromAccountId: from, toAccountId: to, amount: value };
      }
    }
    const saved = await api(path, {
      method: "POST",
      headers: key ? { "Idempotency-Key": key } : {},
      body: JSON.stringify(body)
    });
    if (mode === "account") state.selected = saved.id;
    if (mode === "deposit") state.selected = String(data.get("accountId"));
    if (mode === "transfer") state.selected = String(data.get("fromAccountId"));
    dialog.close();
    await loadAccounts();
    toast(mode === "account" ? "Conta criada com sucesso!" : mode === "deposit"
      ? "Crédito de demonstração registrado!" : "Transferência concluída!");
  }

  $("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const submit = event.currentTarget.querySelector("button[type=submit]");
    submit.disabled = true;
    errorMessage($("login-error"), null);
    const username = $("username").value.trim();
    state.authorization = basicHeader(username, $("password").value);
    try {
      await loadAccounts();
      loginScreen.hidden = true;
      app.hidden = false;
      $("password").value = "";
    } catch (e) {
      state.authorization = null;
      errorMessage($("login-error"), e.message || "Falha ao acessar API.");
    } finally {
      submit.disabled = false;
    }
  });

  $("logout").addEventListener("click", () => {
    state.authorization = null;
    state.accounts = [];
    state.selected = null;
    app.hidden = true;
    loginScreen.hidden = false;
    $("password").value = "";
    $("username").focus();
  });
  $("refresh").addEventListener("click", () => loadAccounts().then(() => toast("Dados atualizados.")).catch((e) => toast(e.message, true)));
  $("history-account").addEventListener("change", (event) => {
    state.selected = event.target.value;
    loadHistory().catch((e) => toast(e.message, true));
  });
  $("open-account").addEventListener("click", () => openDialog("account"));
  $("open-deposit").addEventListener("click", () => openDialog("deposit"));
  $("open-transfer").addEventListener("click", () => openDialog("transfer"));
  $("hero-transfer").addEventListener("click", () => openDialog("transfer"));
  $("dialog-close").addEventListener("click", () => dialog.close());
  $("dialog-cancel").addEventListener("click", () => dialog.close());
  fields.addEventListener("change", () => { state.pendingKey = null; });
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = $("dialog-submit");
    button.disabled = true;
    errorMessage($("dialog-error"), null);
    try { await saveOperation(); }
    catch (e) { errorMessage($("dialog-error"), e.message || "Não foi possível concluir."); }
    finally { button.disabled = false; }
  });
})();
