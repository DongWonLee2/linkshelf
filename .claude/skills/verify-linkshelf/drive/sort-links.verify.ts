import { expect, test } from '@playwright/test';
import { applyTagFilter, expectPageInfo, linkItem, saveLinkViaApi, uniqueId, uniqueUrl, type Link } from '../../../../frontend/e2e/support';
import { aria, json, shot } from './evidence';

test('verify sort-links: oldest first keeps the filter, saving resets to newest', async ({ page, request }) => {
  const tag = `so${uniqueId()}`;
  const saved: Link[] = [];
  for (const name of ['a', 'b', 'c']) {
    saved.push(await saveLinkViaApi(request, { url: uniqueUrl(`verify-sort-${name}`), tags: [tag] }));
  }
  const untagged = await saveLinkViaApi(request, { url: uniqueUrl('verify-sort-untagged') });
  const ids = saved.map((link) => String(link.id));

  await page.goto('/');
  const select = page.getByTestId('link-sort-select');
  const items = page.getByTestId('link-item');
  await expect(select).toHaveValue('newest');

  await applyTagFilter(page, tag);
  await expect(items).toHaveCount(3);
  await select.selectOption('oldest');
  await expect(items.nth(0)).toHaveAttribute('data-link-id', ids[0]);
  await expect(items.nth(1)).toHaveAttribute('data-link-id', ids[1]);
  await expect(items.nth(2)).toHaveAttribute('data-link-id', ids[2]);
  await expectPageInfo(page, 1, 1);
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  await expect(linkItem(page, untagged.id)).toHaveCount(0);
  await shot(page, '1-oldest');
  await aria(page.getByTestId('link-list'), '1-oldest-list');

  const res = await request.get(`/api/links?order=oldest&tag=${encodeURIComponent(tag)}`);
  expect(res.status()).toBe(200);
  const body = (await res.json()) as { items: Link[] };
  json('2-api-oldest', body);
  expect(body.items.map((link) => String(link.id))).toEqual(ids);

  const url = uniqueUrl('verify-sort-new');
  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-save-button').click();
  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(select).toHaveValue('newest');
  await expect(items.first().getByTestId('link-item-url')).toHaveText(url);
  await shot(page, '3-saved-newest');

  await page.reload();
  await expect(select).toHaveValue('newest');
  await expect(items.first().getByTestId('link-item-url')).toHaveText(url);
  await shot(page, '4-after-reload');
});
