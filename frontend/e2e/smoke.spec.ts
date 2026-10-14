import { expect, test } from '@playwright/test';

test('E2E_SMOKE: app shell loads and /api reaches the backend through the Vite proxy', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('app-root')).toBeVisible();

  const response = await page.request.get('/api/links');
  expect(response.status()).toBe(200);
  // Shape only: E2E tests share one in-memory DB, so the list may not be empty (docs/feature-map.md).
  const body = await response.json();
  expect(Array.isArray(body.items)).toBe(true);
  expect(body).toMatchObject({ page: 0, size: 20 });
});
