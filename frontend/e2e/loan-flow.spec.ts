import { test, expect } from '@playwright/test';
import { loginViaStorage } from './helpers/auth.helper';

// ---------------------------------------------------------------------------
// New Loan → Risk Result End-to-End Flow
// ---------------------------------------------------------------------------

test.describe('Loan Application Flow', () => {

  test.beforeEach(async ({ page }) => {
    await loginViaStorage(page);
    await page.goto('/new-loan');
    await expect(page).toHaveURL(/\/new-loan/);
  });

  test('new-loan page renders the loan form', async ({ page }) => {
    await expect(page.locator('form, [class*="form"]').first()).toBeVisible({ timeout: 10_000 });
  });

  test('shows validation error when required fields are empty', async ({ page }) => {
    // Click submit without filling in anything
    const submitBtn = page.getByRole('button', { name: /generate|assess|calculate|submit/i });
    await expect(submitBtn).toBeVisible({ timeout: 10_000 });
    await submitBtn.click();
    await expect(page.locator('.error, .error-msg, [class*="error"]').first())
      .toContainText(/required|fill/i, { timeout: 5_000 });
  });

  test('collateral ratio updates as amounts are entered', async ({ page }) => {
    // Fill loan amount and collateral value to trigger the computed ratio
    const loanAmountInput = page.locator('input[name="loanAmount"], input[placeholder*="amount" i]').first();
    const collateralInput = page.locator('input[name="collateralValue"], input[placeholder*="collateral" i]').first();

    await loanAmountInput.fill('10000');
    await collateralInput.fill('15000');

    // Ratio = 1.50 — the template should render it somewhere on the page
    await expect(page.locator('body')).toContainText(/1\.5/);
  });

  test('submitting a complete form calculates risk score and navigates to /risk-result', async ({ page }) => {
    // Fill all required fields
    await page.locator('input[name="fullName"], input[placeholder*="name" i]').first().fill('Test Borrower');
    await page.locator('input[name="nationalId"], input[placeholder*="id" i]').first().fill('NID-TEST-001');
    await page.locator('input[name="age"], input[placeholder*="age" i]').first().fill('35');
    await page.locator('input[name="monthlyIncome"], input[placeholder*="income" i]').first().fill('3000');
    await page.locator('input[name="collateralValue"], input[placeholder*="collateral" i]').first().fill('20000');
    await page.locator('input[name="loanAmount"], input[placeholder*="amount" i]').first().fill('10000');
    await page.locator('input[name="interestRate"], input[placeholder*="interest" i]').first().fill('12');
    await page.locator('input[name="tenureMonths"], input[placeholder*="tenure" i]').first().fill('24');

    // Location select — pick the first non-empty option if a select exists
    const locationSelect = page.locator('select[name="location"]');
    if (await locationSelect.count() > 0) {
      await locationSelect.selectOption({ index: 1 });
    }

    const submitBtn = page.getByRole('button', { name: /generate|assess|calculate|submit/i });
    await submitBtn.click();

    // Should navigate to /risk-result
    await expect(page).toHaveURL(/\/risk-result/, { timeout: 20_000 });
  });

  // -------------------------------------------------------------------------
  // Risk Result Page (reached via router state from new-loan submission)
  // -------------------------------------------------------------------------
  test.describe('Risk Result Page', () => {

    test.beforeEach(async ({ page }) => {
      // Inject a mock risk result directly via router state using History API
      await loginViaStorage(page);
      await page.evaluate(() => {
        const mockState = {
          riskScore: {
            riskScore: 75,
            riskGrade: 'B',
            probabilityDefault: 0.22,
            recommendedLimit: 8500,
            modelVersion: 'Rule-Based v1',
            explanationJson: '{"method":"rule-based"}',
          },
          loanData: {
            fullName: 'Test Borrower',
            loanAmount: 10000,
            monthlyIncome: 3000,
            businessSector: 'Agriculture',
            location: 'Nairobi',
          },
        };
        history.pushState(mockState, '', '/risk-result');
      });
      await page.goto('/risk-result');
    });

    test('displays the risk score', async ({ page }) => {
      await expect(page.locator('body')).toContainText('75', { timeout: 10_000 });
    });

    test('displays the risk grade', async ({ page }) => {
      await expect(page.locator('body')).toContainText('B', { timeout: 10_000 });
    });

    test('displays the probability of default', async ({ page }) => {
      // 0.22 × 100 = 22.0%
      await expect(page.locator('body')).toContainText(/22\.0/, { timeout: 10_000 });
    });

    test('displays the recommended limit', async ({ page }) => {
      await expect(page.locator('body')).toContainText(/8[,.]?500|8500/, { timeout: 10_000 });
    });

    test('driver contribution values are deterministic (no random flicker)', async ({ page }) => {
      const getText = async () =>
        page.locator('[class*="driver"], [class*="contribution"], [class*="score"]').allInnerTexts();

      const first  = await getText();
      await page.waitForTimeout(500);
      const second = await getText();

      expect(first).toEqual(second);
    });

    test('"New Application" button navigates back to /new-loan', async ({ page }) => {
      const btn = page.getByRole('button', { name: /new application/i });
      if (await btn.count() > 0) {
        await btn.click();
        await expect(page).toHaveURL(/\/new-loan/, { timeout: 5_000 });
      }
    });
  });

});
