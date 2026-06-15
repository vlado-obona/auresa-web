# auresa.sk — Auresa Design Studio

Statický web v **Astro** + **Decap CMS**, deploy cez **GitHub Actions → SFTP (Websupport)**.
Autorské interiérové štúdio Denisy Rakašovej Lašovej (Prešov).

## Stack
- Astro (static output)
- Self-hosted fonty (Fontsource): Cormorant Garamond, Inter Tight, Italianno
- Decap CMS (`/admin/`) — kolekcia *Realizácie*
- PHP endpointy na serveri: kontaktný formulár + Decap OAuth (`public/api/`)

## Vývoj
```bash
npm install
npm run dev        # http://localhost:4321
npm run build      # → dist/
npm run preview
```

## Štruktúra
```
src/
  components/   Nav, Hero, Podstata, VertikalyGrid, SidePanel, OAutorke,
                ViziaStudia, Kontakt, Footer, CookieBanner
  layouts/      BaseLayout.astro   (head, SEO, JSON-LD, fonty, shell)
  pages/        index + dentalna-klinika / domy-a-byty / kancelarie
                realizacie/ (index + [slug]) + 3 právne stránky
  content/      realizacie/ (Decap markdown)
  styles/       global.css (tokeny + reset + komponenty)
  data/         site.ts (kontakt, vertikály, SEO)
public/
  admin/        Decap CMS
  api/          PHP (kontakt.php, auth.php) — viď public/api/README.md
  images/       webp fotky + favicon
```

## Nasadenie
Push do `main` spustí `.github/workflows/deploy.yml`. Pred prvým deployom doplň
GitHub Secrets: `SFTP_HOST`, `SFTP_USER`, `SFTP_PASS`, `SFTP_PATH`.
Na serveri raz: `composer install` v `api/` + vytvor `.env` (vzor `.env.example`).

## Tajomstvá
Nikdy necommituj `.env`. Vzor je v `.env.example` (len placeholdery).
