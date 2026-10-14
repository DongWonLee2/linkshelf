// playwright.config.ts reads process.env.CI. Declared here instead of adding @types/node (ADR-0005: minimal packages).
// Delete this file if @types/node is ever added, or the two declarations will clash.
declare const process: { env: Record<string, string | undefined> };
