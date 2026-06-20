/* ─────────────────────────────────────────────────────────────
   AURESA — zdieľané dáta (single source of truth).
   Copy = schválený mäkký tón (FAZA2 copy framework). E-mail všade
   info@auresa.sk. Firemné údaje z právnych stránok.
   ───────────────────────────────────────────────────────────── */

export const SITE = {
  name: 'Auresa Design Studio',
  shortName: 'Auresa',
  legalName: 'Auresa s.r.o.',
  author: 'Denisa Rakašová Lašová',
  url: 'https://auresa.sk',
  email: 'info@auresa.sk',
  phone: '+421 911 123 181',
  phoneHref: 'tel:+421911123181',
  instagram: 'https://www.instagram.com/auresa_designstudio/',
  instagramHandle: '@auresa_designstudio',
  facebook: 'https://www.facebook.com/profile.php?id=61590847735460',
  facebookLabel: 'Auresa Design Studio',
  city: 'Prešov',
  country: 'Slovensko',
  foundingYear: '2025',
  ico: '57152969',
  dic: '2122596025',
  address: 'Odbojárska 245/46, 082 71 Lipany',
} as const;

export const SEO = {
  defaultTitle: 'Auresa Design Studio — priestor má svoju dušu',
  defaultDescription:
    'Autorský interiérový dizajn pre dentálne kliniky, domy a kancelárie. ' +
    'Auresa Design Studio, Prešov — priestor, ktorý upokojuje.',
  ogImage: '/images/glamident_lounge.webp',
  locale: 'sk_SK',
} as const;

/* Tri vertikály — homepage karty + side-panel obsah + subpage slug. */
export const VERTS = [
  {
    key: 'dental',
    num: '01',
    title: 'Dentálna klinika',
    slug: 'dentalna-klinika',
    cardQuote: 'Priestor, ktorý upokojuje. Starostlivosť, ku ktorej sa pacienti vracajú.',
    cardPhoto: '/images/glamident_treatment.webp',
    panel: {
      label: 'Vertikála I · Dental',
      perex: 'Priestor, ktorý upokojuje. Starostlivosť, ku ktorej sa pacienti vracajú.',
      body: 'Keď pacient prekročí prah ambulancie, prvý dojem vzniká ešte pred samotným ošetrením. Citlivo navrhnutý interiér zmierňuje stres, podporuje pocit istoty a vytvára prostredie, v ktorom sa pacient cíti prijatý. Dizajn sa tak stáva nenápadnou, no dôležitou súčasťou kvalitnej zdravotnej starostlivosti.',
      klient: 'Stomatológ-majiteľ',
      cyklus: '3–9 mesiacov',
      referencie: [
        { label: 'Ideal Smile · Vranov', slug: 'ideal-smile' },
        { label: 'GlamiDent · Prešov', slug: 'glamident' },
        { label: 'ArtSmile · Prešov', slug: 'artsmile' },
      ],
    },
  },
  {
    key: 'domy',
    num: '02',
    title: 'Domy a byty',
    slug: 'domy-a-byty',
    cardQuote: 'Priestor, ktorý odráža váš život. Domov, ktorý prináša pokoj.',
    cardPhoto: '/images/mokrance-ii_obyvacka-krb.webp',
    panel: {
      label: 'Vertikála II · Domy',
      perex: 'Priestor, ktorý odráža váš život. Domov, ktorý prináša pokoj.',
      body: 'Domov je miestom každodenného života, oddychu aj spoločných chvíľ. Citlivo navrhnutý interiér prepája estetiku s každodennou funkčnosťou a vytvára prostredie, v ktorom sa prirodzene žije, odpočíva aj stretáva. Výsledkom nie je len krásny priestor, ale domov, ktorý podporuje kvalitu života v každom detaile.',
      klient: 'Pár / rodina 30–50 r.',
      cyklus: '1–6 mesiacov',
      referencie: [
        { label: 'Rodinný dom Mokrance II · Realizácia', slug: 'mokrance-ii' },
        { label: 'Rodinný dom Šidlovec · V procese', slug: null },
        { label: 'Rodinný dom Torysa · Render', slug: 'torysa' },
        { label: 'Pánsky byt Zvolen · Render', slug: 'zvolen' },
      ],
    },
  },
  {
    key: 'kanc',
    num: '03',
    title: 'Kancelárie',
    slug: 'kancelarie',
    cardQuote: 'Priestor ako tichý náborový nástroj.',
    cardPhoto: '/images/budovatelska_zasadacka-hero.webp',
    panel: {
      label: 'Vertikála III · Kanc',
      perex: 'Priestor ako tichý náborový nástroj.',
      body: 'Kancelária už nie je len adresa, kam sa ráno presúvate. Je to to, čo zamestnanca pritiahne, ale aj to, čo udrží jeho sústredenie v prvých minútach týždňa.',
      klient: 'Súkromný klient',
      cyklus: '2–8 mesiacov',
      referencie: [
        { label: 'Budovateľská · Prešov', slug: null },
      ],
    },
  },
] as const;

export type Vert = (typeof VERTS)[number];

export const PROJECT_TYPES = [
  'Dentálna klinika',
  'Dom alebo byt',
  'Kancelária alebo komerčný priestor',
  'Iné',
] as const;

/* Google Analytics 4 — verejné Measurement ID (jedno miesto).
   Načíta sa LEN po súhlase a LEN na produkčnom hostname (viď CookieBanner). */
export const GA4_ID = 'G-YYBJJWHTQ6';

/* Google Tag Manager — verejné Container ID (jedno miesto).
   Consent-gated: načíta sa LEN po súhlase a LEN na čistom prod hostname `auresa.sk`
   (www/test/staging/localhost = dormantné). Kontajner je zatiaľ PRÁZDNY (žiadne tagy)
   → GA4 ostáva jediný zdroj page_view, žiadne dvojité počítanie. Viď CookieBanner. */
export const GTM_ID = 'GTM-P9MZJWW9';
