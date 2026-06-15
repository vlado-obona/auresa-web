<?php
/**
 * Auresa — kontaktný formulár → e-mail (SMTP Websupport).
 * POST application/json alebo application/x-www-form-urlencoded.
 * Polia: meno, email, telefon, typ_projektu, sprava, _hp (honeypot).
 * Odpoveď: JSON { success: bool, message: string }.
 * Tajomstvá z .env (nikdy hardcode). Vyžaduje PHPMailer (composer).
 */
declare(strict_types=1);
require __DIR__ . '/_env.php';

header('Content-Type: application/json; charset=utf-8');

// --- CORS: len auresa.sk ---
$allowed = ['https://auresa.sk', 'https://www.auresa.sk'];
$origin = $_SERVER['HTTP_ORIGIN'] ?? '';
if (in_array($origin, $allowed, true)) {
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

// --- vstup (JSON alebo form) ---
$raw = file_get_contents('php://input') ?: '';
$data = [];
if (str_contains($_SERVER['CONTENT_TYPE'] ?? '', 'application/json')) {
    $data = json_decode($raw, true) ?: [];
} else {
    $data = $_POST;
}
$f = fn(string $k): string => trim((string)($data[$k] ?? ''));

// --- honeypot: bot vyplní → tváríme sa úspešne, nič neposielame ---
if ($f('_hp') !== '') {
    echo json_encode(['success' => true, 'message' => 'Ďakujeme.']);
    exit;
}

// --- validácia ---
$meno    = $f('meno');
$email   = $f('email');
$telefon = $f('telefon');
$typ     = $f('typ_projektu');
$sprava  = $f('sprava');
$errors = [];
if ($meno === '')   $errors[] = 'meno';
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) $errors[] = 'email';
// obrana proti header injection (CR/LF v adrese), aj keď PHPMailer filtruje
if (preg_match('/[\r\n]/', $email) || preg_match('/[\r\n]/', $meno)) $errors[] = 'email';
if ($telefon === '') $errors[] = 'telefon';
if ($sprava === '')  $errors[] = 'sprava';
if ($errors) {
    http_response_code(422);
    echo json_encode(['success' => false, 'message' => 'Vyplňte prosím povinné polia.', 'fields' => $errors]);
    exit;
}

// --- PHPMailer (composer vendor/) ---
$autoload = __DIR__ . '/vendor/autoload.php';
if (!is_file($autoload)) {
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

    $mail->Subject = 'Nová správa z auresa.sk — ' . $meno;
    $body  = "Meno: $meno\n";
    $body .= "E-mail: $email\n";
    $body .= "Telefón: $telefon\n";
    $body .= "Typ projektu: " . ($typ !== '' ? $typ : '—') . "\n\n";
    $body .= "Správa:\n$sprava\n";
    $mail->Body = $body;

    $mail->send();
    echo json_encode(['success' => true, 'message' => 'Ďakujeme. Ozveme sa Vám.']);
} catch (Exception $e) {
    http_response_code(500);
    error_log('auresa kontakt mailer: ' . $mail->ErrorInfo);
    echo json_encode(['success' => false, 'message' => 'Správu sa nepodarilo odoslať. Skúste neskôr alebo nám napíšte priamo na info@auresa.sk.']);
}
