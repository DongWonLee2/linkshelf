import { expect, test } from '@playwright/test';
import { uniqueUrl, type Link } from '../../../../frontend/e2e/support';
import { aria, json, shot } from './evidence';

test('verify save-link: form save shows the link first, API and reload agree', async ({ page, request }) => {
  const url = uniqueUrl('verify-save-link');
  await page.goto('/');
  await expect(page.getByTestId('app-root')).toBeVisible();

  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-memo-input').fill('검증 메모');
  await page.getByTestId('link-tags-input').fill('Verify, 검증');
  await shot(page, '1-filled');
  await page.getByTestId('link-save-button').click();

  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(page.getByTestId('link-url-input')).toHaveValue('');
  await expect(page.getByTestId('link-memo-input')).toHaveValue('');
  await expect(page.getByTestId('link-tags-input')).toHaveValue('');
  const first = page.getByTestId('link-item').first();
  await expect(first.getByTestId('link-item-url')).toHaveText(url);
  await expect(first.getByTestId('link-item-memo')).toHaveText('검증 메모');
  await expect(first.getByTestId('link-item-tag')).toHaveText(['Verify', '검증']);
  await shot(page, '2-saved');
  await aria(page.getByTestId('link-list'), '2-saved-list');

  const res = await request.get('/api/links?page=0&size=20');
  expect(res.status()).toBe(200);
  const body = (await res.json()) as { items: Link[] };
  json('3-api-list', body);
  const stored = body.items.find((item) => item.url === url);
  expect(stored, 'saved link is returned by GET /api/links').toBeDefined();
  expect(stored?.memo).toBe('검증 메모');
  expect(stored?.tags).toEqual(['Verify', '검증']);

  await page.reload();
  await expect(page.getByTestId('link-item').first().getByTestId('link-item-url')).toHaveText(url);
  await shot(page, '4-after-reload');
});
