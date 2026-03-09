import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E configuration.
 * In CI the Angular dev server is started externally before the test run.
 * Locally, `webServer` spins it up automatically.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,          // sequential – tests share a live backend
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] ? 1 : 1,
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
  ],

  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* TypeScript compilation for the E2E suite */
  tsconfig: './tsconfig.e2e.json',

  /* Start the Angular dev server automatically when not already running */
  webServer: process.env['CI']
    ? undefined                  // CI starts ng serve externally (see ci.yml)
    : {
        command: 'npm run start',
        url: 'http://localhost:4200',
        reuseExistingServer: true,
        timeout: 120_000,
      },
});
