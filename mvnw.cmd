@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script para TJPE Validador Procuracoes
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "USER_MAVEN=C:\Users\vasconcelos\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd"

if exist "%USER_MAVEN%" (
    "%USER_MAVEN%" %*
) else (
    mvn %*
)
