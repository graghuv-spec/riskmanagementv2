import { test, expect } from '@playwright/test';
import { loginViaStorage } from './helpers/auth.helper';

// ---------------------------------------------------------------------------
// Borrower Hub Context & Credit Score E2E
// ---------------------------------------------------------------------------

test.describe('Borrower Hub', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaStorage(page);
    await page.goto('/borrower-hub');
    await expect(page).toHaveURL(/\/borrower-hub/);
  });

  test('renders borrower search and list', async ({ page }) => {
    await expect(page.locator('input[placeholder*="search" i]')).toBeVisible();
    await expect(page.locator('.borrower-list, .borrower-row')).toBeVisible();
  });

  test('selecting a borrower sets context and updates sidebar/header', async ({ page }) => {
    const firstBorrower = page.locator('.borrower-row').first();
    await firstBorrower.click();
    await expect(page.locator('app-sidebar, [class*="sidebar"]')).toContainText(/Borrower:/);
    await expect(page.locator('app-header, [class*="header"]')).toContainText(/Borrower:/);
  });

  test('admin can update credit score for a borrower', async ({ page }) => {
    // Switch to admin role
    await page.evaluate(() => {
      const raw = localStorage.getItem('rm_user');
      if (!raw) return;
      const user = JSON.parse(raw);
      user.role = 'Admin';
      localStorage.setItem('rm_user', JSON.stringify(user));
    });
    await page.reload();
    const firstBorrower = page.locator('.borrower-row').first();
    await firstBorrower.click();
    const creditScoreInput = page.locator('input[name="creditScore"]');
    await expect(creditScoreInput).toBeEditable();
    await creditScoreInput.fill('750');
    const updateBtn = page.getByRole('button', { name: /update credit score/i });
    await updateBtn.click();
    await expect(page.locator('.success-msg')).toContainText(/updated/i);
  });

  test('agent cannot update credit score', async ({ page }) => {
    await page.evaluate(() => {
      const raw = localStorage.getItem('rm_user');
      if (!raw) return;
      const user = JSON.parse(raw);
      user.role = 'Agent';
      localStorage.setItem('rm_user', JSON.stringify(user));
    });
    await page.reload();
    const firstBorrower = page.locator('.borrower-row').first();
    await firstBorrower.click();
    const creditScoreInput = page.locator('input[name="creditScore"]');
    await expect(creditScoreInput).toBeDisabled();
  });
});
