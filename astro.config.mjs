// @ts-check
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

// Static output, deployed via GitHub Actions → SFTP (Websupport).
export default defineConfig({
  site: 'https://auresa.sk',
  trailingSlash: 'ignore',
  output: 'static',
  // /dakujeme/ je noindex ďakovná stránka — do sitemap nepatrí
  integrations: [sitemap({ filter: (page) => !page.includes('/dakujeme') })],
  build: {
    inlineStylesheets: 'auto',
  },
});
