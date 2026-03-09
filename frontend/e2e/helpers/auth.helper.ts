import { Page } from '@playwright/test';

export const TEST_EMAIL    = 'admin@mfb.com';
export const TEST_PASSWORD = 'password123';

/**
 * Performs a full UI login and waits for the dashboard to be visible.
 */
export async function loginViaUI(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Email Address').fill(TEST_EMAIL);
  await page.getByLabel('Password').fill(TEST_PASSWORD);
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL('**/dashboard');
}

/**
 * Injects the user object directly into localStorage so tests that don't need
 * to verify the login flow itself can skip the round-trip.
 */
export async function loginViaStorage(page: Page): Promise<void> {
  await page.goto('/login');          // navigate once to load the origin
  await page.evaluate(([email]) => {
    localStorage.setItem('rm_user', JSON.stringify({
      userId: 1,
      name: 'Alice Admin',
      email,
      role: 'Admin',
      institutionId: 1,
    }));
  }, [TEST_EMAIL]);
  await page.goto('/dashboard');
}

/**
 * Clears auth state from localStorage.
 */
export async function logout(page: Page): Promise<void> {
  await page.evaluate(() => localStorage.removeItem('rm_user'));
}
