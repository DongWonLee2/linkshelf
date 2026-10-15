import { defineConfig, devices } from '@playwright/test';

// ADR-0005: E2E starts the backend on an in-memory H2 database (never the local file DB) and the Vite dev server.
export default defineConfig({
  testDir: './e2e',
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1,
  globalTimeout: 600_000,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    // spec 003 clarify Q2: created-at is shown in local time, so tests and screenshots fix the zone and locale.
    timezoneId: 'Asia/Seoul',
    locale: 'ko-KR',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } }],
  webServer: [
    {
      command: './gradlew bootRun --console=plain',
      cwd: '../backend',
      url: 'http://localhost:8080/api/links',
      env: { SPRING_DATASOURCE_URL: 'jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1' },
      reuseExistingServer: false,
      timeout: 180_000,
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      reuseExistingServer: false,
      timeout: 60_000,
    },
  ],
});
