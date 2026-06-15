# /api — server-side endpointy (PHP)

Tieto súbory bežia na Websupport (PHP 8.x), nie v Astro builde. Astro ich len
skopíruje z `public/api/` do `dist/api/` a deploy ich nahrá na server.

## Súbory
- `kontakt.php` — príjem kontaktného formulára → e-mail cez SMTP (PHPMailer).
- `auth.php` — GitHub OAuth proxy pre Decap CMS (`/admin/`).
- `_env.php` — načítanie `.env` (interný include, neprístupný cez web).
- `composer.json` — závislosť PHPMailer.
- `.htaccess` — blokuje priamy web-prístup k `.env`, `_*.php`, composer súborom.

## Nasadenie na serveri (jednorazovo)
1. `composer install` v priečinku `api/` na serveri (vytvorí `vendor/`).
2. Vytvor `.env` **mimo webrootu** (ideálne o úroveň vyššie) alebo v `api/`
   (chránené `.htaccess`). Vzor je v koreňovom `.env.example`.
3. Doplň reálne hodnoty: `SMTP_PASS`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`.

## Bezpečnosť
- `.env` NIKDY necommituj (je v `.gitignore`).
- Odporúčam umiestniť `.env` nad webroot — `_env.php` ho hľadá aj v
  `dirname(__DIR__)` a `dirname(__DIR__, 2)`.
