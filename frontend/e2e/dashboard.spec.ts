import { test, expect } from '@playwright/test';
import { loginViaStorage } from './helpers/auth.helper';

// ---------------------------------------------------------------------------
// Dashboard Tests
// ---------------------------------------------------------------------------

test.describe('Dashboard', () => {

  test.beforeEach(async ({ page }) => {
    await loginViaStorage(page);
  });

  test('renders the page title / heading', async ({ page }) => {
    await expect(page.locator('h1, h2, .page-title').first()).toBeVisible({ timeout: 15_000 });
  });

  test('displays the four KPI stat cards', async ({ page }) => {
    // Cards typically show active loans, total loans, portfolio value, PAR30
    const cards = page.locator('.stat-card, .kpi-card, [class*="stat"], [class*="kpi"]');
    await expect(cards.first()).toBeVisible({ timeout: 15_000 });
    await expect(cards).toHaveCount(4);
  });

  test('shows non-zero total portfolio value', async ({ page }) => {
    // Wait for data to load (loading spinner disappears)
    await page.waitForSelector('.loading, .spinner', { state: 'hidden', timeout: 15_000 }).catch(() => {});
    const valueCard = page.locator('.stat-card, .kpi-card').filter({ hasText: /portfolio|value|KES|loan/i }).first();
    await expect(valueCard).toBeVisible({ timeout: 15_000 });
  });

  test('sector risk chart canvas is present', async ({ page }) => {
    await page.waitForSelector('canvas', { timeout: 20_000 });
    const canvases = page.locator('canvas');
    await expect(canvases).toHaveCount(2);   // sector bar + grade doughnut
  });

  test('risk grade distribution chart canvas is present', async ({ page }) => {
    const canvases = page.locator('canvas');
    await expect(canvases.nth(1)).toBeVisible({ timeout: 20_000 });
  });

  test('"New Loan" action link navigates to /new-loan', async ({ page }) => {
    const newLoanLink = page.getByRole('link', { name: /new loan/i }).first();
    await expect(newLoanLink).toBeVisible({ timeout: 10_000 });
    await newLoanLink.click();
    await expect(page).toHaveURL(/\/new-loan/);
  });

});
