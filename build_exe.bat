@echo off
title Building EzClient v2.0.0 Release
echo ==========================================
echo    EzClient v2.0.0 Official Release Build
echo ==========================================
pip install -r requirements.txt
python build_release.py
pause
