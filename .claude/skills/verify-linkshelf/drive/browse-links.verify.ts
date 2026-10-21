import { expect, test } from '@playwright/test';
import { applyTagFilter, saveLinkViaApi, seoulMinute, uniqueId, uniqueUrl } from '../../../../frontend/e2e/support';
import { aria, shot } from './evidence';

test('verify browse-links: newest first with fields, empty state', async ({ page, request }) => {
  const older = await saveLinkViaApi(request, { url: uniqueUrl('verify-browse-older'), tags: ['Java'] });
  const newer = await saveLinkViaApi(request, { url: uniqueUrl('verify-browse-newer'), memo: '최신', tags: [] });

  await page.goto('/');
  const items = page.getByTestId('link-item');
  await expect(items.nth(0)).toHaveAttribute('data-link-id', String(newer.id));
  await expect(items.nth(1)).toHaveAttribute('data-link-id', String(older.id));
  await expect(items.nth(0).getByTestId('link-item-memo')).toHaveText('최신');
  await expect(items.nth(0).getByTestId('link-item-created-at')).toHaveText(seoulMinute(newer.createdAt));
  await expect(items.nth(1).getByTestId('link-item-tag')).toHaveText(['Java']);
  await expect(page.getByTestId('link-list-page-info')).toBeVisible();
  await shot(page, '1-list');
  await aria(page.getByTestId('link-list'), '1-list');

  await applyTagFilter(page, `none${uniqueId()}`);
  await expect(page.getByTestId('link-list-empty')).toBeVisible();
  await expect(page.getByTestId('link-list-page-info')).toBeHidden();
  await shot(page, '2-empty');
});
