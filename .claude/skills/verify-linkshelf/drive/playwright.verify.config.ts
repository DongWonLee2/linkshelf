import { defineConfig, devices } from '@playwright/test';
import { join } from 'node:path';

// Drives an instance started by scripts/launch.sh, so there is no webServer here.
const evidence = process.env.VERIFY_EVIDENCE;
if (!evidence) throw new Error('VERIFY_EVIDENCE is not set; run scripts/drive.sh');

export default defineConfig({
  testDir: __dirname,
  testMatch: '*.verify.ts',
  retries: 0,
  workers: 1,
  outputDir: join(evidence, 'test-results'),
  reporter: [['list'], ['json', { outputFile: join(evidence, 'report.json') }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on',
    screenshot: 'on',
    timezoneId: 'Asia/Seoul',
    locale: 'ko-KR',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } }],
});
