import { expect, test } from "@playwright/test";

/**
 * Critical-flow E2E: log in as the env-seeded admin, start a session,
 * log three sets against the first planned exercise, finish the session,
 * and download the Claude summary JSON. Public registration was removed
 * in t-51, so the test assumes an admin has been seeded via
 * APP_ADMIN_EMAIL + APP_ADMIN_PASSWORD_HASH before the stack is brought
 * up with docker compose. The password plaintext must be supplied via
 * E2E_ADMIN_PASSWORD for the login step.
 */

const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? "admin@workouthub.local";
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? "ChangeMe-Admin-1!";

test.describe.configure({ mode: "serial" });

test("login, start session, log three sets, finish, download JSON", async ({
  page,
}) => {
  await page.goto("/login");
  await page.getByLabel(/email/i).fill(ADMIN_EMAIL);
  await page.getByLabel(/secret|password/i).fill(ADMIN_PASSWORD);
  await page.getByRole("button", { name: /log in/i }).click();

  await expect(page).toHaveURL(/\/dashboard/);

  const startButton = page.getByRole("button", { name: /start workout|antrenmani baslat/i });
  const resumeButton = page.getByRole("button", { name: /resume|devam et/i });

  if (await resumeButton.isVisible().catch(() => false)) {
    await resumeButton.click();
  } else {
    await startButton.click();
  }

  await expect(page).toHaveURL(/\/session\//);

  const repsInputs = page.getByLabel(/^reps$|^tekrar$/i);
  const completeButtons = page.getByRole("button", { name: /^done$|^tamam$/i });
  await expect(repsInputs.first()).toBeVisible();

  for (let i = 0; i < 3; i++) {
    await repsInputs.first().fill("10");
    await completeButtons.first().click();
    await page.waitForTimeout(400);
  }

  page.once("dialog", (d) => d.accept());
  await page.getByRole("button", { name: /finish|bitir/i }).click();
  await expect(page).toHaveURL(/\/dashboard/);

  await page.goto("/export");
  const [download] = await Promise.all([
    page.waitForEvent("download"),
    page.getByRole("button", { name: /download summary|ozeti indir/i }).click(),
  ]);
  const suggested = download.suggestedFilename();
  expect(suggested).toMatch(/\.json$/i);
});
