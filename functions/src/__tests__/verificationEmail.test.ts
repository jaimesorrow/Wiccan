import { renderVerificationEmail } from "../verificationEmail";

describe("renderVerificationEmail", () => {
  it("renders the Wiccan mystical treatment and plain-text fallback", () => {
    const message = renderVerificationEmail(
      "https://example.test/verify?mode=verifyEmail&oobCode=moon",
      "Rowan",
    );

    expect(message.subject).toBe("The moon is waiting — verify your Wiccan email");
    expect(message.html).toContain("◐ ○ ● ○ ◑");
    expect(message.html).toContain("The circle is almost open, Rowan");
    expect(message.html).toContain("Verify your email");
    expect(message.html).toContain("https://example.test/verify?mode=verifyEmail&amp;oobCode=moon");
    expect(message.text).toContain("https://example.test/verify?mode=verifyEmail&oobCode=moon");
    expect(message.text).toContain("Moonlight on the threshold");
    expect(message.text).toContain(
      "This link is yours alone and expires automatically. Wiccan will never ask you to share your password or verification link.",
    );
    expect(message.html).toContain("If you did not create a Wiccan account, no action is needed.");
    expect(message.text).toContain("The Wiccan Circle");
    expect(message.html).toContain("© Wiccan");
  });

  it("escapes user-controlled names and action links", () => {
    const message = renderVerificationEmail(
      "https://example.test/verify?a=1&b=2",
      "<script>alert('moon')</script>",
    );

    expect(message.html).not.toContain("<script>");
    expect(message.html).toContain("&lt;script&gt;");
    expect(message.html).toContain("a=1&amp;b=2");
  });

  it("uses an intentional generic greeting when no display name exists", () => {
    const message = renderVerificationEmail("https://example.test/verify");

    expect(message.html).toContain("The circle is almost open, friend");
  });
});
