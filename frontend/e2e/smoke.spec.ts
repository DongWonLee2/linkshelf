import { expect, test } from '@playwright/test';

test('E2E_SMOKE: app shell loads and /api reaches the backend through the Vite proxy', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('app-root')).toBeVisible();

  const response = await page.request.get('/api/links');
  expect(response.status()).toBe(200);
  expect(await response.json()).toMatchObject({ items: [], page: 0, size: 20 });
});
