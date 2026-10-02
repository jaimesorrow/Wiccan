// ESLint 9 removed .eslintrc in favour of flat config, and removed the --ext
// flag that used to scope the run to .ts. Both of those live here now: the
// files glob replaces --ext, and the ignores block replaces ignorePatterns.
//
// .mjs rather than .js because this package is CommonJS ("main": "lib/index.js"
// with no "type": "module"); a plain eslint.config.js would be parsed as CJS
// and the import statements below would fail.
import eslint from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["lib/**", "node_modules/**"] },
  {
    files: ["src/**/*.ts"],
    extends: [eslint.configs.recommended, ...tseslint.configs.recommended],
    languageOptions: {
      ecmaVersion: 2021,
      sourceType: "module",
      globals: { ...globals.node },
    },
  },
);
