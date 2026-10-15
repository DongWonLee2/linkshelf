import { expect, test, type Page } from '@playwright/test';
import { isLinksList } from './support';

// R-UI-13 Visual QA (plan decisions 1-3, research R1-R2):
// - Baselines are made on CI (ubuntu-24.04) only, so other platforms skip instead of writing -darwin.png files.
// - API answers are fixed JSON taken from the spec 001/002 contract examples, because the shared in-memory DB
//   cannot give a stable list or an empty one. Functional E2E checks the real API; this file checks drawing only.
// - Zone, locale and viewport are pinned in playwright.config.ts; the mouse is moved away before each capture.
test.skip(process.platform !== 'linux', 'Visual baselines are Linux (CI) only');

const contractList = {
  items: [
    {
      id: 3,
      url: 'https://example.com/c',
      memo: null,
      tags: ['java', '스프링'],
      createdAt: '2026-10-14T03:00:02Z',
      read: false,
    },
    {
      id: 1,
      url: 'https://example.com/a',
      memo: '메모',
      tags: ['Java'],
      createdAt: '2026-10-14T03:00:00Z',
      read: true,
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
  hasNext: false,
};

const emptyList = { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false };

const contractSaveErrors = {
  title: 'Bad Request',
  status: 400,
  detail: 'Invalid request content.',
  instance: '/api/links',
  errors: [
    { field: 'url', message: 'http 또는 https URL이어야 합니다' },
    { field: 'memo', message: '500자 이하여야 합니다' },
  ],
};

async function fixListResponse(page: Page, body: object): Promise<void> {
  await page.route(isLinksList, (route) => {
    if (route.request().method() !== 'GET') return route.fallback();
    return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
  });
}

async function capture(page: Page, name: string): Promise<void> {
  await page.mouse.move(-1, -1);
  await expect(page).toHaveScreenshot(name, { fullPage: true });
}

test('VISUAL_R_UI_13_first_screen', async ({ page }) => {
  await fixListResponse(page, contractList);
  await page.goto('/');
  await expect(page.getByTestId('link-item')).toHaveCount(2);

  await capture(page, 'first-screen.png');
});

test('VISUAL_R_UI_13_empty_list', async ({ page }) => {
  await fixListResponse(page, emptyList);
  await page.goto('/');
  await expect(page.getByTestId('link-list-empty')).toBeVisible();

  await capture(page, 'empty-list.png');
});

test('VISUAL_R_UI_13_form_errors', async ({ page }) => {
  await fixListResponse(page, contractList);
  await page.route(isLinksList, (route) => {
    if (route.request().method() !== 'POST') return route.fallback();
    return route.fulfill({
      status: 400,
      contentType: 'application/problem+json',
      body: JSON.stringify(contractSaveErrors),
    });
  });
  await page.goto('/');
  await expect(page.getByTestId('link-item')).toHaveCount(2);

  await page.getByTestId('link-url-input').fill('not a url');
  await page.getByTestId('link-memo-input').fill('메모가 너무 길다고 가정한 입력');
  await page.getByTestId('link-save-button').click();
  await expect(page.locator('[data-testid="link-form-error"][data-field="url"]')).toBeVisible();
  await expect(page.locator('[data-testid="link-form-error"][data-field="memo"]')).toBeVisible();
  // Drop the focus ring from the clicked button so it is not part of the baseline.
  await page.getByTestId('link-save-button').blur();

  await capture(page, 'form-errors.png');
});
