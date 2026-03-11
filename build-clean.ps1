# Сборка RichClient без Cursor (чтобы Java extension не блокировал mappings.jar)
# Запусти этот скрипт из PowerShell ПОСЛЕ закрытия Cursor

Write-Host "Останавливаем Gradle daemon..." -ForegroundColor Yellow
& .\gradlew --stop 2>$null

Write-Host "Удаляем кэш fabric-loom..." -ForegroundColor Yellow
$loomCache = "$env:USERPROFILE\.gradle\caches\fabric-loom\1.21.4"
if (Test-Path $loomCache) {
    Remove-Item -Recurse -Force $loomCache -ErrorAction SilentlyContinue
    if (Test-Path $loomCache) {
        Write-Host "ОШИБКА: Не удалось удалить кэш. Закрой Cursor и все Java-процессы!" -ForegroundColor Red
        exit 1
    }
}

Write-Host "Запускаем сборку..." -ForegroundColor Green
& .\gradlew runClient --no-daemon
