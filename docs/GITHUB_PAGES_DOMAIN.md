# GitHub Pages launch site — `app.wiccanandroid.com`

The launch site and legal pages live in `hosting/` and deploy from
`.github/workflows/pages.yml`. GitHub Pages is the intended host for the
`app.wiccanandroid.com` subdomain; the apex domain remains available for a
future company/home site or redirect.

## One-time GitHub setup

1. Open `jaimesorrow/Wiccan` → **Settings → Pages**.
2. Set **Source** to **GitHub Actions**.
3. Under **Custom domain**, enter `app.wiccanandroid.com` and save.
4. Leave **Enforce HTTPS** off until GitHub reports that the certificate is
   issued; then turn it on.

The `hosting/CNAME` file records the intended hostname, but GitHub requires the
custom domain to be saved in repository settings when Pages deploys through a
custom Actions workflow.

## DNS at the domain registrar

Create this record in the DNS zone for `wiccanandroid.com`:

| Type | Host/name | Target/value | TTL |
| --- | --- | --- | --- |
| CNAME | `app` | `jaimesorrow.github.io` | Automatic or 3600 |

Enter only the hostname—no `https://` and no path. Remove any conflicting A,
AAAA, ALIAS, ANAME, or CNAME record for the `app` host. Do not point the
subdomain back to the apex domain; point it directly to `jaimesorrow.github.io`.

GitHub may offer a domain-verification TXT record. Add the exact host and value
shown in the GitHub account's Pages/domain settings; do not invent this value or
reuse a verification value from another account.

## Verification

DNS and certificate issuance can take time. Verify in this order:

```bash
dig +short CNAME app.wiccanandroid.com
curl -I https://app.wiccanandroid.com/
curl -I https://app.wiccanandroid.com/privacy.html
curl -I https://app.wiccanandroid.com/delete-account.html
```

The CNAME should resolve to `jaimesorrow.github.io`, and all three HTTPS requests
must return a successful response before entering the URLs in Play Console.
Also open the pages signed out in a normal browser and on a phone.

As of November 16, 2026, public DNS returned `NXDOMAIN` for
`wiccanandroid.com`; the registrar's nameservers/DNS zone must become publicly
available before GitHub can issue HTTPS for the subdomain.

Official GitHub guidance:

- https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site
- https://docs.github.com/en/pages/getting-started-with-github-pages/securing-your-github-pages-site-with-https

