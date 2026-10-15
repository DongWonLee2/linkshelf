import { expect, test } from '@playwright/test';
import { isMarkRead, linkItem, saveLinkViaApi, uniqueUrl } from './support';

test('E2E_R_UI_09_marks_read_and_keeps_after_reload', async ({ page, request }) => {
  const link = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-09-read') });

  await page.goto('/');
  const toggle = linkItem(page, link.id).getByTestId('link-item-read-toggle');
  await expect(toggle).not.toBeChecked();

  const marked = page.waitForResponse((res) => isMarkRead(new URL(res.url())) && res.request().method() === 'PUT');
  await toggle.click();
  await expect(toggle).toBeChecked();
  expect((await marked).status()).toBe(200);

  await page.reload();
  await expect(linkItem(page, link.id).getByTestId('link-item-read-toggle')).toBeChecked();
});

test('E2E_R_UI_09_marks_unread_again', async ({ page, request }) => {
  const link = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-09-unread') });
  const put = await request.put(`/api/links/${link.id}/read`, { data: { read: true } });
  expect(put.status()).toBe(200);

  await page.goto('/');
  const toggle = linkItem(page, link.id).getByTestId('link-item-read-toggle');
  await expect(toggle).toBeChecked();

  const marked = page.waitForResponse((res) => isMarkRead(new URL(res.url())) && res.request().method() === 'PUT');
  await toggle.click();
  await expect(toggle).not.toBeChecked();
  expect((await marked).status()).toBe(200);

  await page.reload();
  await expect(linkItem(page, link.id).getByTestId('link-item-read-toggle')).not.toBeChecked();
});

test('E2E_R_UI_10_reverts_checkbox_when_marking_fails', async ({ page, request }) => {
  const link = await saveLinkViaApi(request, { url: uniqueUrl('r-ui-10-revert') });
  let failure: 'not-found' | 'network' = 'not-found';
  // Only the mark-read PUT fails; the list still comes from the real backend.
  await page.route(isMarkRead, (route) => {
    if (failure === 'network') return route.abort('failed');
    return route.fulfill({
      status: 404,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        title: 'Not Found',
        status: 404,
        detail: '링크를 찾을 수 없습니다',
        instance: `/api/links/${link.id}/read`,
        id: link.id,
      }),
    });
  });

  await page.goto('/');
  const item = linkItem(page, link.id);
  const toggle = item.getByTestId('link-item-read-toggle');
  await expect(toggle).not.toBeChecked();

  // click(), not check(): check() fails when the box flips back, which is the expected behavior here.
  const firstAttempt = page.waitForRequest((req) => isMarkRead(new URL(req.url())));
  await toggle.click();
  await firstAttempt;
  await expect(toggle).not.toBeChecked();
  await expect(item.getByTestId('link-item-error')).toBeVisible();

  failure = 'network';
  const secondAttempt = page.waitForRequest((req) => isMarkRead(new URL(req.url())));
  await toggle.click();
  await secondAttempt;
  await expect(toggle).not.toBeChecked();
  await expect(item.getByTestId('link-item-error')).toBeVisible();

  // Nothing was stored.
  await page.unroute(isMarkRead);
  await page.reload();
  await expect(linkItem(page, link.id).getByTestId('link-item-read-toggle')).not.toBeChecked();
});
