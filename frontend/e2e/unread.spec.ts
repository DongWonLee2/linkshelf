import { expect, test, type Page } from '@playwright/test';
import {
  applyTagFilter,
  expectPageInfo,
  isLinksList,
  linkItem,
  saveLinkViaApi,
  uniqueId,
  uniqueUrl,
  type Link,
} from './support';

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

async function markReadViaApi(page: Page, link: Link): Promise<void> {
  const response = await page.request.put(`/api/links/${link.id}/read`, { data: { read: true } });
  expect(response.status(), await response.text()).toBe(200);
}

function unreadToggle(page: Page) {
  return page.getByTestId('link-unread-only-toggle');
}

test('E2E_R_UI_21_toggle_defaults_off_and_resets_on_reload', async ({ page }) => {
  const requests = recordListRequests(page);

  await page.goto('/');
  await expect(page.getByRole('checkbox', { name: '안 읽은 것만', exact: true })).toHaveAttribute(
    'data-testid',
    'link-unread-only-toggle',
  );
  await expect(unreadToggle(page)).not.toBeChecked();
  await expect(page.getByTestId('link-list')).toBeVisible();
  for (const url of requests) expect(url.searchParams.has('unread')).toBe(false);

  const beforeCheck = requests.length;
  await unreadToggle(page).check();
  await expect(unreadToggle(page)).toBeChecked();
  await expect.poll(() => requests.length).toBeGreaterThan(beforeCheck);
  expect(requests[requests.length - 1].searchParams.get('unread')).toBe('true');
  expect(new URL(page.url()).search).toBe('');

  const beforeReload = requests.length;
  await page.reload();
  await expect(unreadToggle(page)).not.toBeChecked();
  await expect(page.getByTestId('link-list')).toBeVisible();
  expect(requests.length).toBeGreaterThan(beforeReload);
  for (const url of requests.slice(beforeReload)) expect(url.searchParams.has('unread')).toBe(false);
});

test('E2E_R_UI_22_toggle_reloads_first_page_keeping_tag_and_order', async ({ page }) => {
  const tag = `unr${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-22-first-page', 23);
  await markReadViaApi(page, saved[0]);
  await markReadViaApi(page, saved[22]);
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await page.getByTestId('link-sort-select').selectOption('oldest');
  await expectPageInfo(page, 1, 2);
  await page.getByTestId('link-list-next').click();
  await expectPageInfo(page, 2, 2);

  const before = requests.length;
  await unreadToggle(page).check();

  const items = page.getByTestId('link-item');
  await expectPageInfo(page, 1, 2);
  await expect(items).toHaveCount(20);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[1].id));
  await expect(page.getByTestId('link-item-read-toggle').and(page.locator(':checked'))).toHaveCount(0);
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  await expect(page.getByTestId('link-sort-select')).toHaveValue('oldest');

  const sent = requests.slice(before);
  expect(sent).toHaveLength(1);
  expect(sent[0].searchParams.get('unread')).toBe('true');
  expect(sent[0].searchParams.get('page')).toBe('0');
  expect(sent[0].searchParams.get('tag')).toBe(tag);
  expect(sent[0].searchParams.get('order')).toBe('oldest');

  const beforeOff = requests.length;
  await unreadToggle(page).uncheck();
  await expectPageInfo(page, 1, 2);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[0].id));
  const off = requests.slice(beforeOff);
  expect(off).toHaveLength(1);
  expect(off[0].searchParams.get('unread')).not.toBe('true');
  expect(off[0].searchParams.get('tag')).toBe(tag);
  expect(off[0].searchParams.get('order')).toBe('oldest');
});

test('E2E_R_UI_22_paging_tag_and_order_keep_unread', async ({ page }) => {
  const tag = `keepunr${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-22-keep', 22);
  await markReadViaApi(page, saved[21]);
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expectPageInfo(page, 1, 2);
  const before = requests.length;
  await unreadToggle(page).check();
  await expect(linkItem(page, saved[21].id)).toHaveCount(0);

  await page.getByTestId('link-list-next').click();
  await expectPageInfo(page, 2, 2);
  await page.getByTestId('link-sort-select').selectOption('oldest');
  await expectPageInfo(page, 1, 2);
  await page.getByTestId('tag-filter-clear').click();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await applyTagFilter(page, tag);
  await expect(page.getByTestId('tag-filter-active')).toBeVisible();
  await expectPageInfo(page, 1, 2);

  await expect(unreadToggle(page)).toBeChecked();
  const sent = requests.slice(before);
  expect(sent).toHaveLength(5);
  for (const url of sent) expect(url.searchParams.get('unread')).toBe('true');
  await expect(linkItem(page, saved[21].id)).toHaveCount(0);
});

test('E2E_R_UI_22_toggle_does_not_move_focus_or_scroll', async ({ page }) => {
  const tag = `stillunr${uniqueId()}`;
  const links = await saveTagged(page, tag, 'r-ui-22-still', 40);
  await markReadViaApi(page, links[39]);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expectPageInfo(page, 1, 2);

  const toggle = unreadToggle(page);
  await toggle.evaluate((el) => window.scrollTo(0, el.getBoundingClientRect().top + window.scrollY - 4));
  await expect(page.getByRole('heading', { name: '저장한 링크' })).not.toBeInViewport();
  const scrollBefore = await page.evaluate(() => window.scrollY);
  expect(scrollBefore).toBeGreaterThan(0);

  await toggle.focus();
  await toggle.check();
  await expect(page.getByTestId('link-item').first()).toHaveAttribute('data-link-id', String(links[38].id));
  await expectPageInfo(page, 1, 2);

  await expect(toggle).toBeFocused();
  expect(await page.evaluate(() => window.scrollY)).toBe(scrollBefore);
});

test('E2E_R_UI_23_saving_turns_unread_off', async ({ page }) => {
  const tag = `saveunr${uniqueId()}`;
  await saveTagged(page, tag, 'r-ui-23', 2);
  const url = uniqueUrl('r-ui-23-new');
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await unreadToggle(page).check();
  await expect(page.getByTestId('link-item')).toHaveCount(2);

  const beforeSave = requests.length;
  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-save-button').click();

  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(unreadToggle(page)).not.toBeChecked();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(page.getByTestId('link-sort-select')).toHaveValue('newest');
  await expect(page.getByTestId('link-item').first().getByTestId('link-item-url')).toHaveText(url);
  const sent = requests.slice(beforeSave);
  expect(sent.length).toBeGreaterThanOrEqual(1);
  for (const sentUrl of sent) expect(sentUrl.searchParams.get('unread')).not.toBe('true');
});

test('E2E_R_UI_25_marking_read_keeps_item_until_reload', async ({ page }) => {
  const tag = `markunr${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-25-keep', 2);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await unreadToggle(page).check();
  await expect(page.getByTestId('link-item')).toHaveCount(2);

  const item = linkItem(page, saved[1].id);
  const markResponse = page.waitForResponse((res) => res.request().method() === 'PUT');
  await item.getByTestId('link-item-read-toggle').check();
  await markResponse;
  await expect(item).toBeVisible();
  await expect(item.getByTestId('link-item-read-toggle')).toBeChecked();

  await unreadToggle(page).uncheck();
  await expect(page.getByTestId('link-item')).toHaveCount(2);
  await unreadToggle(page).check();
  await expect(page.getByTestId('link-item')).toHaveCount(1);
  await expect(linkItem(page, saved[1].id)).toHaveCount(0);
});

test('E2E_R_UI_25_page_shift_after_marking_read', async ({ page }) => {
  const tag = `shift${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-25-shift', 21);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await unreadToggle(page).check();
  await expectPageInfo(page, 1, 2);
  await expect(linkItem(page, saved[0].id)).toHaveCount(0);

  const markResponse = page.waitForResponse((res) => res.request().method() === 'PUT');
  await linkItem(page, saved[20].id).getByTestId('link-item-read-toggle').check();
  await markResponse;
  await page.getByTestId('link-list-next').click();

  await expect(page.getByTestId('link-list-empty')).toBeVisible();
  await expect(linkItem(page, saved[0].id)).toHaveCount(0);
  await expect(page.getByTestId('link-list-prev')).toHaveCount(0);
  await expect(page.getByTestId('link-list-next')).toHaveCount(0);

  await unreadToggle(page).uncheck();
  await expectPageInfo(page, 1, 2);
  await unreadToggle(page).check();
  await expectPageInfo(page, 1, 1);
  await expect(linkItem(page, saved[0].id)).toBeVisible();
});

test('E2E_R_UI_22_list_error_keeps_chosen_toggle', async ({ page }) => {
  const requests = recordListRequests(page);
  await page.goto('/');
  await expect(page.getByTestId('link-list')).toBeVisible();
  await page.route(isLinksList, (route) =>
    new URL(route.request().url()).searchParams.get('unread') === 'true'
      ? route.fulfill({ status: 500, body: '' })
      : route.continue(),
  );

  await unreadToggle(page).check();
  await expect(page.getByTestId('list-error')).toBeVisible();
  await expect(unreadToggle(page)).toBeChecked();

  const beforeRetry = requests.length;
  await unreadToggle(page).uncheck();
  await expect(page.getByTestId('list-error')).toBeHidden();
  await expect(page.getByTestId('link-list')).toBeVisible();
  const retry = requests.slice(beforeRetry);
  expect(retry).toHaveLength(1);
  expect(retry[0].searchParams.get('unread')).not.toBe('true');
});

test('E2E_R_UI_22_empty_result_shows_empty_notice', async ({ page }) => {
  const tag = `emptyunr${uniqueId()}`;
  const [link] = await saveTagged(page, tag, 'r-ui-22-empty', 1);
  await markReadViaApi(page, link);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expect(page.getByTestId('link-item')).toHaveCount(1);
  await unreadToggle(page).check();

  await expect(page.getByTestId('link-list-empty')).toBeVisible();
  await expect(page.getByTestId('list-error')).toBeHidden();
  await expect(unreadToggle(page)).toBeChecked();
});

// R-UI-24: each race overlaps for real (the earlier request is delayed). Do not add waits to make these pass
// (spec 005 research R5: a wait that avoids a race hides it).
async function delayListRequests(page: Page, when: (url: URL) => boolean): Promise<void> {
  await page.route(isLinksList, async (route) => {
    if (when(new URL(route.request().url()))) await new Promise((resolve) => setTimeout(resolve, 800));
    await route.continue();
  });
}

test('E2E_R_UI_24_toggle_right_after_tag_apply_or_clear_keeps_new_tag', async ({ page }) => {
  const tag = `raceunr${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-24-tag', 3);
  await markReadViaApi(page, saved[0]);
  const untagged = await saveLinkViaApi(page.request, { url: uniqueUrl('r-ui-24-tag-untagged') });
  const requests = recordListRequests(page);

  await page.goto('/');
  await expect(page.getByTestId('link-list')).toBeVisible();
  await delayListRequests(page, (url) => url.searchParams.get('unread') !== 'true');

  await applyTagFilter(page, tag);
  await unreadToggle(page).check();

  const items = page.getByTestId('link-item');
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  await expect(items).toHaveCount(2);
  await expect(linkItem(page, saved[0].id)).toHaveCount(0);
  await expect(linkItem(page, untagged.id)).toHaveCount(0);
  const last = requests[requests.length - 1];
  expect(last.searchParams.get('tag')).toBe(tag);
  expect(last.searchParams.get('unread')).toBe('true');

  await page.unrouteAll({ behavior: 'wait' });
  await delayListRequests(page, (url) => url.searchParams.get('unread') === 'true');
  await page.getByTestId('tag-filter-clear').click();
  await unreadToggle(page).uncheck();

  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(unreadToggle(page)).not.toBeChecked();
  await expect(page.getByTestId('list-error')).toBeHidden();
  const cleared = requests[requests.length - 1];
  expect(cleared.searchParams.has('tag')).toBe(false);
  expect(cleared.searchParams.get('unread')).not.toBe('true');
});

test('E2E_R_UI_24_order_change_right_after_toggle_keeps_unread', async ({ page }) => {
  const tag = `raceord${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-24-order', 3);
  await markReadViaApi(page, saved[0]);
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expect(page.getByTestId('link-item')).toHaveCount(3);
  await delayListRequests(page, (url) => url.searchParams.get('order') === 'newest');

  await unreadToggle(page).check();
  await page.getByTestId('link-sort-select').selectOption('oldest');

  const items = page.getByTestId('link-item');
  await expect(items).toHaveCount(2);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[1].id));
  await expect(unreadToggle(page)).toBeChecked();
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  const last = requests[requests.length - 1];
  expect(last.searchParams.get('order')).toBe('oldest');
  expect(last.searchParams.get('unread')).toBe('true');
  expect(last.searchParams.get('tag')).toBe(tag);
});

test('E2E_R_UI_24_toggle_right_after_save_keeps_filters_cleared', async ({ page }) => {
  const tag = `racesaveunr${uniqueId()}`;
  await saveTagged(page, tag, 'r-ui-24-save', 2);
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await page.getByTestId('link-sort-select').selectOption('oldest');
  await unreadToggle(page).check();
  await expect(page.getByTestId('link-item')).toHaveCount(2);
  await delayListRequests(page, (url) => !url.searchParams.has('tag') && url.searchParams.get('unread') !== 'true');

  await page.getByTestId('link-url-input').fill(uniqueUrl('r-ui-24-save-new'));
  await page.getByTestId('link-save-button').click();
  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await unreadToggle(page).check();

  await expect(unreadToggle(page)).toBeChecked();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(page.getByTestId('link-sort-select')).toHaveValue('newest');
  const last = requests[requests.length - 1];
  expect(last.searchParams.has('tag')).toBe(false);
  expect(last.searchParams.get('order')).toBe('newest');
  expect(last.searchParams.get('unread')).toBe('true');
});
