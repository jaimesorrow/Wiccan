import { initializeContentProject, parseContentArgs } from "../scripts/contentProject";
import { applicationDefault, initializeApp } from "firebase-admin/app";

jest.mock("firebase-admin/app", () => ({
  applicationDefault: jest.fn(() => ({ marker: "adc" })),
  initializeApp: jest.fn(),
}));

describe("production content project guard", () => {
  afterEach(() => {
    delete process.env.FIRESTORE_EMULATOR_HOST;
    jest.clearAllMocks();
  });

  it("validates locally without selecting a remote project", () => {
    expect(parseContentArgs([], true)).toEqual({ commit: false, project: undefined });
    expect(initializeApp).not.toHaveBeenCalled();
  });

  it("requires the literal intended project for any remote action", () => {
    expect(() => initializeContentProject(undefined)).toThrow("Explicit --project wiccan-android");
    expect(() => parseContentArgs(["--project", "other-project", "--commit"], true)).toThrow("only targets wiccan-android");
    expect(initializeApp).not.toHaveBeenCalled();
  });

  it("does not interpret misspelled flags or positional arguments as authorization", () => {
    expect(() => parseContentArgs(["--comit"], true)).toThrow();
    expect(() => parseContentArgs(["--commit", "wiccan-android"], true)).toThrow();
    expect(() => parseContentArgs(["--commit"], false)).toThrow();
  });

  it("refuses to report emulator data as production data", () => {
    process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
    expect(() => initializeContentProject("wiccan-android")).toThrow("Unset FIRESTORE_EMULATOR_HOST");
    expect(initializeApp).not.toHaveBeenCalled();
  });

  it("initializes Admin SDK with an explicit target and application default credentials", () => {
    const args = parseContentArgs(["--project", "wiccan-android", "--commit"], true);
    expect(args.commit).toBe(true);
    initializeContentProject(args.project);
    expect(applicationDefault).toHaveBeenCalledTimes(1);
    expect(initializeApp).toHaveBeenCalledWith({ projectId: "wiccan-android", credential: { marker: "adc" } });
  });
});
