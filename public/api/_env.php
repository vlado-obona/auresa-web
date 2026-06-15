<?php
// Jednoduchý .env loader (bez závislostí). .env leží mimo webrootu alebo
// chránený .htaccess — NIKDY nie je v gite.
function au_load_env(): void {
    static $loaded = false;
    if ($loaded) return;
    $candidates = [
        __DIR__ . '/.env',
        dirname(__DIR__) . '/.env',
        dirname(__DIR__, 2) . '/.env',
    ];
    foreach ($candidates as $path) {
        if (!is_readable($path)) continue;
        foreach (file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
            $line = trim($line);
            if ($line === '' || $line[0] === '#') continue;
            if (!str_contains($line, '=')) continue;
            [$k, $v] = explode('=', $line, 2);
            $k = trim($k);
            $v = trim($v, " \t\n\r\0\x0B\"'");
            if ($k !== '' && getenv($k) === false) {
                putenv("$k=$v");
                $_ENV[$k] = $v;
            }
        }
        break;
    }
    $loaded = true;
}

function au_env(string $key, ?string $default = null): ?string {
    au_load_env();
    $v = getenv($key);
    return ($v === false || $v === '') ? $default : $v;
}
