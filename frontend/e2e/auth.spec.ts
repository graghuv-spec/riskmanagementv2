import { test, expect } from '@playwright/test';
import { loginViaUI, loginViaStorage, logout, TEST_EMAIL, TEST_PASSWORD } from './helpers/auth.helper';

// ---------------------------------------------------------------------------
// Authentication Flow Tests
// ---------------------------------------------------------------------------

test.describe('Authentication', () => {

  test('unauthenticated visit to dashboard redirects to /login', async ({ page }) => {
    await logout(page);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);
  });

  test('unauthenticated visit to any guarded route redirects to /login', async ({ page }) => {
    await logout(page);
    for (const route of ['/new-loan', '/risk-result', '/portfolio', '/reports']) {
      await page.goto(route);
      await expect(page).toHaveURL(/\/login/, { timeout: 5_000 });
    }
  });

  test('shows validation error when fields are empty', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.locator('.error-msg')).toContainText(/please enter email and password/i);
  });

  test('shows error on invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email Address').fill('wrong@test.com');
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.locator('.error-msg')).toContainText(/invalid email or password/i, { timeout: 10_000 });
    // Must NOT reveal actual credentials in error message
    await expect(page.locator('.error-msg')).not.toContainText('password123');
  });

  test('successfully logs in with valid credentials and lands on dashboard', async ({ page }) => {
    await logout(page);
    await loginViaUI(page);
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.locator('app-sidebar').first()).toBeVisible({ timeout: 10_000 });
  });

  test('already authenticated user is redirected away from /login', async ({ page }) => {
    await loginViaStorage(page);
    await page.goto('/login');
    await expect(page).toHaveURL(/\/dashboard/);
  });

  test('logout clears session and redirects to /login', async ({ page }) => {
    await loginViaStorage(page);
    // Trigger logout via localStorage (direct, since no logout button may exist yet)
    await logout(page);
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);
  });

});
