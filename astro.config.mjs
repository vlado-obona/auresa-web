// @ts-check
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

// Static output, deployed via GitHub Actions → SFTP (Websupport).
export default defineConfig({
  site: 'https://auresa.sk',
  trailingSlash: 'ignore',
  output: 'static',
  integrations: [sitemap()],
  build: {
    inlineStylesheets: 'auto',
  },
});
