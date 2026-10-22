import { expect, test } from '@playwright/test';
import { applyTagFilter, expectPageInfo, linkItem, saveLinkViaApi, uniqueId, uniqueUrl, type Link } from '../../../../frontend/e2e/support';
import { aria, json, shot } from './evidence';

test('verify unread-filter: unread only keeps tag and order, saving turns it off', async ({ page, request }) => {
  const tag = `un${uniqueId()}`;
  const saved: Link[] = [];
  for (const name of ['a', 'b', 'c']) {
    saved.push(await saveLinkViaApi(request, { url: uniqueUrl(`verify-unread-${name}`), tags: [tag] }));
  }
  const marked = await request.put(`/api/links/${saved[1].id}/read`, { data: { read: true } });
  expect(marked.status()).toBe(200);
  const untagged = await saveLinkViaApi(request, { url: uniqueUrl('verify-unread-untagged') });
  const ids = [saved[0], saved[2]].map((link) => String(link.id));

  await page.goto('/');
  const toggle = page.getByTestId('link-unread-only-toggle');
  const select = page.getByTestId('link-sort-select');
  const items = page.getByTestId('link-item');
  await expect(toggle).not.toBeChecked();

  await applyTagFilter(page, tag);
  await expect(items).toHaveCount(3);
  await select.selectOption('oldest');
  await expect(items.nth(0)).toHaveAttribute('data-link-id', String(saved[0].id));
  await toggle.check();
  await expect(items).toHaveCount(2);
  await expect(items.nth(0)).toHaveAttribute('data-link-id', ids[0]);
  await expect(items.nth(1)).toHaveAttribute('data-link-id', ids[1]);
  await expect(linkItem(page, saved[1].id)).toHaveCount(0);
  await expect(linkItem(page, untagged.id)).toHaveCount(0);
  await expectPageInfo(page, 1, 1);
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  await expect(select).toHaveValue('oldest');
  await shot(page, '1-unread-oldest');
  await aria(page.getByTestId('link-list'), '1-unread-list');

  const res = await request.get(`/api/links?unread=true&order=oldest&tag=${encodeURIComponent(tag)}`);
  expect(res.status()).toBe(200);
  const body = (await res.json()) as { items: Link[] };
  json('2-api-unread', body);
  expect(body.items.map((link) => String(link.id))).toEqual(ids);

  const url = uniqueUrl('verify-unread-new');
  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-save-button').click();
  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(toggle).not.toBeChecked();
  await expect(items.first().getByTestId('link-item-url')).toHaveText(url);
  await shot(page, '3-saved-off');

  await page.reload();
  await expect(toggle).not.toBeChecked();
  await expect(items.first().getByTestId('link-item-url')).toHaveText(url);
  await shot(page, '4-after-reload');
});
