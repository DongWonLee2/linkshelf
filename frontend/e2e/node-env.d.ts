// playwright.config.ts reads process.env.CI. Declared here instead of adding @types/node (ADR-0005: minimal packages).
declare const process: { env: Record<string, string | undefined> };
