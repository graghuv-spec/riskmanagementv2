# New Loan Generate Risk Score Fix Plan

## Problem Statement

In the New Loan screen, after entering all fields and clicking **Generate Risk Score**, the UI appears to do nothing for some users.

## Current Flow (Observed)

1. `frontend/src/app/screens/new-loan/new-loan.component.html`
   - Form submit calls `(ngSubmit)="generate()"`.
2. `frontend/src/app/screens/new-loan/new-loan.component.ts`
   - `generate()` validates fields.
   - Calls `loanService.calculateRiskScore(this.form)`.
   - On success, navigates to `/risk-result`.
   - On error, shows generic message: `Failed to calculate risk score. Is the backend running?`.
3. `frontend/src/app/core/services/loan.service.ts`
   - POST to `/api/risk-scores/calculate`.
4. `backend/src/main/java/com/riskmanagement/controller/RiskScoreController.java`
   - `@PostMapping("/calculate")` returns calculated `RiskScore`.

## Likely Root Cause

Security was tightened in backend:

- `backend/src/main/java/com/riskmanagement/config/SecurityConfig.java`
  - Only `/api/auth/login` is public.
  - All other `/api/**` require auth.

New Loan API call now requires a valid Bearer token. If token is missing/expired/invalid:

- Backend returns `401` or `403`.
- Frontend shows generic backend-down error.
- User perceives this as "button does nothing".

## Reproduction Checklist

1. Start app in native mode.
2. Open DevTools Network tab.
3. Login and go to New Loan.
4. Enter valid form values and click Generate Risk Score.
5. Inspect request to `POST /api/risk-scores/calculate`.
6. Confirm whether status is `200` (works) or `401/403` (auth issue).

## Fix Plan

### Phase A: Improve Failure Visibility (Frontend)

1. Update error handling in `new-loan.component.ts`:
   - Differentiate errors by status code.
   - `401/403` message: "Session expired or unauthorized. Please login again."
   - `5xx` message: "Server error while calculating risk score."
   - Network failure message: "Cannot reach backend service."
2. Ensure `loading` is reset in all error paths.
3. Add optional console logging for request failure in development mode.

### Phase B: Add Auth Recovery UX

1. On `401/403` from Generate Risk call:
   - Clear invalid auth state (`AuthService.logout()`),
   - Redirect to `/login` with return URL to `/new-loan`.
2. Show a visible toast/error banner before redirecting (or on login page).

### Phase C: Validate Request Payload and API Contract

1. Confirm payload keys from `new-loan.component.ts` match backend DTO in `RiskScoreController.CalculateRiskRequest`.
2. Add frontend-side coercion for numeric fields before submit to avoid accidental string payload issues.
3. Add backend validation annotations for request fields (optional hardening).

### Phase D: Optional Security Rule Adjustment (Decision)

Decide whether `POST /api/risk-scores/calculate` should remain protected.

- Option 1 (recommended): keep protected, improve auth UX.
- Option 2: permit unauthenticated calculate endpoint in `SecurityConfig` (not preferred for production).

## Test Plan

### Manual

1. Valid login + valid form => navigates to `/risk-result`.
2. Expired/invalid token => clear auth feedback + redirect to `/login`.
3. Backend down => user sees clear connectivity error.
4. Missing required field => client validation error shown immediately.

### Automated

1. Add/adjust E2E in `frontend/e2e/loan-flow.spec.ts`:
   - Authenticated generate flow should reach `/risk-result`.
   - Unauthorized flow should redirect to `/login` and show message.
2. Add unit tests for `generate()` error branch behavior (status-specific handling).

## Acceptance Criteria

1. Clicking Generate Risk Score always results in one visible outcome:
   - success navigation,
   - validation message,
   - clear auth error with redirect,
   - clear backend/network error.
2. No silent failures.
3. Error messages reflect real cause (auth vs backend availability).

## Implementation Order

1. Frontend error status mapping in `new-loan.component.ts`.
2. Auth recovery redirect on `401/403`.
3. E2E coverage updates for loan flow.
4. Optional backend validation/security adjustment.
