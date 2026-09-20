@echo off
title Building EzClient v2.2.1 Release
echo ==========================================
echo    EzClient v2.2.1 Official Release Build
echo ==========================================
pip install -r requirements.txt
python build_release.py
pause
