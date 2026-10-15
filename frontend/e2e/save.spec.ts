import { expect, test } from '@playwright/test';
import { applyTagFilter, expectPageInfo, isLinksList, saveLinkViaApi, uniqueId, uniqueUrl } from './support';

test('E2E_R_UI_05_saves_link_and_shows_it_first', async ({ page }) => {
  const url = uniqueUrl('r-ui-05-save');
  await page.goto('/');

  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-memo-input').fill('주말에 읽기 😀');
  await page.getByTestId('link-tags-input').fill('Java, 스프링');
  await page.getByTestId('link-save-button').click();

  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(page.getByTestId('link-url-input')).toHaveValue('');
  await expect(page.getByTestId('link-memo-input')).toHaveValue('');
  await expect(page.getByTestId('link-tags-input')).toHaveValue('');

  const first = page.getByTestId('link-item').first();
  await expect(first.getByTestId('link-item-url')).toHaveText(url);
  await expect(first.getByTestId('link-item-memo')).toHaveText('주말에 읽기 😀');
  await expect(first.getByTestId('link-item-tag')).toHaveText(['Java', '스프링']);

  // clarify Q3: the saved notice stays until the next input starts.
  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await page.getByTestId('link-url-input').fill(uniqueUrl('r-ui-05-next'));
  await expect(page.getByTestId('link-form-saved')).toBeHidden();
});

test('E2E_R_UI_05_save_clears_tag_filter', async ({ page, request }) => {
  const tag = `flt${uniqueId()}`;
  await saveLinkViaApi(request, { url: uniqueUrl('r-ui-05-filtered'), tags: [tag] });
  const url = uniqueUrl('r-ui-05-untagged');

  await page.goto('/');
  await applyTagFilter(page, tag);
  await expect(page.getByTestId('tag-filter-active')).toBeVisible();
  await expect(page.getByTestId('link-item')).toHaveCount(1);

  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-save-button').click();

  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  await expect(page.getByTestId('link-item').first().getByTestId('link-item-url')).toHaveText(url);
});

test('E2E_R_UI_05_save_returns_to_first_page', async ({ page, request }) => {
  // 21 links guarantee a second page; saving from there reloads page 1 with the new link on top (PR #27 review).
  for (let i = 0; i < 21; i += 1) {
    await saveLinkViaApi(request, { url: uniqueUrl(`r-ui-05-page-${i}`) });
  }
  const url = uniqueUrl('r-ui-05-from-page-2');

  await page.goto('/');
  await expect(page.getByTestId('link-list-next')).toBeEnabled();
  await page.getByTestId('link-list-next').click();
  await expectPageInfo(page, 2);

  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-save-button').click();

  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expectPageInfo(page, 1);
  await expect(page.getByTestId('link-item').first().getByTestId('link-item-url')).toHaveText(url);
});

test('E2E_R_UI_04_save_button_disabled_while_saving', async ({ page }) => {
  let posts = 0;
  let release!: () => void;
  const held = new Promise<void>((resolve) => {
    release = resolve;
  });
  // Hold the save request so the "saving" state can be observed.
  await page.route(isLinksList, async (route) => {
    if (route.request().method() !== 'POST') return route.fallback();
    posts += 1;
    await held;
    return route.fallback();
  });

  await page.goto('/');
  const save = page.getByTestId('link-save-button');
  await page.getByTestId('link-url-input').fill(uniqueUrl('r-ui-04-saving'));
  await save.click();

  await expect(save).toBeDisabled();
  // A second press while saving must not send a second request.
  await save.click({ force: true });
  release();

  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(save).toBeEnabled();
  expect(posts).toBe(1);
});

test('E2E_R_UI_06_shows_field_errors', async ({ page, request }) => {
  const longMemo = 'a'.repeat(501);
  // The screen shows the server's own messages, so read them from the API first (nothing is saved on 400).
  const problem = await request.post('/api/links', { data: { url: 'not a url', memo: longMemo, tags: [] } });
  expect(problem.status()).toBe(400);
  const { errors } = (await problem.json()) as { errors: { field: string; message: string }[] };
  const messageOf = (field: string) => errors.find((e) => e.field === field)!.message;

  await page.goto('/');
  const urlInput = page.getByTestId('link-url-input');
  const memoInput = page.getByTestId('link-memo-input');
  const fieldError = (field: string) => page.locator(`[data-testid="link-form-error"][data-field="${field}"]`);

  // Scenario 2: URL error only; input stays.
  await urlInput.fill('not a url');
  await page.getByTestId('link-save-button').click();
  await expect(fieldError('url')).toContainText(messageOf('url'));
  await expect(page.getByTestId('link-form-error')).toHaveCount(1);
  await expect(urlInput).toHaveValue('not a url');
  await expect(page.getByTestId('link-form-saved')).toBeHidden();

  // Scenario 3: URL and memo errors together.
  await memoInput.fill(longMemo);
  await page.getByTestId('link-save-button').click();
  await expect(fieldError('url')).toContainText(messageOf('url'));
  await expect(fieldError('memo')).toContainText(messageOf('memo'));
  await expect(page.getByTestId('link-form-error')).toHaveCount(2);
  await expect(urlInput).toHaveValue('not a url');
  await expect(memoInput).toHaveValue(longMemo);

  // The next save attempt clears earlier errors.
  const url = uniqueUrl('r-ui-06-fixed');
  await urlInput.fill(url);
  await memoInput.fill('');
  await page.getByTestId('link-save-button').click();
  await expect(page.getByTestId('link-form-saved')).toBeVisible();
  await expect(page.getByTestId('link-form-error')).toHaveCount(0);
  await expect(page.getByTestId('link-item').first().getByTestId('link-item-url')).toHaveText(url);
});

test('E2E_R_UI_06_shows_duplicate_notice', async ({ page, request }) => {
  const id = uniqueId();
  const saved = await saveLinkViaApi(request, { url: `https://e2e.test/r-ui-06-dup/${id}/a` });

  await page.goto('/');
  const items = page.getByTestId('link-item');
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved.id));

  // Same link after normalization (scheme/host case, default port).
  await page.getByTestId('link-url-input').fill(`HTTPS://E2E.TEST:443/r-ui-06-dup/${id}/a`);
  await page.getByTestId('link-save-button').click();

  await expect(page.getByTestId('link-form-duplicate')).toBeVisible();
  await expect(page.getByTestId('link-form-saved')).toBeHidden();
  await expect(items.first()).toHaveAttribute('data-link-id', String(saved.id));
});

test('E2E_R_UI_06_shows_tags_error', async ({ page, request }) => {
  const longTag = 't'.repeat(31);
  const url = uniqueUrl('r-ui-06-tags');
  // Server message for a 31-character tag (nothing is saved on 400).
  const problem = await request.post('/api/links', { data: { url, tags: [longTag] } });
  expect(problem.status()).toBe(400);
  const { errors } = (await problem.json()) as { errors: { field: string; message: string }[] };
  const tagsMessage = errors.find((e) => e.field === 'tags')!.message;

  await page.goto('/');
  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-tags-input').fill(`ok, ${longTag}`);
  await page.getByTestId('link-save-button').click();

  const tagsError = page.locator('[data-testid="link-form-error"][data-field="tags"]');
  await expect(tagsError).toContainText(tagsMessage);
  await expect(page.getByTestId('link-form-error')).toHaveCount(1);
  await expect(page.getByTestId('link-tags-input')).toHaveValue(`ok, ${longTag}`);
  await expect(page.getByTestId('link-url-input')).toHaveValue(url);
  await expect(page.getByTestId('link-form-saved')).toBeHidden();
});
