/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: "node",
  roots: ["<rootDir>/src"],
  transform: {
    // isolatedModules (set in tsconfig.test.json) skips full type-checking
    // per test file — tsc already covers that in `npm run build`.
    "^.+\\.ts$": ["ts-jest", { tsconfig: "tsconfig.test.json" }],
  },
};
