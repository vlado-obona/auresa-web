import { defineCollection, z } from 'astro:content';

const realizacie = defineCollection({
  type: 'content',
  schema: z.object({
    nazov: z.string(),
    vertikala: z.enum(['dental', 'domy', 'kancelarie']),
    lokalita: z.string(),
    rok: z.number(),
    status: z.enum(['realizacia', 'render', 'v-procese']),
    hlavna_foto: z.string(),
    galeria: z.array(z.string()).optional(),
    perex: z.string(),
    case_study: z.string().optional(),
    klient: z.string().optional(),   // voliteľné — zobrazí sa v meta detailu
    termin: z.string().optional(),   // ak je, nahrádza zobrazenie poľa "Rok"
  }),
});

export const collections = { realizacie };
