<?php
/**
 * Decap CMS — GitHub OAuth proxy.
 * Krok 1: GET /api/auth        → presmeruje na GitHub authorize (+ state cookie).
 * Krok 2: GET /api/auth?code=  → overí state, vymení code za access_token a
 *         pošle postMessage späť do Decap okna (len na dôveryhodný origin).
 * Client ID/Secret z .env (nikdy hardcode).
 */
declare(strict_types=1);
require __DIR__ . '/_env.php';

const ALLOWED_ORIGIN = 'https://auresa.sk';

$clientId     = au_env('GITHUB_CLIENT_ID', '');
$clientSecret = au_env('GITHUB_CLIENT_SECRET', '');
$redirectUri  = ALLOWED_ORIGIN . '/api/auth';

if ($clientId === '' || $clientSecret === '') {
    http_response_code(500);
    header('Content-Type: text/plain; charset=utf-8');
    echo 'OAuth nie je nakonfigurovaný (chýba GITHUB_CLIENT_ID/SECRET v .env).';
    exit;
}

$code  = $_GET['code'] ?? null;
$state = $_GET['state'] ?? null;

// Krok 1 — presmerovanie na GitHub (+ uložiť state do cookie pre CSRF ochranu)
if ($code === null) {
    $newState = bin2hex(random_bytes(16));
    setcookie('au_oauth_state', $newState, [
        'expires'  => time() + 600,
        'path'     => '/api/',
        'secure'   => true,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    $url = 'https://github.com/login/oauth/authorize?' . http_build_query([
        'client_id'    => $clientId,
        'redirect_uri' => $redirectUri,
        'scope'        => 'repo,user',
        'state'        => $newState,
    ]);
    header('Location: ' . $url);
    exit;
}

// Krok 2 — overenie state (CSRF)
$cookieState = $_COOKIE['au_oauth_state'] ?? '';
setcookie('au_oauth_state', '', ['expires' => time() - 3600, 'path' => '/api/']);
if (!$state || !$cookieState || !hash_equals($cookieState, (string) $state)) {
    http_response_code(400);
    header('Content-Type: text/plain; charset=utf-8');
    echo 'Neplatný alebo chýbajúci state parameter (CSRF ochrana).';
    exit;
}

// Krok 3 — výmena code za token
$ch = curl_init('https://github.com/login/oauth/access_token');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_TIMEOUT        => 15,
    CURLOPT_HTTPHEADER     => ['Accept: application/json'],
    CURLOPT_POSTFIELDS     => http_build_query([
        'client_id'     => $clientId,
        'client_secret' => $clientSecret,
        'code'          => $code,
        'redirect_uri'  => $redirectUri,
    ]),
]);
$resp = curl_exec($ch);
$curlErr = curl_error($ch);
curl_close($ch);

header('Content-Type: text/html; charset=utf-8');

if ($resp === false) {
    error_log('auresa OAuth curl error: ' . $curlErr);
    $message = 'authorization:github:error:' . json_encode(['provider' => 'github', 'error' => 'network']);
} else {
    $token = json_decode((string) $resp, true)['access_token'] ?? null;
    if (!$token) {
        $message = 'authorization:github:error:' . json_encode(['provider' => 'github', 'error' => 'no_token']);
    } else {
        $message = 'authorization:github:success:' . json_encode(['provider' => 'github', 'token' => $token]);
    }
}
?>
<!doctype html>
<html><head><meta charset="utf-8"></head><body>
<script>
(function () {
  var ORIGIN = <?php echo json_encode(ALLOWED_ORIGIN); ?>;
  var MESSAGE = <?php echo json_encode($message); ?>;
  function receiveMessage(e) {
    if (e.origin !== ORIGIN) return;          // posielaj len dôveryhodnému oknu
    window.opener.postMessage(MESSAGE, ORIGIN);
  }
  window.addEventListener('message', receiveMessage, false);
  window.opener && window.opener.postMessage('authorizing:github', ORIGIN);
})();
</script>
<p>Prihlasovanie dokončené, môžete zavrieť toto okno.</p>
</body></html>
