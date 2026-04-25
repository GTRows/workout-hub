import { expect, test } from "@playwright/test";

/**
 * Responsive smoke at 360x800 (iPhone SE / older Android baseline).
 * Asserts no horizontal overflow on the five highest-traffic pages and
 * that the bottom tab-bar is present, since the desktop top nav is
 * hidden below sm. Requires the same env-seeded admin as critical-flow.
 */

const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? "admin@workouthub.local";
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? "ChangeMe-Admin-1!";

const PAGES = ["/dashboard", "/plan", "/history", "/exercises", "/nutrition"];

test.describe.configure({ mode: "serial" });
test.use({ viewport: { width: 360, height: 800 } });

test("no horizontal overflow on 5 key pages at 360px", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel(/email/i).fill(ADMIN_EMAIL);
  await page.getByLabel(/secret|password/i).fill(ADMIN_PASSWORD);
  await page.getByRole("button", { name: /log in/i }).click();
  await expect(page).toHaveURL(/\/dashboard/);

  for (const path of PAGES) {
    await page.goto(path);
    await expect(page.getByTestId("bottom-nav")).toBeVisible();

    const overflow = await page.evaluate(() => {
      const docWidth = document.documentElement.scrollWidth;
      const viewWidth = document.documentElement.clientWidth;
      return docWidth - viewWidth;
    });
    expect(overflow, `${path} should not overflow horizontally`).toBeLessThanOrEqual(1);

    await page.screenshot({ path: `e2e/screenshots/360-${path.replace(/\//g, "_")}.png`, fullPage: true });
  }
});
