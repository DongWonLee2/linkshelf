import { expect, test } from '@playwright/test';
import { isMarkRead, linkItem, saveLinkViaApi, uniqueUrl, type Link } from '../../../../frontend/e2e/support';
import { json, shot } from './evidence';

test('verify mark-read: checkbox persists across reload and in the API', async ({ page, request }) => {
  const link = await saveLinkViaApi(request, { url: uniqueUrl('verify-mark-read') });

  await page.goto('/');
  const toggle = linkItem(page, link.id).getByTestId('link-item-read-toggle');
  await expect(toggle).not.toBeChecked();
  const marked = page.waitForResponse((res) => isMarkRead(new URL(res.url())) && res.request().method() === 'PUT');
  await toggle.click();
  expect((await marked).status()).toBe(200);
  await expect(toggle).toBeChecked();
  await shot(page, '1-checked');

  await page.reload();
  await expect(linkItem(page, link.id).getByTestId('link-item-read-toggle')).toBeChecked();
  await shot(page, '2-after-reload');

  const body = (await (await request.get('/api/links?page=0&size=100')).json()) as { items: Link[] };
  const stored = body.items.find((item) => item.id === link.id);
  json('3-api-list', { id: link.id, stored });
  expect(stored?.read).toBe(true);
});
