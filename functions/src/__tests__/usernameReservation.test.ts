import { parseUsername } from "../callableInput";
import { decideUsernameReservation, UsernameRequiredError, UsernameTakenError } from "../usernameReservation";

describe("parseUsername", () => {
  it.each(["Morgan", "  moon_witch  ", "A1_b"])("accepts a trimmed handle: %p", (value) => {
    expect(parseUsername(value)).toBe(value.trim());
  });

  it.each([undefined, null, 7, "ab", "a".repeat(25), "has space", "dot.name", "café"])(
    "rejects an invalid handle: %p",
    (value) => expect(parseUsername(value)).toBeNull(),
  );

  it("treats case as the same reservation key", () => {
    expect(parseUsername("Rose")!.toLowerCase()).toBe(parseUsername("rose")!.toLowerCase());
  });
});

describe("decideUsernameReservation", () => {
  it("reserves a new handle and does not release anything", () => {
    expect(decideUsernameReservation({
      uid: "uid-1", requested: " Rose ", currentUsername: undefined, reservedByUid: null,
    })).toEqual({ displayUsername: "Rose", key: "rose", previousKey: null });
  });

  it("keeps an existing handle when the caller omits one", () => {
    expect(decideUsernameReservation({
      uid: "uid-1", requested: undefined, currentUsername: "Rose", reservedByUid: "uid-1",
    })).toEqual({ displayUsername: "Rose", key: "rose", previousKey: null });
  });

  it("rejects a missing handle so profile completion cannot be stamped", () => {
    expect(() => decideUsernameReservation({
      uid: "uid-1", requested: undefined, currentUsername: "  ", reservedByUid: null,
    })).toThrow(UsernameRequiredError);
    expect(() => decideUsernameReservation({
      uid: "uid-1", requested: "no", currentUsername: "Rose", reservedByUid: "uid-1",
    })).toThrow(UsernameRequiredError);
  });

  it("rejects a handle reserved by someone else", () => {
    expect(() => decideUsernameReservation({
      uid: "uid-1", requested: "Rose", currentUsername: undefined, reservedByUid: "uid-2",
    })).toThrow(UsernameTakenError);
  });

  it("allows the owner to keep their own reservation and releases the old key when it changes", () => {
    expect(decideUsernameReservation({
      uid: "uid-1", requested: "rose", currentUsername: "Rose", reservedByUid: "uid-1",
    }).previousKey).toBeNull();
    expect(decideUsernameReservation({
      uid: "uid-1", requested: "Rowan", currentUsername: "Rose", reservedByUid: null,
    })).toMatchObject({ displayUsername: "Rowan", key: "rowan", previousKey: "rose" });
  });
});
