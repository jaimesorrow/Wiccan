import { renderVerificationEmail } from "../verificationEmail";

describe("renderVerificationEmail", () => {
  it("renders the Wiccan mystical treatment and plain-text fallback", () => {
    const message = renderVerificationEmail(
      "https://example.test/verify?mode=verifyEmail&oobCode=moon",
      "Rowan",
    );

    expect(message.subject).toBe("A moonlit welcome — verify your Wiccan email");
    expect(message.html).toContain("◐ ○ ● ○ ◑");
    expect(message.html).toContain("Blessed welcome, Rowan");
    expect(message.html).toContain("Complete the circle");
    expect(message.text).toContain("Under moon and starlight");
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

    expect(message.html).toContain("Blessed welcome, kindred spirit");
  });
});
