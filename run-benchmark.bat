@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0benchmark\run-benchmark.ps1" %*
