import { expect, test, type Page, type Response } from '@playwright/test';
import {
  applyTagFilter,
  isLinksList,
  isMarkRead,
  linkItem,
  saveLinkViaApi,
  uniqueId,
  uniqueUrl,
  type Link,
} from './support';

// Issue #51. R-UI-24 in the reverse order of unread.spec.ts: the unread toggle's request is still loading when the
// user changes the order or saves (or tries the tag filter). The toggle's request is delayed so the two really overlap;
// no wait is added before the second action (spec 005 research R5). After the second action the test waits for the
// delayed response too, so a stale response that overwrote the screen would fail the screen assertions.

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

// Delays the matching list requests. Returns { settled }: the first delayed response, so the test can make sure the
// earlier request finished before it checks the screen. (Wrapped in an object: an async function returning a bare
// promise would wait for it, and the test would wait out the race before starting it.)
async function delayListRequests(
  page: Page,
  when: (url: URL) => boolean,
): Promise<{ settled: Promise<Response> }> {
  await page.route(isLinksList, async (route) => {
    if (when(new URL(route.request().url()))) await new Promise((resolve) => setTimeout(resolve, 800));
    await route.continue();
  });
  return { settled: page.waitForResponse((res) => isLinksList(new URL(res.url())) && when(new URL(res.url()))) };
}

// The tag filter's apply and clear buttons are disabled while a list request loads (TagFilter busy), so the tag
// cannot overlap the toggle's request from the screen. This test pins that block: no tag request is sent while the
// toggle loads (button click or Enter), and the tag request sent afterwards keeps the unread value.
test('E2E_R_UI_24_tag_apply_or_clear_blocked_while_toggle_loading_then_keeps_unread', async ({ page }) => {
  const tag = `revtag${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-24-rev-tag', 3);
  await markReadViaApi(page, saved[0]);
  // Newest of all, and read: shown first whenever unread is off and no tag is applied.
  const newestRead = await saveLinkViaApi(page.request, { url: uniqueUrl('r-ui-24-rev-tag-read') });
  await markReadViaApi(page, newestRead);
  const requests = recordListRequests(page);
  const items = page.getByTestId('link-item');

  await page.goto('/');
  await expect(linkItem(page, newestRead.id)).toBeVisible();

  // (1) toggle on (delayed) -> try to apply the tag at once.
  const toggleOn = await delayListRequests(page, (url) => url.searchParams.get('unread') === 'true');
  const beforeOn = requests.length;
  await unreadToggle(page).check();
  await page.getByTestId('tag-filter-input').fill(tag);
  await expect(page.getByTestId('tag-filter-apply')).toBeDisabled();
  await page.getByTestId('tag-filter-input').press('Enter');
  await toggleOn.settled;
  expect(requests.slice(beforeOn)).toHaveLength(1);

  await page.getByTestId('tag-filter-apply').click();
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  await expect(unreadToggle(page)).toBeChecked();
  await expect(items).toHaveCount(2);
  await expect(linkItem(page, saved[0].id)).toHaveCount(0);
  const applied = requests[requests.length - 1];
  expect(applied.searchParams.get('tag')).toBe(tag);
  expect(applied.searchParams.get('unread')).toBe('true');

  // (2) toggle off (delayed) -> try to clear the tag at once.
  await page.unrouteAll({ behavior: 'wait' });
  const toggleOff = await delayListRequests(page, (url) => url.searchParams.get('unread') !== 'true');
  const beforeOff = requests.length;
  await unreadToggle(page).uncheck();
  await expect(page.getByTestId('tag-filter-clear')).toBeDisabled();
  await toggleOff.settled;
  expect(requests.slice(beforeOff)).toHaveLength(1);

  await page.getByTestId('tag-filter-clear').click();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(unreadToggle(page)).not.toBeChecked();
  await expect(page.getByTestId('list-error')).toBeHidden();
  await expect(items.first()).toHaveAttribute('data-link-id', String(newestRead.id));
  const cleared = requests[requests.length - 1];
  expect(cleared.searchParams.has('tag')).toBe(false);
  expect(cleared.searchParams.get('unread')).not.toBe('true');
});

test('E2E_R_UI_24_order_change_while_toggle_loading_keeps_unread', async ({ page }) => {
  const tag = `revord${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-24-rev-order', 3);
  await markReadViaApi(page, saved[0]);
  const requests = recordListRequests(page);
  const items = page.getByTestId('link-item');

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expect(items).toHaveCount(3);

  // Only the toggle's request (still newest) is delayed; the order change's request goes through.
  const toggleOn = await delayListRequests(
    page,
    (url) => url.searchParams.get('unread') === 'true' && url.searchParams.get('order') === 'newest',
  );
  await unreadToggle(page).check();
  await page.getByTestId('link-sort-select').selectOption('oldest');
  await toggleOn.settled;

  await expect(unreadToggle(page)).toBeChecked();
  await expect(page.getByTestId('link-sort-select')).toHaveValue('oldest');
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
  await expect(items).toHaveCount(2);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[1].id));
  const last = requests[requests.length - 1];
  expect(last.searchParams.get('order')).toBe('oldest');
  expect(last.searchParams.get('unread')).toBe('true');
  expect(last.searchParams.get('tag')).toBe(tag);
});

test('E2E_R_UI_24_save_while_toggle_loading_turns_unread_off', async ({ page }) => {
  const tag = `revsave${uniqueId()}`;
  await saveTagged(page, tag, 'r-ui-24-rev-save', 2);
  const url = uniqueUrl('r-ui-24-rev-save-new');
  const requests = recordListRequests(page);

  await page.goto('/');
  await applyTagFilter(page, tag);
  await page.getByTestId('link-sort-select').selectOption('oldest');
  await expect(page.getByTestId('link-item')).toHaveCount(2);

  const toggleOn = await delayListRequests(page, (u) => u.searchParams.get('unread') === 'true');
  await unreadToggle(page).check();
  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-save-button').click();
  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await toggleOn.settled;

  await expect(unreadToggle(page)).not.toBeChecked();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(page.getByTestId('link-sort-select')).toHaveValue('newest');
  await expect(page.getByTestId('link-item').first().getByTestId('link-item-url')).toHaveText(url);
  const last = requests[requests.length - 1];
  expect(last.searchParams.has('tag')).toBe(false);
  expect(last.searchParams.get('order')).toBe('newest');
  expect(last.searchParams.get('unread')).not.toBe('true');
});

test('E2E_R_UI_25_marking_read_fails_with_unread_on_reverts_and_keeps_item', async ({ page }) => {
  const tag = `markfail${uniqueId()}`;
  const saved = await saveTagged(page, tag, 'r-ui-25-fail', 2);
  const requests = recordListRequests(page);
  let failure: 'not-found' | 'network' = 'not-found';
  await page.route(isMarkRead, (route) => {
    if (failure === 'network') return route.abort('failed');
    return route.fulfill({
      status: 404,
      contentType: 'application/problem+json',
      body: JSON.stringify({ title: 'Not Found', status: 404, detail: '링크를 찾을 수 없습니다' }),
    });
  });

  await page.goto('/');
  await applyTagFilter(page, tag);
  await unreadToggle(page).check();
  await expect(page.getByTestId('link-item')).toHaveCount(2);
  const listRequestsBefore = requests.length;

  const item = linkItem(page, saved[1].id);
  const toggle = item.getByTestId('link-item-read-toggle');
  for (const kind of ['not-found', 'network'] as const) {
    failure = kind;
    const attempt = page.waitForRequest((req) => isMarkRead(new URL(req.url())));
    // click(), not check(): check() fails when the box flips back, which is the expected behavior here.
    await toggle.click();
    await attempt;
    await expect(toggle).not.toBeChecked();
    await expect(item.getByTestId('link-item-error')).toBeVisible();
    await expect(item).toBeVisible();
    await expect(page.getByTestId('link-item')).toHaveCount(2);
  }

  await expect(unreadToggle(page)).toBeChecked();
  expect(requests.length).toBe(listRequestsBefore);
});
