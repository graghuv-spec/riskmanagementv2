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
  const loginResponse = await page.request.post('/api/auth/login', {
    data: { email: TEST_EMAIL, password: TEST_PASSWORD }
  });

  if (!loginResponse.ok()) {
    throw new Error(`Storage login failed: ${loginResponse.status()}`);
  }

  const loginUser = await loginResponse.json();

  await page.goto('/login');
  await page.evaluate((user) => {
    localStorage.setItem('rm_user', JSON.stringify({
      userId: user.userId,
      name: user.name,
      email: user.email,
      role: user.role,
      institutionId: user.institutionId,
      token: user.token,
      tokenType: user.tokenType,
    }));
  }, loginUser);
  await page.goto('/dashboard');
}

/**
 * Clears auth state from localStorage.
 */
export async function logout(page: Page): Promise<void> {
  await page.goto('/login');
  await page.evaluate(() => localStorage.removeItem('rm_user'));
}
