import { expect, test } from "@playwright/test";
import { loginAsAdmin } from "./fixtures/auth";

/**
 * Critical-flow E2E: log in as the env-seeded admin via the shared
 * loginAsAdmin fixture, start a session, log three sets against the first
 * planned exercise, finish the session, and download the Claude summary
 * JSON. Public registration was removed in t-51, so the test assumes an
 * admin has been seeded via APP_ADMIN_EMAIL + APP_ADMIN_PASSWORD_HASH
 * before the stack is brought up with docker compose. The password
 * plaintext must be supplied via E2E_ADMIN_PASSWORD for the login step.
 *
 * Locators are i18n-immune: form fields use stable HTML attributes (id,
 * data-testid), action buttons use anchored EN+TR alternation regexes,
 * and the per-set network round-trip is awaited via waitForResponse on
 * POST /api/sessions/{id}/sets instead of a fixed timeout.
 */

test.describe.configure({ mode: "serial" });

test("login, start session, log three sets, finish, download JSON", async ({
  page,
}) => {
  await loginAsAdmin(page);

  const startButton = page.getByRole("button", {
    name: /^(Antrenmani baslat|Start workout)$/i,
  });
  const resumeButton = page.getByRole("button", {
    name: /^(Devam et|Resume)$/i,
  });

  if (await resumeButton.isVisible().catch(() => false)) {
    await resumeButton.click();
  } else {
    await startButton.click();
  }

  await expect(page).toHaveURL(/\/session\//);

  const repsInputs = page.locator('[data-testid="set-reps-input"]');
  const completeButtons = page.locator('[data-testid="set-done-button"]');
  await expect(repsInputs.first()).toBeVisible();

  for (let i = 0; i < 3; i++) {
    await repsInputs.first().fill("10");
    await completeButtons.first().click();
    await page.waitForResponse(
      (res) =>
        /\/api\/sessions\/.+\/sets$/.test(res.url()) &&
        res.request().method() === "POST" &&
        res.status() < 400
    );
  }

  page.once("dialog", (d) => d.accept());
  await page
    .getByRole("button", { name: /^(Antrenmani bitir|Finish workout)$/i })
    .click();
  await expect(page).toHaveURL(/\/dashboard/);

  await page.goto("/export");
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page
      .getByRole("button", { name: /^(Ozeti indir|Download summary)$/i })
      .click(),
  ]);
  const suggested = download.suggestedFilename();
  expect(suggested).toMatch(/\.json$/i);
});
