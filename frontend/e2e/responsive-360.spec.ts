import { expect, test } from "@playwright/test";
import { loginAsAdmin } from "./fixtures/auth";

/**
 * Responsive smoke at 360x800 (iPhone SE / older Android baseline).
 * Asserts no horizontal overflow on the five highest-traffic pages and
 * that the bottom tab-bar is present, since the desktop top nav is
 * hidden below sm. Uses the shared loginAsAdmin fixture so the login
 * step stays i18n-immune (phase 38-01).
 */

const PAGES = ["/dashboard", "/plan", "/history", "/exercises", "/nutrition"];

test.describe.configure({ mode: "serial" });
test.use({ viewport: { width: 360, height: 800 } });

test("no horizontal overflow on 5 key pages at 360px", async ({ page }) => {
  await loginAsAdmin(page);

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
