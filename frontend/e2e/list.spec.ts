import { expect, test } from '@playwright/test';
import {
  applyTagFilter,
  expectPageInfo,
  isLinksList,
  saveLinkViaApi,
  seoulMinute,
  uniqueId,
  uniqueUrl,
  type Link,
} from './support';

test('E2E_R_UI_01_lists_newest_first_with_fields', async ({ page, request }) => {
  const a = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-01-a'), tags: ['Java'] });
  const b = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-01-b'), memo: '주말에 읽기 😀', tags: [] });
  const c = await saveLinkViaApi(request, {
    url: uniqueUrl('r-ui-01-c'),
    memo: '한글 메모',
    tags: ['Java', '스프링'],
  });

  await page.goto('/');

  const items = page.getByTestId('link-item');
  // Tests run one at a time (workers: 1), so the three links just saved are the newest three.
  await expect(items.nth(0)).toHaveAttribute('data-link-id', String(c.id));
  await expect(items.nth(1)).toHaveAttribute('data-link-id', String(b.id));
  await expect(items.nth(2)).toHaveAttribute('data-link-id', String(a.id));

  const expectFields = async (index: number, link: Link) => {
    const item = items.nth(index);
    const url = item.getByTestId('link-item-url');
    await expect(url).toHaveText(link.url);
    await expect(url).toHaveAttribute('href', link.url);
    // Edge case: opens in a new tab and the opened page cannot reach this one.
    await expect(url).toHaveAttribute('target', '_blank');
    await expect(url).toHaveAttribute('rel', /noopener/);
    if (link.memo === null) {
      await expect(item.getByTestId('link-item-memo')).toHaveCount(0);
    } else {
      await expect(item.getByTestId('link-item-memo')).toHaveText(link.memo);
    }
    await expect(item.getByTestId('link-item-tag')).toHaveText(link.tags);
    await expect(item.getByTestId('link-item-created-at')).toHaveText(seoulMinute(link.createdAt));
  };

  await expect(page.getByTestId('link-list')).toBeVisible();
  await expectFields(0, c);
  await expectFields(1, b);
  await expectFields(2, a);
});

test('E2E_R_UI_02_pages_through_links', async ({ page, request }) => {
  // 21 links guarantee at least two pages of 20 (server default size).
  const saved: Link[] = [];
  for (let i = 0; i < 21; i += 1) {
    saved.push(await saveLinkViaApi(request, { url: uniqueUrl(`r-ui-02-${i}`) }));
  }
  const newest = saved[20];
  const oldest = saved[0];

  await page.goto('/');

  const items = page.getByTestId('link-item');
  const prev = page.getByTestId('link-list-prev');
  const next = page.getByTestId('link-list-next');

  await expectPageInfo(page, 1);
  await expect(items).toHaveCount(20);
  await expect(items.first()).toHaveAttribute('data-link-id', String(newest.id));
  // Next enabled first: both buttons are disabled while a page loads, so prev is disabled by the page boundary.
  await expect(next).toBeEnabled();
  await expect(prev).toBeDisabled();

  await next.click();
  await expectPageInfo(page, 2);
  await expect(items.first()).toHaveAttribute('data-link-id', String(oldest.id));
  await expect(prev).toBeEnabled();

  // Walk to the last page; the total depends on what other tests saved into the shared DB.
  const info = await page.getByTestId('link-list-page-info').textContent();
  const total = Number(/\/\s*(\d+)/.exec(info ?? '')?.[1]);
  expect(total).toBeGreaterThanOrEqual(2);
  for (let current = 2; current < total; current += 1) {
    await next.click();
    await expectPageInfo(page, current + 1, total);
  }
  await expectPageInfo(page, total, total);
  // Prev enabled first, so next is disabled by the last page and not by loading.
  await expect(prev).toBeEnabled();
  await expect(next).toBeDisabled();

  await prev.click();
  await expectPageInfo(page, total - 1, total);
});

test('E2E_R_UI_03_shows_empty_state', async ({ page }) => {
  await page.goto('/');
  // The shared DB is never empty on purpose, so the empty state comes from a tag no link has.
  await applyTagFilter(page, `none${uniqueId()}`);

  await expect(page.getByTestId('link-list-empty')).toBeVisible();
  await expect(page.getByTestId('link-item')).toHaveCount(0);
  await expect(page.getByTestId('link-list-page-info')).toBeHidden();
  await expect(page.getByTestId('link-list-prev')).toBeHidden();
  await expect(page.getByTestId('link-list-next')).toBeHidden();
});

test('E2E_R_UI_03_shows_list_error_when_api_fails', async ({ page }) => {
  // Only the list GET fails; everything else still reaches the real backend.
  await page.route(isLinksList, (route) =>
    route.request().method() === 'GET' ? route.abort('failed') : route.fallback(),
  );

  await page.goto('/');

  await expect(page.getByTestId('list-error')).toBeVisible();
  await expect(page.getByTestId('link-item')).toHaveCount(0);
});
