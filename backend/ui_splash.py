import sys
from PySide6.QtWidgets import QSplashScreen
from PySide6.QtGui import QPixmap, QPainter, QColor, QFont, QLinearGradient, QPen, QBrush
from PySide6.QtCore import Qt


class EzSplashScreen(QSplashScreen):
    """
    Ultra-fast native startup splash screen with modern dark glassmorphism
    and vivid EzClient neon green branding.
    """

    def __init__(self, version: str = "1.0.4"):
        self.version = version
        self.current_msg = "EzClient wird geladen…"
        self.current_pct = 15
        
        pix = self._render_pixmap()
        super().__init__(pix, Qt.WindowStaysOnTopHint | Qt.FramelessWindowHint)
        self.setAttribute(Qt.WA_TranslucentBackground)

    def setMessage(self, message: str, percent: int = 50) -> None:
        self.current_msg = message
        self.current_pct = max(0, min(100, percent))
        self.setPixmap(self._render_pixmap())

    def _render_pixmap(self) -> QPixmap:
        width = 480
        height = 240
        pix = QPixmap(width, height)
        pix.fill(Qt.transparent)

        painter = QPainter(pix)
        painter.setRenderHint(QPainter.Antialiasing)

        # 1. Background Card with Gradient & Subtle Border
        card_grad = QLinearGradient(0, 0, width, height)
        card_grad.setColorAt(0.0, QColor("#12141C"))
        card_grad.setColorAt(1.0, QColor("#090A0F"))

        painter.setBrush(QBrush(card_grad))
        painter.setPen(QPen(QColor("#242838"), 1))
        painter.drawRoundedRect(1, 1, width - 2, height - 2, 16, 16)

        # 2. Glowing Neon Top Accent
        glow = QLinearGradient(0, 0, width, 0)
        glow.setColorAt(0.0, QColor(34, 201, 110, 0))
        glow.setColorAt(0.5, QColor(34, 201, 110, 180))
        glow.setColorAt(1.0, QColor(34, 201, 110, 0))
        painter.setPen(QPen(QBrush(glow), 2))
        painter.drawLine(24, 2, width - 24, 2)

        # 3. Logo Badge
        painter.setBrush(QColor("#16281E"))
        painter.setPen(QPen(QColor("#22C96E"), 1.5))
        painter.drawRoundedRect(36, 36, 56, 56, 14, 14)

        painter.setPen(QColor("#22C96E"))
        painter.setFont(QFont("Segoe UI", 24, QFont.Bold))
        painter.drawText(36, 36, 56, 56, Qt.AlignCenter, "⚡")

        # 4. App Title & Version
        painter.setPen(QColor("#F0F0F5"))
        painter.setFont(QFont("Segoe UI", 20, QFont.Bold))
        painter.drawText(108, 62, "EZCLIENT")

        painter.setFont(QFont("Segoe UI", 10, QFont.Bold))
        painter.setPen(QColor("#22C96E"))
        painter.drawText(230, 61, f"v{self.version}")

        # Subtitle
        painter.setPen(QColor("#8A8F9E"))
        painter.setFont(QFont("Segoe UI", 11))
        painter.drawText(108, 84, "High-Performance Minecraft Client")

        # 5. Status Text
        painter.setPen(QColor("#8A90A0"))
        painter.setFont(QFont("Segoe UI", 10))
        painter.drawText(36, 160, self.current_msg)

        # Percentage indicator
        painter.setPen(QColor("#22C96E"))
        painter.setFont(QFont("Segoe UI", 10, QFont.Bold))
        pct_str = f"{int(self.current_pct)}%"
        painter.drawText(width - 36 - 40, 160, pct_str)

        # 6. Progress Bar Track
        track_x = 36
        track_y = 178
        track_w = width - 72
        track_h = 6

        painter.setBrush(QColor("#181B26"))
        painter.setPen(Qt.NoPen)
        painter.drawRoundedRect(track_x, track_y, track_w, track_h, 3, 3)

        # Progress Fill with Gradient
        fill_w = int(track_w * (self.current_pct / 100.0))
        if fill_w > 0:
            fill_grad = QLinearGradient(track_x, 0, track_x + track_w, 0)
            fill_grad.setColorAt(0.0, QColor("#22C96E"))
            fill_grad.setColorAt(1.0, QColor("#5AEEA0"))
            painter.setBrush(QBrush(fill_grad))
            painter.drawRoundedRect(track_x, track_y, max(6, fill_w), track_h, 3, 3)

        painter.end()
        return pix
