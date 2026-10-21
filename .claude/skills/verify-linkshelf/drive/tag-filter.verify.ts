import { expect, test } from '@playwright/test';
import { applyTagFilter, linkItem, saveLinkViaApi, uniqueId, uniqueUrl } from '../../../../frontend/e2e/support';
import { shot } from './evidence';

test('verify tag-filter: case-insensitive match, error keeps the filtered list, clear restores the list', async ({ page, request }) => {
  const id = uniqueId();
  const upper = await saveLinkViaApi(request, { url: uniqueUrl('verify-filter-upper'), tags: [`Vf${id}`] });
  const other = await saveLinkViaApi(request, { url: uniqueUrl('verify-filter-other'), tags: [`etc${id}`] });

  await page.goto('/');
  await applyTagFilter(page, ` VF${id} `);
  await expect(page.getByTestId('link-item')).toHaveCount(1);
  await expect(linkItem(page, upper.id)).toBeVisible();
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(`vf${id}`, 'i'));
  await shot(page, '1-filtered');

  await applyTagFilter(page, 'x'.repeat(31));
  await expect(page.getByTestId('tag-filter-error')).toBeVisible();
  await expect(page.getByTestId('link-item')).toHaveCount(1);
  await expect(linkItem(page, upper.id)).toBeVisible();
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(`vf${id}`, 'i'));
  await shot(page, '2-error-keeps-list');

  await page.getByTestId('tag-filter-clear').click();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(linkItem(page, other.id)).toBeVisible();
  await shot(page, '3-cleared');
});
