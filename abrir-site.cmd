@echo off
rem ================================================================
rem  TJPE - Validacao Inteligente de Procuracoes
rem  Abre o sistema completo com um comando so:
rem    - no terminal (dentro da pasta do projeto):  .\abrir-site.cmd
rem    - ou com dois cliques neste arquivo
rem  Para desligar o sistema, feche a janela "TJPE - Servidor".
rem ================================================================
setlocal
cd /d "%~dp0"
set "URL=http://localhost:8080"

rem Se o sistema ja estiver rodando, so abre o navegador
"%SystemRoot%\System32\curl.exe" -s -o nul -m 2 %URL%/
if not errorlevel 1 goto abrir

echo.
echo  Iniciando o sistema TJPE - Validacao Inteligente de Procuracoes...
echo  Uma janela "TJPE - Servidor" vai abrir. Deixe-a aberta enquanto usar o site.
echo  Aguarde: na primeira vez pode levar ate 1 minuto.
echo.
start "TJPE - Servidor (feche esta janela para desligar o sistema)" cmd /k call "%~dp0mvnw.cmd" spring-boot:run "-Dspring-boot.run.profiles=h2"

set /a tentativas=0
:esperar
set /a tentativas+=1
if %tentativas% gtr 90 goto falhou
ping -n 3 127.0.0.1 >nul
"%SystemRoot%\System32\curl.exe" -s -o nul -m 2 %URL%/
if errorlevel 1 goto esperar

:abrir
echo  Sistema pronto! Abrindo %URL% no navegador...
start "" %URL%
exit /b 0

:falhou
echo  O sistema nao respondeu em 3 minutos.
echo  Veja a janela "TJPE - Servidor" para entender o erro.
exit /b 1
