import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { tmpdir } from "node:os";
import { join } from "node:path";

// Exercise the real Istanbul loader used by Jest, including its overridden
// YAML dependency. Version 4 retains load(), but removes unsafe JS YAML tags.
const requireHere = createRequire(__filename);
const { loadNycConfig } = requireHere("@istanbuljs/load-nyc-config") as {
  loadNycConfig(options: {
    cwd: string;
    nycrcPath: string;
  }): Promise<Record<string, unknown>>;
};

describe("coverage configuration dependency compatibility", () => {
  let directory: string;

  beforeEach(async () => {
    directory = await mkdtemp(join(tmpdir(), "wiccan-coverage-config-"));
    await writeFile(join(directory, "package.json"), "{}");
  });

  afterEach(async () => {
    await rm(directory, { recursive: true, force: true });
  });

  it.each(["yml", "yaml"])("loads .nycrc.%s through the real loader", async (extension) => {
    const filename = `.nycrc.${extension}`;
    await writeFile(
      join(directory, filename),
      "all: true\ninclude:\n  - src/**/*.ts\nexclude:\n  - src/__tests__/**\nextension: .ts\n",
    );

    await expect(loadNycConfig({ cwd: directory, nycrcPath: filename })).resolves.toMatchObject({
      all: true,
      include: ["src/**/*.ts"],
      exclude: ["src/__tests__/**"],
      extension: [".ts"],
    });
  });

  it("retains YAML extends and option normalization", async () => {
    await writeFile(join(directory, "base.yml"), "include: src/**/*.ts\nreport-dir: coverage\n");
    await writeFile(join(directory, ".nycrc.yml"), "extends: ./base.yml\nall: true\n");

    await expect(loadNycConfig({ cwd: directory, nycrcPath: ".nycrc.yml" })).resolves.toMatchObject({
      all: true,
      include: ["src/**/*.ts"],
      reportDir: "coverage",
    });
  });

  it("rejects executable JavaScript YAML tags", async () => {
    await writeFile(
      join(directory, ".nycrc.yml"),
      'fixture: !!js/function "function () { return true; }"\n',
    );

    await expect(loadNycConfig({ cwd: directory, nycrcPath: ".nycrc.yml" })).rejects.toThrow(
      /unknown tag/,
    );
  });

  it("keeps sprintf-js out of the complete locked dependency tree", async () => {
    const lock = JSON.parse(await readFile(join(__dirname, "../../package-lock.json"), "utf8")) as {
      packages: Record<string, { dependencies?: Record<string, string> }>;
    };

    for (const [path, entry] of Object.entries(lock.packages)) {
      expect(path).not.toMatch(/(?:^|\/)node_modules\/sprintf-js$/);
      expect(entry.dependencies ?? {}).not.toHaveProperty("sprintf-js");
    }
  });
});
