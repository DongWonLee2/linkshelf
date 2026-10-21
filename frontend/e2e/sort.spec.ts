import { expect, test, type Page } from '@playwright/test';
import {
  applyTagFilter,
  expectPageInfo,
  isLinksList,
  saveLinkViaApi,
  uniqueId,
  uniqueUrl,
  type Link,
} from './support';

// The shared DB holds other tests' links, so the oldest link overall is not ours. Order and count checks run on
// a unique tag's filtered list (tasks.md common pitfall, R-UI-12).

function recordListRequests(page: Page): URL[] {
  const urls: URL[] = [];
  page.on('request', (req) => {
    const url = new URL(req.url());
    if (req.method() === 'GET' && isLinksList(url)) urls.push(url);
  });
  return urls;
}

async function saveTagged(page: Page, tag: string, slug: string, count: number): Promise<Link[]> {
  const saved: Link[] = [];
  for (let i = 0; i < count; i += 1) {
    saved.push(await saveLinkViaApi(page.request, { url: uniqueUrl(`${slug}-${i}`), tags: [tag] }));
  }
  return saved;
}

test('E2E_R_UI_18_sort_select_defaults_to_newest_and_resets_on_reload', async ({ page }) => {
  const tag = `def${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-18', 3);
  const requests = recordListRequests(page);

  await page.goto('/');
  const select = page.getByTestId('link-sort-select');
  // The select is named by its label "정렬".
  await expect(page.getByRole('combobox', { name: '정렬' })).toHaveAttribute('data-testid', 'link-sort-select');
  await expect(select).toHaveValue('newest');
  await expect(select.locator('option:checked')).toHaveText('최신순');

  await applyTagFilter(page, tag);
  const items = page.getByTestId('link-item');
  await expect(items).toHaveCount(3);
  await expect(items.nth(0)).toHaveAttribute('data-link-id', String(saved[2].id));
  await expect(items.nth(2)).toHaveAttribute('data-link-id', String(saved[0].id));
  // The order is always sent, newest by default.
  expect(requests.length).toBeGreaterThan(0);
  for (const url of requests) expect(url.searchParams.get('order')).toBe('newest');

  await select.selectOption('oldest');
  await expect(items.nth(0)).toHaveAttribute('data-link-id', String(saved[0].id));
  await expect(select).toHaveValue('oldest');
  // The order is not kept in the address.
  expect(new URL(page.url()).search).toBe('');

  const beforeReload = requests.length;
  await page.reload();
  await expect(select).toHaveValue('newest');
  await expect(page.getByTestId('link-list')).toBeVisible();
  expect(requests.length).toBeGreaterThan(beforeReload);
  for (const url of requests.slice(beforeReload)) expect(url.searchParams.get('order')).toBe('newest');
});

test('E2E_R_UI_19_changing_order_reloads_first_page_keeping_filter', async ({ page }) => {
  const tag = `ord${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-19-first-page', 21);
  // A newer untagged link must not leak into the filtered list.
  await saveLinkViaApi(page.request, { url: uniqueUrl('r-ui-19-first-page-untagged') });
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expectPageInfo(page, 1, 2);
  await page.getByTestId('link-list-next').click();
  await expectPageInfo(page, 2, 2);

  const before = requests.length;
  await page.getByTestId('link-sort-select').selectOption('oldest');

  const items = page.getByTestId('link-item');
  await expectPageInfo(page, 1, 2);
  await expect(items).toHaveCount(20);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[0].id));
  await expect(items.last()).toHaveAttribute('data-link-id', String(saved[19].id));
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  await expect(page.getByTestId('link-sort-select')).toHaveValue('oldest');

  const sent = requests.slice(before);
  expect(sent).toHaveLength(1);
  expect(sent[0].searchParams.get('order')).toBe('oldest');
  expect(sent[0].searchParams.get('page')).toBe('0');
  expect(sent[0].searchParams.get('tag')).toBe(tag);
});

test('E2E_R_UI_19_paging_and_filter_keep_order', async ({ page }) => {
  const tag = `keep${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-19-keep', 21);
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  // Wait for the filtered list: an order change sent before it lands would start from the unfiltered state.
  await expectPageInfo(page, 1, 2);
  const select = page.getByTestId('link-sort-select');
  const items = page.getByTestId('link-item');
  const beforeSelect = requests.length;
  await select.selectOption('oldest');
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[0].id));

  // Paging keeps the order: page 2 of the oldest-first list holds the newest of the 21.
  await page.getByTestId('link-list-next').click();
  await expectPageInfo(page, 2, 2);
  await expect(items).toHaveCount(1);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[20].id));
  await page.getByTestId('link-list-prev').click();
  await expectPageInfo(page, 1, 2);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[0].id));

  // Clearing the filter keeps the order.
  const beforeClear = requests.length;
  await page.getByTestId('tag-filter-clear').click();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(select).toHaveValue('oldest');
  await expectPageInfo(page, 1);
  const clearSent = requests.slice(beforeClear);
  expect(clearSent).toHaveLength(1);
  expect(clearSent[0].searchParams.get('order')).toBe('oldest');
  expect(clearSent[0].searchParams.has('tag')).toBe(false);

  // Applying the filter again keeps the order too.
  const beforeApply = requests.length;
  await applyTagFilter(page, tag);
  await expect(page.getByTestId('tag-filter-active')).toBeVisible();
  await expect(items).toHaveCount(20);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[0].id));
  await expect(select).toHaveValue('oldest');
  const applySent = requests.slice(beforeApply);
  expect(applySent).toHaveLength(1);
  expect(applySent[0].searchParams.get('order')).toBe('oldest');

  // Every request after choosing "oldest" carried it, the paging ones included.
  expect(requests.length - beforeSelect).toBe(5);
  for (const url of requests.slice(beforeSelect)) expect(url.searchParams.get('order')).toBe('oldest');
});

test('E2E_R_UI_19_order_change_does_not_move_focus_or_scroll', async ({ page }) => {
  // R-UI-16/17 scroll and move focus on paging only. 40 links give a long page, so a scroll would show.
  const tag = `still${uniqueId()}`;
  const links = await saveTagged(page, tag, 'r-ui-19-still', 40);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expectPageInfo(page, 1, 2);

  const select = page.getByTestId('link-sort-select');
  // Put the select near the viewport top with the list area's top (its heading) above it, so scrolling the
  // list area into view would change the scroll position.
  await select.evaluate((el) => window.scrollTo(0, el.getBoundingClientRect().top + window.scrollY - 4));
  await expect(page.getByRole('heading', { name: '저장한 링크' })).not.toBeInViewport();
  const scrollBefore = await page.evaluate(() => window.scrollY);
  expect(scrollBefore).toBeGreaterThan(0);

  await select.focus();
  await select.selectOption('oldest');
  await expect(page.getByTestId('link-item').first()).toHaveAttribute('data-link-id', String(links[0].id));
  await expectPageInfo(page, 1, 2);

  await expect(select).toBeFocused();
  await expect(page.getByRole('link', { name: links[0].url, exact: true })).not.toBeFocused();
  expect(await page.evaluate(() => window.scrollY)).toBe(scrollBefore);
});

test('E2E_R_UI_20_saving_resets_order_to_newest', async ({ page }) => {
  const tag = `rst${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-20', 21);
  const url = uniqueUrl('r-ui-20-new');
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expectPageInfo(page, 1, 2);
  const select = page.getByTestId('link-sort-select');
  const items = page.getByTestId('link-item');
  await select.selectOption('oldest');
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[0].id));

  const beforeSave = requests.length;
  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-save-button').click();

  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(select).toHaveValue('newest');
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expectPageInfo(page, 1);
  await expect(items.first().getByTestId('link-item-url')).toHaveText(url);

  // Next page is newest first too. Tests run one at a time (workers: 1), so the 21 links and the new one are the
  // newest 22: page 1 is the new link and saved[20..2], page 2 starts with saved[1].
  await page.getByTestId('link-list-next').click();
  await expectPageInfo(page, 2);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[1].id));

  const sent = requests.slice(beforeSave);
  expect(sent.length).toBeGreaterThanOrEqual(2);
  for (const sentUrl of sent) expect(sentUrl.searchParams.get('order')).toBe('newest');
});
