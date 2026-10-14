<?php
/**
 * Auresa — kontaktný formulár → e-mail (SMTP Websupport).
 * POST application/json z formulára na auresa.sk (fetch v Kontakt.astro).
 * Polia: meno, email, telefon, typ_projektu, sprava, _hp (honeypot),
 *        _t + _s (časový token z token.php).
 * Odpoveď: JSON { success: bool, message: string }.
 *
 * Ochrana proti spamu — vrstvy v poradí, od najlacnejšej:
 *   1 host  2 origin  3 iba JSON  4 honeypot  5 časový token
 *   6 validácia  7 obsah  8 rate limit
 * Bot dostane rovnakú odpoveď ako človek (au_ok) a nič sa neodošle — LEN tam, kde
 * človek prakticky nemôže skončiť (chýbajúci Origin, urlencoded, honeypot, http(s)://
 * v mene, BBCode). Čo by mohlo zasiahnuť aj skutočného klienta, správu NEzahodí:
 * buď ju označí v predmete [PODOZRIVÉ] (odkaz v správe, www. v mene, azbuka, neúplný
 * telefón, viac správ z jednej IP — hotelová Wi-Fi, CGNAT), alebo vráti VIDITEĽNÚ
 * chybu s kontaktom (au_fail: zlý host, záplava z jednej IP, príliš dlhé telo) —
 * klient vtedy nepresmeruje ani nepošle konverziu.
 * Dôvod každého bloku ide do error_logu: 'auresa kontakt blok: <dôvod>'.
 * Na staging.auresa.sk (bez mailera) ho vidno aj v hlavičke X-Au-Debug.
 * Tajomstvá z .env (nikdy hardcode). Vyžaduje PHPMailer (composer).
 */
declare(strict_types=1);
require __DIR__ . '/_env.php';

// Varovanie PHP vo výstupe by pokazilo JSON → klient by ukázal chybu, hoci mail odišiel.
ini_set('display_errors', '0');
header('Content-Type: application/json; charset=utf-8');

const AU_HOSTS   = ['auresa.sk', 'www.auresa.sk', 'staging.auresa.sk'];
const AU_ORIGINS = ['https://auresa.sk', 'https://www.auresa.sk', 'https://staging.auresa.sk'];

$host = strtolower((string) preg_replace('/:\d+$/', '', (string) ($_SERVER['HTTP_HOST'] ?? '')));

function au_debug(string $msg): void {
    global $host;
    if ($host === 'staging.auresa.sk' && !headers_sent()) {
        header('X-Au-Debug: ' . preg_replace('/[^\x20-\x7E]/', '?', $msg), false);
    }
}
function au_ok(): void {
    echo json_encode(['success' => true, 'message' => 'Ďakujeme. Ozveme sa Vám.']);
    exit;
}
function au_block(string $why): void {
    error_log('auresa kontakt blok: ' . $why . ' | ip ' . ($_SERVER['REMOTE_ADDR'] ?? '?'));
    au_debug('blok: ' . $why);
    au_ok();
}
function au_fail(int $code, string $why, string $msg): void {
    error_log('auresa kontakt ' . $code . ': ' . $why . ' | ip ' . ($_SERVER['REMOTE_ADDR'] ?? '?'));
    au_debug($code . ': ' . $why);
    http_response_code($code);
    echo json_encode(['success' => false, 'message' => $msg]);
    exit;
}

// --- CORS (preflight) ---
$origin = (string) ($_SERVER['HTTP_ORIGIN'] ?? '');
if (in_array($origin, AU_ORIGINS, true)) {
    header("Access-Control-Allow-Origin: $origin");
    header('Vary: Origin');
    header('Access-Control-Allow-Methods: POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type');
}
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') { http_response_code(204); exit; }
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Metóda nie je povolená.']);
    exit;
}

// --- 1) host: maily len z produkcie (+ staging, ktorý mailer nemá); test.auresa.sk nie ---
// Viditeľná chyba, nie falošný úspech: test.auresa.sk má rovnaký docroot ako prod a stará
// otvorená karta odtiaľ je človek (stránky test → prod presmeruje .htaccess, /api nie).
if (!in_array($host, AU_HOSTS, true)) {
    au_fail(403, 'host ' . $host, 'Formulár odošlite prosím na https://auresa.sk alebo nám napíšte na info@auresa.sk.');
}

// --- 2) origin: prehliadač pri POST z fetch() posiela Origin vždy; keď chýba, skúsime Referer ---
$ref = (string) ($_SERVER['HTTP_REFERER'] ?? '');
$refOrigin = $ref !== '' ? parse_url($ref, PHP_URL_SCHEME) . '://' . parse_url($ref, PHP_URL_HOST) : '';
if (!in_array($origin, AU_ORIGINS, true) && !(in_array($origin, ['', 'null'], true) && in_array($refOrigin, AU_ORIGINS, true))) {
    au_block('origin "' . $origin . '" referer "' . $refOrigin . '"');
}

// --- 3) iba JSON (náš formulár posiela výhradne JSON; urlencoded = bot z HTML) ---
if (!str_contains((string) ($_SERVER['CONTENT_TYPE'] ?? ''), 'application/json')) au_block('content-type');
// skutočný formulár má najviac ~25 kB; väčšie telo nečítame celé (hosting má post_max_size 512M)
if ((int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > 65536) {
    au_fail(413, 'telo > 64 kB', 'Správa je príliš dlhá. Skráťte ju prosím alebo nám napíšte na info@auresa.sk.');
}
$data = json_decode(file_get_contents('php://input', false, null, 0, 65537) ?: '', true);
if (!is_array($data)) au_block('json');
$f = fn(string $k): string => is_scalar($data[$k] ?? null) ? trim((string) $data[$k]) : '';

// --- 4) honeypot ---
if ($f('_hp') !== '') au_block('honeypot');

$flags = [];   // mäkké signály → [PODOZRIVÉ] v predmete, správa sa doručí

// --- 5) časový token (token.php) ---
// Bez FORM_SECRET sa token neoveruje (fail-open + log) — chyba konfigurácie nesmie zahodiť leady.
// FORM_TOKEN_ENFORCE=0 (predvolené) → len loguje a označí; =1 → falošný alebo príliš rýchly token
// blokuje. ENFORCE nezapínať aspoň do konca kampane PSD2026 (24. 10.) a potom len ak log ukáže boty.
// Chýbajúci token (klientovi zlyhal fetch token.php) a starý token sa len označia, neblokujú.
$secret  = (string) au_env('FORM_SECRET', '');
$enforce = au_env('FORM_TOKEN_ENFORCE', '0') === '1';
$t = (int) $f('_t');
$tokenErr = null;
if ($secret === '') {
    error_log('auresa kontakt: FORM_SECRET chýba v .env — časový token sa neoveruje');
} elseif ($f('_s') === '') {
    error_log('auresa kontakt: token chýba');
    $flags[] = 'bez overovacieho tokenu';
} else {
    $age = time() - $t;
    if (!hash_equals(hash_hmac('sha256', (string) $t, $secret), $f('_s'))) $tokenErr = 'token neplatný';
    elseif ($age >= 0 && $age < 3) $tokenErr = 'rýchlosť ' . $age . ' s';   // záporný vek (hodiny) neblokuje
    elseif ($age > 7 * 86400) $flags[] = 'formulár otvorený viac ako 7 dní';
    if ($tokenErr !== null) {
        if ($enforce) au_block($tokenErr);
        error_log('auresa kontakt (len log): ' . $tokenErr);
        au_debug('len log: ' . $tokenErr);
        $flags[] = 'token: ' . $tokenErr;
    }
}

// --- 6) validácia ---
$meno    = $f('meno');
$email   = $f('email');
$telefon = $f('telefon');
$typ     = $f('typ_projektu');
$sprava  = $f('sprava');
$errors = [];
if ($meno === '' || strlen($meno) > 400) $errors['meno'] = 'meno';
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) $errors['email'] = 'e-mail';
// obrana proti header injection (CR/LF), aj keď PHPMailer filtruje
if (preg_match('/[\r\n]/', $email) || preg_match('/[\r\n]/', $meno)) $errors['email'] = 'e-mail';
// telefón: chyba len pri prázdnom / nezmyselne dlhom; menej ako 9 číslic („radšej e-mailom",
// klapka, dve čísla) prejde s príznakom — nová prekážka by stála leady, proti spamu nepomôže
$digits = strlen((string) preg_replace('/\D+/', '', $telefon));
if ($telefon === '' || $digits > 30 || strlen($telefon) > 200) $errors['telefon'] = 'telefón';
if ($sprava === '' || strlen($sprava) > 20000) $errors['sprava'] = 'správa';
if ($errors) {
    http_response_code(422);
    echo json_encode([
        'success' => false,
        'message' => 'Skontrolujte prosím: ' . implode(', ', $errors) . '.',
        'fields'  => array_keys($errors),
    ]);
    exit;
}

// --- 7) obsah: tvrdé pravidlá blokujú, mäkké len označia predmet ---
// „www." v mene píše aj skutočný klient (MUDr. Novák, www.novakdent.sk) → len príznak
$urlRe = '~(https?://|www\.)~i';
if (preg_match('~https?://~i', $meno)) au_block('odkaz v mene');
elseif (preg_match('~\bwww\.~i', $meno)) $flags[] = 'web v mene';
if ($digits < 9 && preg_match('/^.{0,40}/us', $telefon, $m)) $flags[] = 'telefón neúplný: ' . $m[0];
if (preg_match('~\[url=|<a\s+href~i', $sprava)) au_block('bbcode/html odkaz');
$nUrl = (int) preg_match_all($urlRe, $sprava);
if ($nUrl > 0) $flags[] = 'odkazy v správe: ' . $nUrl;
if (preg_match('/[\p{Cyrillic}\p{Han}\p{Hangul}\p{Arabic}\p{Thai}]/u', $meno . ' ' . $sprava)) $flags[] = 'nelatinské písmo';

// --- 8) rate limit podľa IP ---
// Mäkký: od 3 správ / 10 min alebo 10 / deň → [PODOZRIVÉ] (zdieľaná IP — hotel, firma, mobilný
// operátor — nesmie zahodiť skutočný lead). Tvrdý: od 10 / 10 min alebo 30 / deň → VIDITEĽNÁ
// chyba 429 s kontaktom (nie falošný úspech), a len pri verejnej IP: ak by REMOTE_ADDR bola
// adresa proxy/privátna, tvrdý limit sa neuplatní (inak by zdieľali vedro všetci návštevníci).
// IPv6 sa počíta po /64 (jeden pripojený klient = celá /64). IP sa neukladá v čitateľnej podobe
// (hash so soľou + dátum), súbory sa mažú po 48 h. Adresár musí byť náš (0700, nie symlink),
// inak sa vrstva vypne (fail-open) — všetky operácie sú potichu (@).
$rlDir = rtrim(sys_get_temp_dir(), '/\\') . '/au-kontakt-rl-' . substr(hash('sha256', __FILE__ . '|' . $secret), 0, 12);
if (!@is_dir($rlDir)) @mkdir($rlDir, 0700);
$rlOk = @is_dir($rlDir) && !@is_link($rlDir) && ((@fileperms($rlDir) & 0777) === 0700) && @is_writable($rlDir);
if ($rlOk) {
    $ip = (string) ($_SERVER['REMOTE_ADDR'] ?? '');
    $ipKey = $ip;
    $bin = @inet_pton($ip);
    if ($bin !== false && strlen($bin) === 16) $ipKey = bin2hex(substr($bin, 0, 8)) . '/64';
    $ipPublic = filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE) !== false;
    $rlFile = $rlDir . '/' . hash('sha256', $ipKey . '|' . date('Y-m-d') . '|' . ($secret !== '' ? $secret : __FILE__));
    $now = time();
    if (@is_link($rlFile)) @unlink($rlFile);
    $lines = @is_file($rlFile) ? (@file($rlFile, FILE_IGNORE_NEW_LINES) ?: []) : [];
    $hits = array_values(array_filter(array_map('intval', $lines), fn(int $ts): bool => $ts > $now - 86400));
    $last10 = count(array_filter($hits, fn(int $ts): bool => $ts > $now - 600));
    $rl = $last10 . '/10 min, ' . count($hits) . '/deň';
    au_debug('rate ' . $last10 . '/10min ' . count($hits) . '/den ip ' . $ip . ($ipPublic ? '' : ' (neverejna)'));
    if ($last10 >= 10 || count($hits) >= 30) {
        if ($ipPublic) {
            au_fail(429, 'rate limit ' . $rl, 'Z vašej siete sme práve prijali veľa správ. Napíšte nám prosím priamo na info@auresa.sk alebo zavolajte +421 911 123 181.');
        }
        error_log('auresa kontakt: rate limit ' . $rl . ' — REMOTE_ADDR ' . $ip . ' nie je verejná IP, bez bloku');
    }
    if ($last10 >= 3 || count($hits) >= 10) $flags[] = 'viac správ z jednej IP: ' . $rl;
    $hits[] = $now;
    @file_put_contents($rlFile, implode("\n", $hits), LOCK_EX);
    if (random_int(1, 50) === 1) {
        foreach (@glob($rlDir . '/*') ?: [] as $p) {
            if (@filemtime($p) < $now - 172800) @unlink($p);
        }
    }
} else {
    error_log('auresa kontakt: rate limit vypnutý — ' . $rlDir . ' nie je náš / zapisovateľný');
}

if ($flags) au_debug('podozrive: ' . implode(', ', $flags));

// --- PHPMailer (composer vendor/) ---
$autoload = __DIR__ . '/vendor/autoload.php';
if (!is_file($autoload)) {
    // na stagingu zámerne (bez mailera) — HTTP 500 tu = správa prešla všetkými filtrami
    error_log('auresa kontakt: chýba vendor/autoload.php');
    au_debug('presla filtrami, mailer chyba');
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Mailer nie je nainštalovaný (composer install).']);
    exit;
}
require $autoload;

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

$mail = new PHPMailer(true);
try {
    $mail->isSMTP();
    $mail->Host       = au_env('SMTP_HOST', 'smtp.websupport.sk');
    $mail->SMTPAuth   = true;
    $mail->Username   = au_env('SMTP_USER', '');
    $mail->Password   = au_env('SMTP_PASS', '');
    $mail->SMTPSecure = PHPMailer::ENCRYPTION_SMTPS; // SSL 465
    $mail->Port       = (int) au_env('SMTP_PORT', '465');
    $mail->CharSet    = 'UTF-8';

    $from     = au_env('SMTP_FROM', 'info@auresa.sk');
    $fromName = au_env('SMTP_FROM_NAME', 'Auresa Design Studio');
    $to       = au_env('MAIL_TO', 'info@auresa.sk');

    $mail->setFrom($from, $fromName);
    $mail->addAddress($to);
    $mail->addReplyTo($email, $meno);

    $mail->Subject = ($flags ? '[PODOZRIVÉ] ' : '') . 'Nová správa z auresa.sk — ' . $meno;
    $body  = "Meno: $meno\n";
    $body .= "E-mail: $email\n";
    $body .= "Telefón: $telefon\n";
    $body .= "Typ projektu: " . ($typ !== '' ? $typ : '—') . "\n\n";
    $body .= "Správa:\n$sprava\n";
    if ($flags) $body .= "\n—\nAutomatická kontrola (správa NIE JE zablokovaná, len označená): " . implode('; ', $flags) . "\n";
    $mail->Body = $body;

    $mail->send();
    au_ok();
} catch (Exception $e) {
    http_response_code(500);
    error_log('auresa kontakt mailer: ' . $mail->ErrorInfo);
    echo json_encode(['success' => false, 'message' => 'Správu sa nepodarilo odoslať. Skúste neskôr alebo nám napíšte priamo na info@auresa.sk.']);
}
