import { expect, test } from '@playwright/test';
import { applyTagFilter, expectPageInfo, isLinksList, saveLinkViaApi, uniqueId, uniqueUrl, type Link } from './support';

test('E2E_R_UI_07_filters_by_tag_ignoring_case', async ({ page, request }) => {
  // Unique stand-ins for the spec's "Java", "java", "스프링" tags (shared DB).
  const id = uniqueId();
  const upper = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-07-upper'), tags: [`Jv${id}`] });
  await saveLinkViaApi(request, { url: uniqueUrl('r-ui-07-other'), tags: [`스프링${id}`] });
  const lower = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-07-lower'), tags: [`jv${id}`] });

  await page.goto('/');
  await applyTagFilter(page, ` JV${id} `);

  const items = page.getByTestId('link-item');
  await expect(items).toHaveCount(2);
  await expect(items.nth(0)).toHaveAttribute('data-link-id', String(lower.id));
  await expect(items.nth(1)).toHaveAttribute('data-link-id', String(upper.id));
  await expect(page.getByTestId('tag-filter-active')).toBeVisible();
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(`jv${id}`, 'i'));

  // Clearing goes back to the whole list from the first page.
  await page.getByTestId('tag-filter-clear').click();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expectPageInfo(page, 1);
  await expect(items.first()).toHaveAttribute('data-link-id', String(lower.id));
  await expect(items).not.toHaveCount(2);
});

test('E2E_R_UI_07_blank_filter_clears', async ({ page, request }) => {
  const tag = `blank${uniqueId()}`;
  const tagged = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-07-blank-tagged'), tags: [tag] });
  const newest = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-07-blank-newest') });

  const listRequests: URL[] = [];
  page.on('request', (req) => {
    const url = new URL(req.url());
    if (req.method() === 'GET' && isLinksList(url)) listRequests.push(url);
  });

  await page.goto('/');
  const items = page.getByTestId('link-item');

  for (const blank of ['   ', '']) {
    await applyTagFilter(page, tag);
    await expect(page.getByTestId('tag-filter-active')).toBeVisible();
    await expect(items).toHaveCount(1);
    await expect(items.first()).toHaveAttribute('data-link-id', String(tagged.id));

    const before = listRequests.length;
    await applyTagFilter(page, blank);

    await expect(page.getByTestId('tag-filter-active')).toBeHidden();
    await expect(page.getByTestId('tag-filter-error')).toBeHidden();
    await expect(items.first()).toHaveAttribute('data-link-id', String(newest.id));
    // A blank tag is never sent (the API answers 400 to an empty tag).
    expect(listRequests.length).toBeGreaterThan(before);
    for (const url of listRequests.slice(before)) {
      expect(url.searchParams.has('tag')).toBe(false);
    }
  }
});

test('E2E_R_UI_08_shows_filter_error_and_keeps_list', async ({ page, request }) => {
  const tag = `keep${uniqueId()}`;
  const tagged = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-08-keep'), tags: [tag] });

  await page.goto('/');
  await applyTagFilter(page, tag);
  const items = page.getByTestId('link-item');
  await expect(items).toHaveCount(1);

  await applyTagFilter(page, 'a'.repeat(31));

  await expect(page.getByTestId('tag-filter-error')).toBeVisible();
  await expect(items).toHaveCount(1);
  await expect(items.first()).toHaveAttribute('data-link-id', String(tagged.id));
  await expect(page.getByTestId('tag-filter-active')).toBeVisible();
  await expect(page.getByTestId('tag-filter-active')).toContainText(new RegExp(tag, 'i'));
});

test('E2E_R_UI_08_pages_within_filter', async ({ page, request }) => {
  const tag = `pg${uniqueId()}`;
  const saved: Link[] = [];
  for (let i = 0; i < 21; i += 1) {
    saved.push(await saveLinkViaApi(request, { url: uniqueUrl(`r-ui-08-page-${i}`), tags: [tag] }));
  }
  // Newer untagged links must not leak into the filtered pages.
  await saveLinkViaApi(request, { url: uniqueUrl('r-ui-08-page-untagged') });

  await page.goto('/');
  await applyTagFilter(page, tag);

  const items = page.getByTestId('link-item');
  await expectPageInfo(page, 1, 2);
  await expect(items).toHaveCount(20);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[20].id));

  await page.getByTestId('link-list-next').click();
  await expectPageInfo(page, 2, 2);
  await expect(items).toHaveCount(1);
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved[0].id));
  await expect(page.getByTestId('link-list-next')).toBeDisabled();
  await expect(page.getByTestId('tag-filter-active')).toBeVisible();

  await page.getByTestId('link-list-prev').click();
  await expectPageInfo(page, 1, 2);
  await expect(items).toHaveCount(20);
});
