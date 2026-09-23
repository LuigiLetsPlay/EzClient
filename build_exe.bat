@echo off
title Building EzClient v2.2.3 Release
echo ==========================================
echo    EzClient v2.2.3 Official Release Build
echo ==========================================
pip install -r requirements.txt
python build_release.py
pause
