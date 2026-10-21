import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import type { Locator, Page } from '@playwright/test';

const dir = process.env.VERIFY_EVIDENCE as string;

export async function shot(page: Page, name: string): Promise<void> {
  await page.screenshot({ path: join(dir, `${name}.png`), fullPage: true });
}

export async function aria(locator: Locator, name: string): Promise<void> {
  writeFileSync(join(dir, `${name}.aria.txt`), await locator.ariaSnapshot());
}

export function json(name: string, value: unknown): void {
  writeFileSync(join(dir, `${name}.json`), JSON.stringify(value, null, 2));
}
