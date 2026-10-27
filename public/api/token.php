<?php
/**
 * Auresa — časový token pre kontaktný formulár.
 * Formulár si ho vypýta pri prvom kliknutí do formulára; kontakt.php overí podpis
 * a že od vydania prešli aspoň 3 s (bot token nemá vôbec, alebo odosiela okamžite).
 * Bez FORM_SECRET v .env vráti prázdny podpis a kontakt.php token neoveruje.
 */
declare(strict_types=1);
require __DIR__ . '/_env.php';

ini_set('display_errors', '0');
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$t = time();
$secret = (string) au_env('FORM_SECRET', '');
echo json_encode(['t' => $t, 's' => $secret === '' ? '' : hash_hmac('sha256', (string) $t, $secret)]);
