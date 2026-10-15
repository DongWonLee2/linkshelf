import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test';

// Feature Map data isolation: all E2E tests share one in-memory DB, so every test writes only its own unique
// URLs and tags and never assumes the DB is empty.
let counter = 0;

export function uniqueId(): string {
  counter += 1;
  return `${Date.now()}${counter}`;
}

// https://e2e.test/<test name>/<time>. Lower case so server URL normalization leaves it unchanged.
export function uniqueUrl(testSlug: string): string {
  return `https://e2e.test/${testSlug}/${uniqueId()}`;
}

export type Link = {
  id: number;
  url: string;
  memo: string | null;
  tags: string[];
  createdAt: string;
  read: boolean;
};

export async function saveLinkViaApi(
  request: APIRequestContext,
  body: { url: string; memo?: string | null; tags?: string[] },
): Promise<Link> {
  const response = await request.post('/api/links', { data: body });
  expect(response.status(), await response.text()).toBe(201);
  return (await response.json()) as Link;
}

export function linkItem(page: Page, id: number): Locator {
  return page.getByTestId('link-item').and(page.locator(`[data-link-id="${id}"]`));
}

export function isLinksList(url: URL): boolean {
  return url.pathname === '/api/links';
}

export function isMarkRead(url: URL): boolean {
  return /^\/api\/links\/\d+\/read$/.test(url.pathname);
}

export async function applyTagFilter(page: Page, tag: string): Promise<void> {
  await page.getByTestId('tag-filter-input').fill(tag);
  await page.getByTestId('tag-filter-apply').click();
}

// Asserts "current / total" in link-list-page-info and returns the total.
export async function expectPageInfo(page: Page, current: number, total?: number): Promise<void> {
  const totalPattern = total === undefined ? '\\d+' : String(total);
  await expect(page.getByTestId('link-list-page-info')).toHaveText(
    new RegExp(`^\\s*${current}\\s*/\\s*${totalPattern}\\s*$`),
  );
}

// spec 003 clarify Q2: created-at is shown in the user's local time as YYYY-MM-DD HH:mm.
// playwright.config.ts pins the zone to Asia/Seoul (UTC+9, no daylight saving).
export function seoulMinute(isoInstant: string): string {
  const shifted = new Date(Date.parse(isoInstant) + 9 * 60 * 60 * 1000);
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${shifted.getUTCFullYear()}-${pad(shifted.getUTCMonth() + 1)}-${pad(shifted.getUTCDate())} ` +
    `${pad(shifted.getUTCHours())}:${pad(shifted.getUTCMinutes())}`
  );
}
