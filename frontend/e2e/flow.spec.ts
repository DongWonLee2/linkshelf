import { statSync } from 'node:fs';
import { expect, test } from '@playwright/test';
import { applyTagFilter, saveLinkViaApi, uniqueId, uniqueUrl } from './support';

test('E2E_SC_001_full_flow', async ({ page }) => {
  // PRD-01~04 on the screen only: save -> top of list -> filter -> read -> still read after reload.
  const url = uniqueUrl('sc-001-flow');
  const tag = `flow${uniqueId()}`;

  await page.goto('/');
  await page.getByTestId('link-url-input').fill(url);
  await page.getByTestId('link-memo-input').fill('1분 흐름');
  await page.getByTestId('link-tags-input').fill(`${tag}, 스프링`);
  await page.getByTestId('link-save-button').click();
  await expect(page.getByTestId('link-form-saved')).toBeVisible();

  const first = page.getByTestId('link-item').first();
  await expect(first.getByTestId('link-item-url')).toHaveText(url);
  const id = await first.getAttribute('data-link-id');
  expect(id).not.toBeNull();

  await applyTagFilter(page, tag.toUpperCase());
  const items = page.getByTestId('link-item');
  await expect(page.getByTestId('tag-filter-active')).toBeVisible();
  await expect(items).toHaveCount(1);
  await expect(items.first()).toHaveAttribute('data-link-id', id!);

  const toggle = items.first().getByTestId('link-item-read-toggle');
  await toggle.click();
  await expect(toggle).toBeChecked();
  await expect(items.first().getByTestId('link-item-error')).toBeHidden();

  // Reload resets screen state (first page, no filter); the read flag comes back from the server.
  await page.reload();
  await expect(page.getByTestId('tag-filter-active')).toBeHidden();
  const reloaded = page.getByTestId('link-item').and(page.locator(`[data-link-id="${id}"]`));
  await expect(reloaded.getByTestId('link-item-url')).toHaveText(url);
  await expect(reloaded.getByTestId('link-item-read-toggle')).toBeChecked();
});

// R-UI-12 / SC-005 guard: the E2E backend must not touch the local file DB (ADR-0001 ./data/linkshelf in backend/).
// playwright.config.ts starts bootRun with an in-memory H2 URL; this checks the effect, not the setting:
// a write through the API must leave backend/data/linkshelf.mv.db exactly as it was (or absent).
test('E2E_R_UI_12_backend_uses_in_memory_db', async ({ request }, testInfo) => {
  const dbFile = `${testInfo.project.testDir}/../../backend/data/linkshelf.mv.db`;
  const snapshot = () => {
    const stat = statSync(dbFile, { throwIfNoEntry: false });
    return stat === undefined ? 'absent' : `mtime=${stat.mtimeMs} size=${stat.size}`;
  };

  const before = snapshot();
  await saveLinkViaApi(request, { url: uniqueUrl('r-ui-12-guard'), memo: 'in-memory guard', tags: ['guard'] });
  // H2 flushes file stores in the background (about a second), so give a file-backed server time to write.
  await new Promise((resolve) => setTimeout(resolve, 3_000));

  expect(snapshot(), `E2E backend changed ${dbFile}; it must run on jdbc:h2:mem`).toBe(before);
});
