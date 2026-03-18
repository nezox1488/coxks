@echo off
REM Сборка с отдельным кэшем — обходит блокировку mappings.jar от Java extension
REM Используй этот скрипт вместо runClient в Cursor

set GRADLE_USER_HOME=%~dp0.gradle-cache
echo Using cache: %GRADLE_USER_HOME%
call gradlew.bat runClient %*
