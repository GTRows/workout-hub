import { expect, test } from "@playwright/test";
import { loginAsAdmin } from "./fixtures/auth";

/**
 * Proves the offline-first session contract: a session that has logged at
 * least one set survives navigation to another route and is resumable from
 * the dashboard. Companion to critical-flow.spec.ts (which finishes the
 * session immediately).
 */

test.describe.configure({ mode: "serial" });

test("partial session survives navigation and is resumable", async ({ page }) => {
  await loginAsAdmin(page);

  // Start a session from the dashboard. If a session is already active
  // from a prior failed run, resume it; otherwise start one.
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
  const sessionUrl = page.url();

  // Log ONE set.
  const repsInput = page.locator('[data-testid="set-reps-input"]').first();
  const doneButton = page.locator('[data-testid="set-done-button"]').first();
  await repsInput.fill("10");
  await doneButton.click();
  await page.waitForResponse(
    (res) =>
      /\/api\/sessions\/.+\/sets$/.test(res.url()) &&
      res.request().method() === "POST" &&
      res.status() < 400
  );

  // Navigate away and back via the dashboard.
  await page.goto("/exercises");
  await page.goto("/dashboard");
  await expect(resumeButton).toBeVisible();
  await resumeButton.click();
  await expect(page).toHaveURL(sessionUrl);

  // Confirm the logged set's value persisted.
  await expect(repsInput).toHaveValue("10");
});
