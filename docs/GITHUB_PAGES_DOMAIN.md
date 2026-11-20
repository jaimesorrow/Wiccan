# Launch site and legal pages — `wiccan.app`

The launch site and legal pages live in `hosting/`. The canonical host is
**Firebase Hosting** (project `wiccan-android`, `"public": "hosting"` in
`firebase.json`) at **https://wiccan.app**. The domain was bought on Cloudflare
Registrar (2026-11-19), so its DNS is managed in Cloudflare.

`app/…/core/config/PolicyUrls.kt` and the Play Console listing use these URLs:

| Page | URL |
| --- | --- |
| Home | https://wiccan.app/ |
| Privacy policy | https://wiccan.app/privacy.html |
| Terms of service | https://wiccan.app/terms.html |
| Content & Safety | https://wiccan.app/content-safety.html |
| Potion Safety | https://wiccan.app/potion-safety.html |
| Account deletion | https://wiccan.app/delete-account.html |

(The file keeps its historical name; GitHub Pages is now only a secondary
preview — see the end of this document.)

## One-time Firebase Hosting setup

1. Firebase console → **Hosting** → **Add custom domain** → `wiccan.app`
   (already added for `wiccan-android`; the console shows the records below).
2. In the Cloudflare DNS zone for `wiccan.app`, add the records Firebase shows
   (values current as of 2026-11-19; trust the console if they differ):

   | Type | Name | Value | Proxy |
   | --- | --- | --- | --- |
   | A | `@` | `199.36.158.100` | **DNS only** (grey cloud) |
   | TXT | `@` | `hosting-site=wiccan-android` | n/a |

   Keep the A record un-proxied until Firebase reports the domain as
   *Connected* and its certificate is issued; Cloudflare proxying can block
   Firebase's certificate validation. Remove any conflicting A, AAAA or CNAME
   record for `@`.
3. Optionally add `www` as a redirect to the apex in the Firebase console.
4. Deploy the pages: `firebase deploy --only hosting --project wiccan-android`.

`.app` is on the HSTS preload list, so the site is HTTPS-only; Firebase issues
the certificate automatically once DNS validates (can take up to a few hours).

## Verification

```bash
dig +short A wiccan.app          # 199.36.158.100
dig +short TXT wiccan.app        # "hosting-site=wiccan-android"
curl -I https://wiccan.app/
curl -I https://wiccan.app/privacy.html
curl -I https://wiccan.app/terms.html
curl -I https://wiccan.app/delete-account.html
```

All requests must return a successful response before the URLs are entered in
Play Console. Also open the pages signed out in a normal browser and on a phone.
Pages in `hosting/` use root-absolute paths (`/styles.css`, `/assets/...`),
which resolve correctly at the `wiccan.app` root.

## GitHub Pages (secondary preview)

`.github/workflows/pages.yml` still publishes `hosting/` to
https://jaimesorrow.github.io/Wiccan/ . Because that site lives under the
`/Wiccan/` path, the workflow stages a copy of `hosting/` and prefixes the
root-absolute `href`/`src` paths with `/Wiccan/`; the files in `hosting/` stay
unchanged for Firebase. The old `hosting/CNAME` for a previous unused
hostname was removed, and no custom domain is configured in the
repository's Pages settings. The public domain is now `wiccan.app`, which
is served by Firebase Hosting, so do not set it as the Pages custom
domain.
