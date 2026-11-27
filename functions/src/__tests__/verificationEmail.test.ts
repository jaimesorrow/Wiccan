import { renderVerificationEmail } from "../verificationEmail";

describe("renderVerificationEmail", () => {
  it("renders a plain verification message and plain-text fallback", () => {
    const message = renderVerificationEmail(
      "https://example.test/verify?mode=verifyEmail&oobCode=moon",
      "Rowan",
    );

    expect(message.subject).toBe("Your Wiccan path awaits — verify your email");
    expect(message.html).toContain("Hi Rowan,");
    expect(message.html).toContain(
      "A place in the circle has been prepared for you. Confirm your email address to open your Wiccan account.",
    );
    expect(message.html).toContain("Open the moonlit link");
    expect(message.html).toContain("https://example.test/verify?mode=verifyEmail&amp;oobCode=moon");
    expect(message.text).toContain("https://example.test/verify?mode=verifyEmail&oobCode=moon");
    expect(message.text).toContain("Hi Rowan,");
    expect(message.text).toContain(
      "This link is only for you and expires automatically. The Grimoire remains sealed until it is opened. Wiccan will never ask you to share your password or this link.",
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

  it("renders Spanish verification copy", () => {
    const message = renderVerificationEmail("https://example.test/verify", "Luna", "es");

    expect(message.subject).toBe("Tu camino en Wiccan te espera — verifica tu correo");
    expect(message.html).toContain('lang="es"');
    expect(message.text).toContain("Hola Luna,");
    expect(message.text).toContain("Abrir el enlace a la luz de la luna");
  });

  it("renders Brazilian Portuguese verification copy", () => {
    const message = renderVerificationEmail("https://example.test/verify", undefined, "pt-BR");

    expect(message.subject).toBe("Seu caminho no Wiccan espera por você — confirme seu e-mail");
    expect(message.html).toContain('lang="pt-BR"');
    expect(message.text).toContain("Olá amiga ou amigo,");
    expect(message.text).toContain("Abrir o link ao luar");
  });
});
