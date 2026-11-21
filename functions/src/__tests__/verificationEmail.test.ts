import { renderVerificationEmail } from "../verificationEmail";

describe("renderVerificationEmail", () => {
  it("renders a plain verification message and plain-text fallback", () => {
    const message = renderVerificationEmail(
      "https://example.test/verify?mode=verifyEmail&oobCode=moon",
      "Rowan",
    );

    expect(message.subject).toBe("Verify your Wiccan email");
    expect(message.html).toContain("Hi Rowan,");
    expect(message.html).toContain(
      "Confirm your email address to finish setting up your Wiccan account.",
    );
    expect(message.html).toContain("Verify your email");
    expect(message.html).toContain("https://example.test/verify?mode=verifyEmail&amp;oobCode=moon");
    expect(message.text).toContain("https://example.test/verify?mode=verifyEmail&oobCode=moon");
    expect(message.text).toContain("Hi Rowan,");
    expect(message.text).toContain(
      "This link is just for you and expires automatically. Wiccan will never ask you to share your password or this link.",
    );
    expect(message.html).toContain(
      "If you didn't create a Wiccan account, you can ignore this email.",
    );
    expect(message.text).toContain("The Wiccan Circle");
    expect(message.html).toContain("© Wiccan");
    expect(message.html).not.toContain("The moon is waiting");
    expect(message.html).not.toContain("The circle is almost open");
    expect(message.html).not.toContain("Moonlight on the threshold");
    expect(message.text).not.toContain("May your path be clear");
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

    expect(message.html).toContain("Hi friend,");
    expect(message.text).toContain("Hi friend,");
  });
});
