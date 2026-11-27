import { SupportedLocale } from "./locale";

export interface VerificationEmail {
  subject: string;
  text: string;
  html: string;
}

const EMAIL_COPY: Record<SupportedLocale, {
  subject: string; hello: string; friend: string; intro: string; action: string; security: string;
  ignore: string; circle: string; questions: string; preheader: string;
}> = {
  en: {
    subject: "Your Wiccan path awaits — verify your email", hello: "Hi", friend: "friend",
    intro: "A place in the circle has been prepared for you. Confirm your email address to open your Wiccan account.",
    action: "Open the moonlit link",
    security: "This link is only for you and expires automatically. The Grimoire remains sealed until it is opened. Wiccan will never ask you to share your password or this link.",
    ignore: "If you didn't create a Wiccan account, you can ignore this email.", circle: "The Wiccan Circle",
    questions: "Questions along your path?", preheader: "Open your moonlit verification link to enter Wiccan.",
  },
  es: {
    subject: "Tu camino en Wiccan te espera — verifica tu correo", hello: "Hola", friend: "amiga o amigo",
    intro: "Hay un lugar preparado para ti en el círculo. Confirma tu correo electrónico para abrir tu cuenta de Wiccan.",
    action: "Abrir el enlace a la luz de la luna",
    security: "Este enlace es solo para ti y caduca automáticamente. El Grimorio permanecerá sellado hasta que lo abras. Wiccan nunca te pedirá que compartas tu contraseña ni este enlace.",
    ignore: "Si no creaste una cuenta de Wiccan, puedes ignorar este correo.", circle: "El Círculo de Wiccan",
    questions: "¿Preguntas sobre tu camino?", preheader: "Abre el enlace de verificación para entrar en Wiccan.",
  },
  "pt-BR": {
    subject: "Seu caminho no Wiccan espera por você — confirme seu e-mail", hello: "Olá", friend: "amiga ou amigo",
    intro: "Um lugar no círculo foi preparado para você. Confirme seu e-mail para abrir sua conta do Wiccan.",
    action: "Abrir o link ao luar",
    security: "Este link é somente seu e expira automaticamente. O Grimório permanece selado até que você o abra. O Wiccan nunca pedirá que você compartilhe sua senha nem este link.",
    ignore: "Se você não criou uma conta do Wiccan, ignore este e-mail.", circle: "O Círculo do Wiccan",
    questions: "Dúvidas sobre o seu caminho?", preheader: "Abra o link de verificação para entrar no Wiccan.",
  },
};

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

/**
 * Renders the production verification message without remote assets or CSS
 * classes so the visual treatment survives Gmail, Outlook and mobile clients.
 */
export function renderVerificationEmail(
  verificationLink: string,
  displayName?: string,
  locale: SupportedLocale = "en",
): VerificationEmail {
  const copy = EMAIL_COPY[locale];
  const greeting = displayName?.trim() || copy.friend;
  const safeName = escapeHtml(greeting);
  const safeLink = escapeHtml(verificationLink);
  const subject = copy.subject;
  const text = [
    `${copy.hello} ${greeting},`,
    "",
    copy.intro,
    "",
    `${copy.action}:`,
    verificationLink,
    "",
    copy.security,
    "",
    copy.ignore,
    "",
    copy.circle,
    `${copy.questions} customerservice@wiccan.app`,
  ].join("\n");

  const html = `<!doctype html>
<html lang="${locale}">
  <body style="margin:0;padding:0;background:#000000;color:#f7f0df;font-family:Arial,sans-serif">
    <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent">${copy.preheader}</div>
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%;background-color:#000000;padding:36px 12px">
      <tr><td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%;max-width:620px;background:#171322;border:1px solid #d4af37">
          <tr><td style="height:5px;background:#c9a85e;font-size:0;line-height:0">&nbsp;</td></tr>
          <tr><td style="padding:34px 46px 0;text-align:center">
            <p style="margin:0;color:#8f7aa8;font-family:Georgia,serif;font-size:16px;letter-spacing:12px">◐ ○ ● ○ ◑</p>
            <p style="margin:23px 0 9px;color:#d8bd78;font-size:10px;font-weight:bold;letter-spacing:4px;text-transform:uppercase">Moonlit practice</p>
            <h1 style="margin:0;color:#f7f0df;font-family:Georgia,'Times New Roman',serif;font-size:56px;font-weight:normal;line-height:1">Wiccan</h1>
            <p style="margin:8px 0 0;color:#b4a4c6;font-family:Georgia,'Times New Roman',serif;font-size:17px;font-style:italic">a daily rite</p>
          </td></tr>
          <tr><td style="padding:24px 46px 0;text-align:center;color:#d8bd78;font-family:Georgia,serif;font-size:17px;letter-spacing:7px">✦ &nbsp; ☾ &nbsp; ✦</td></tr>
          <tr><td style="padding:27px 46px 44px">
            <h2 style="margin:0 0 18px;color:#f7f0df;font-family:Georgia,'Times New Roman',serif;font-size:31px;font-weight:normal;line-height:1.25;text-align:center">${copy.hello} ${safeName},</h2>
            <p style="margin:0 auto 29px;max-width:470px;color:#c9c0d1;font-size:16px;line-height:1.75;text-align:center">${copy.intro}</p>
            <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center">
              <tr><td style="background:#d8bd78;border:1px solid #edd798;border-radius:2px;text-align:center">
                <a href="${safeLink}" style="display:inline-block;padding:17px 28px;color:#171322;font-size:12px;font-weight:bold;letter-spacing:2px;text-decoration:none;text-transform:uppercase">${copy.action}</a>
              </td></tr>
            </table>
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin-top:34px;border-top:1px solid #3d334a;border-bottom:1px solid #3d334a">
              <tr>
                <td width="54" valign="middle" style="padding:18px 0;color:#d8bd78;font-family:Georgia,serif;font-size:28px;text-align:center">☽</td>
                <td style="padding:18px 8px;color:#94899f;font-size:12px;line-height:1.65">${copy.security}</td>
                <td width="54" valign="middle" style="padding:18px 0;color:#d8bd78;font-family:Georgia,serif;font-size:28px;text-align:center">☾</td>
              </tr>
            </table>
            <p style="margin:25px 0 0;color:#81768d;font-size:11px;line-height:1.65;text-align:center">${copy.ignore}</p>
          </td></tr>
          <tr><td style="background:#100d19;border-top:1px solid #3d334a;padding:25px 40px;color:#807488;font-family:Georgia,'Times New Roman',serif;font-size:12px;line-height:1.7;text-align:center">
            <span style="color:#bca56d;letter-spacing:4px">❧ &nbsp; ✦ &nbsp; ❧</span><br>
            <span style="color:#a494b5">${copy.circle}</span><br>
            ${copy.questions} <a href="mailto:customerservice@wiccan.app" style="color:#d8bd78">customerservice@wiccan.app</a>
          </td></tr>
        </table>
        <p style="margin:18px 0 0;color:#61586b;font-size:10px;line-height:1.5;text-align:center">© Wiccan</p>
      </td></tr>
    </table>
  </body>
</html>`;

  return { subject, text, html };
}
