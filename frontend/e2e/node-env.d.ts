// playwright.config.ts reads process.env.CI and visual.spec.ts reads process.platform; flow.spec.ts stats the local
// H2 file (R-UI-12). Declared here instead of adding @types/node (ADR-0005: minimal packages).
// Delete this file and .claude/skills/verify-linkshelf/drive/node-env.d.ts if @types/node is ever added, or the declarations will clash.
declare const process: { env: Record<string, string | undefined>; platform: string };

declare module 'node:fs' {
  export function statSync(
    path: string,
    options: { throwIfNoEntry: false },
  ): { mtimeMs: number; size: number } | undefined;
}
