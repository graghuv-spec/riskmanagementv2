import { test, expect } from '@playwright/test';
import { loginViaStorage, logout } from './helpers/auth.helper';

// ---------------------------------------------------------------------------
// Navigation & Auth Guard Tests
// ---------------------------------------------------------------------------

test.describe('Navigation', () => {

  test.describe('Auth guard', () => {

    test('redirects every protected route to /login when unauthenticated', async ({ page }) => {
      const protectedRoutes = ['/dashboard', '/new-loan', '/risk-result', '/portfolio', '/reports'];
      for (const route of protectedRoutes) {
        await logout(page);
        await page.goto(route);
        await expect(page).toHaveURL(/\/login/, { timeout: 5_000 });
      }
    });

    test('root path "/" redirects to /dashboard when authenticated', async ({ page }) => {
      await loginViaStorage(page);
      await page.goto('/');
      await expect(page).toHaveURL(/\/dashboard/);
    });

    test('wildcard "/**" redirects to /dashboard when authenticated', async ({ page }) => {
      await loginViaStorage(page);
      await page.goto('/some-nonexistent-page');
      await expect(page).toHaveURL(/\/dashboard/);
    });

  });

  test.describe('Sidebar navigation', () => {

    test.beforeEach(async ({ page }) => {
      await loginViaStorage(page);
    });

    test('sidebar is visible on all authenticated pages', async ({ page }) => {
      for (const route of ['/dashboard', '/new-loan', '/portfolio', '/reports']) {
        await page.goto(route);
        await expect(page.locator('app-sidebar, [class*="sidebar"]').first())
          .toBeVisible({ timeout: 10_000 });
      }
    });

    test('clicking Dashboard link navigates to /dashboard', async ({ page }) => {
      await page.goto('/new-loan');
      await page.locator('app-sidebar, [class*="sidebar"]').first()
        .getByRole('link', { name: /dashboard/i }).click();
      await expect(page).toHaveURL(/\/dashboard/);
    });

    test('clicking New Loan link navigates to /new-loan', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForSelector('app-sidebar, [class*="sidebar"]', { timeout: 10_000 });
      await page.locator('app-sidebar, [class*="sidebar"]').first()
        .getByRole('link', { name: /new loan/i }).click();
      await expect(page).toHaveURL(/\/new-loan/);
    });

    test('clicking Portfolio link navigates to /portfolio', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForSelector('app-sidebar, [class*="sidebar"]', { timeout: 10_000 });
      await page.locator('app-sidebar, [class*="sidebar"]').first()
        .getByRole('link', { name: /portfolio/i }).click();
      await expect(page).toHaveURL(/\/portfolio/);
    });

    test('clicking Reports link navigates to /reports', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForSelector('app-sidebar, [class*="sidebar"]', { timeout: 10_000 });
      await page.locator('app-sidebar, [class*="sidebar"]').first()
        .getByRole('link', { name: /report/i }).click();
      await expect(page).toHaveURL(/\/reports/);
    });

  });

  test.describe('Header', () => {

    test.beforeEach(async ({ page }) => {
      await loginViaStorage(page);
    });

    test('header is visible on all authenticated pages', async ({ page }) => {
      for (const route of ['/dashboard', '/new-loan', '/portfolio']) {
        await page.goto(route);
        await expect(page.locator('app-header, header, [class*="header"]').first())
          .toBeVisible({ timeout: 10_000 });
      }
    });

    test('header shows the application name', async ({ page }) => {
      await expect(page.locator('app-header, header, [class*="header"]').first())
        .toContainText(/risk/i, { timeout: 10_000 });
    });

  });

});
