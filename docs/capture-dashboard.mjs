// Screenshot of the ACTUAL Spring Boot dashboard rendered by Chromium.
// The workflow runs against an ephemeral H2 database and creates fictional accounts.
import { chromium } from "playwright";

const browser = await chromium.launch({ headless: true });
try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1100 },
    deviceScaleFactor: 1,
    reducedMotion: "reduce"
  });
  page.on("pageerror", (error) => { throw error; });

  await page.goto("http://127.0.0.1:8080/", { waitUntil: "networkidle" });
  await page.locator("#username").fill("admin");
  await page.locator("#password").fill("dev-only-change-me");
  await page.locator('#login-form button[type="submit"]').click();
  await page.locator("#app").waitFor({ state: "visible", timeout: 15000 });

  async function createAccount(owner) {
    await page.locator("#open-account").click();
    await page.locator("#field-ownerName").fill(owner);
    await page.locator("#dialog-submit").click();
    await page.locator("#operation-dialog").waitFor({ state: "hidden", timeout: 15000 });
  }
  await createAccount("Vértice Serviços");
  await createAccount("Orion Sistemas");
  await createAccount("Helix Soluções");

  async function deposit(index, value) {
    await page.locator("#open-deposit").click();
    await page.locator("#field-accountId").selectOption({ index });
    await page.locator("#field-amount").fill(value);
    await page.locator("#dialog-submit").click();
    await page.locator("#operation-dialog").waitFor({ state: "hidden", timeout: 15000 });
  }
  await deposit(0, "25000");
  await deposit(1, "12500");

  await page.locator("#open-transfer").click();
  await page.locator("#field-fromAccountId").selectOption({ index: 0 });
  await page.locator("#field-toAccountId").selectOption({ index: 1 });
  await page.locator("#field-amount").fill("2300");
  await page.locator("#dialog-submit").click();
  try {
    await page.locator("#operation-dialog").waitFor({ state: "hidden", timeout: 15000 });
  } catch (error) {
    console.error("Transfer dialog error:", await page.locator("#dialog-error").textContent());
    console.error("From account:", await page.locator("#field-fromAccountId").inputValue());
    console.error("To account:", await page.locator("#field-toAccountId").inputValue());
    console.error("Amount:", await page.locator("#field-amount").inputValue());
    throw error;
  }

  await page.waitForFunction(() => document.querySelectorAll("#accounts-body tr").length === 3);
  await page.waitForFunction(() => document.querySelectorAll(".activity-row").length >= 2);
  await page.locator("#toast").evaluate((el) => { el.hidden = true; });
  // A full-page screenshot should display the navigation at the top.
  await page.addStyleTag({ content: ".sidebar { position: relative !important; top: 0 !important; height: auto !important; min-height: 100% !important; }" });

  await page.screenshot({
    path: "docs/assets/dashboard-real.png",
    fullPage: true,
    animations: "disabled"
  });
  console.log("Captured actual dashboard backed by H2 demo data.");
} finally {
  await browser.close();
}
