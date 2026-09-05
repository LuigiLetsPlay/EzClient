import { Download, Check, Sparkles, FileCode, CheckCircle2, Clock } from 'lucide-react';

const WindowsIcon = ({ className = "w-6 h-6" }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.901-1.799" />
  </svg>
);

const AppleIcon = ({ className = "w-6 h-6" }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M15.97 6.85c.65-.79 1.09-1.89.97-2.99-.95.04-2.1.63-2.78 1.42-.59.68-1.11 1.79-.97 2.87 1.06.08 2.13-.51 2.78-1.3" />
  </svg>
);

const LinuxIcon = ({ className = "w-6 h-6" }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M12.002 0c-4.417 0-8.002 3.585-8.002 8.002 0 1.246.286 2.428.795 3.483C2.102 12.83 0 15.65 0 18.998 0 21.758 2.242 24 5.002 24c.734 0 1.43-.16 2.057-.447 1.44.927 3.145 1.449 4.943 1.449 1.797 0 3.502-.522 4.942-1.449.627.287 1.323.447 2.057.447 2.76 0 5.002-2.242 5.002-5.002 0-3.348-2.102-6.168-4.795-7.513.509-1.055.795-2.237.795-3.483C20.004 3.585 16.419 0 12.002 0z" />
  </svg>
);

export default function DownloadHub() {
  return (
    <section id="download" className="relative py-20 lg:py-28 bg-[#050805] border-b border-emerald-900/30 overflow-hidden">
      {/* Background Ambient Glow */}
      <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-[700px] h-[700px] bg-emerald-500/10 rounded-full blur-[180px] pointer-events-none" />

      <div className="page-container relative z-10">
        
        {/* Header */}
        <div className="text-center max-w-2xl mx-auto mb-16 space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30">
            <Download className="w-3.5 h-3.5 text-emerald-400" />
            <span className="font-pixel text-[10px] font-bold tracking-wider text-emerald-400">
              DOWNLOAD • V1.8.2
            </span>
          </div>

          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-tight">
            Hol dir EzClient.
          </h2>

          <p className="text-sm sm:text-base text-zinc-400 leading-relaxed">
            Aktuell optimiert und verfügbar für Windows. Mac- und Linux-Nutzer können die plattformunabhängige Fabric Mod JAR nutzen.
          </p>
        </div>

        {/* Platform Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          
          {/* WINDOWS CARD (Primary Supported Platform) */}
          <div className="bg-[#080d09] border-2 border-emerald-500/60 rounded-3xl p-6 sm:p-8 flex flex-col justify-between relative shadow-[0_0_35px_rgba(34,197,94,0.18)] group">
            
            {/* Live Badge */}
            <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 bg-emerald-400 text-black font-pixel text-[9px] font-black px-4 py-1 rounded-full uppercase tracking-wider shadow-[0_0_15px_#22c55e]">
              ★ WINDOWS LIVE
            </div>

            <div>
              <div className="flex items-center justify-between mb-6 pt-2">
                <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                  <WindowsIcon className="w-6 h-6" />
                </div>
                <span className="font-pixel text-[10px] text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded border border-emerald-500/20">
                  v1.8.2
                </span>
              </div>

              <h3 className="text-2xl font-black text-white mb-2">
                Windows
              </h3>
              <p className="text-xs text-zinc-300 mb-6">
                Windows 10 &amp; 11 (64-Bit). Vollautomatischer Installer mit Desktop-Verknüpfung und Auto-Updates.
              </p>

              <div className="space-y-2 mb-6">
                {['Automatisches Auto-Update', 'Integrierte Java 25 Runtime', 'DirectX & Vulkan GPU Support'].map((feat) => (
                  <div key={feat} className="flex items-center gap-2 text-xs text-zinc-300">
                    <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                    <span>{feat}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="space-y-2.5 pt-4 border-t border-emerald-900/40">
              <a
                href="https://github.com/LuigiLetsPlay/EzClient/releases/latest/download/EzClient-Setup.exe"
                className="btn-emerald w-full py-3.5 rounded-xl text-sm flex items-center justify-center gap-2 text-center"
              >
                <Download className="w-4 h-4 stroke-[2.5]" />
                <span>Installer (.exe) laden</span>
              </a>

              <a
                href="https://github.com/LuigiLetsPlay/EzClient/releases/latest/download/EzClient.exe"
                className="w-full py-2.5 rounded-xl bg-black/50 hover:bg-white/[0.06] border border-emerald-900/40 text-zinc-300 hover:text-white font-medium text-xs flex items-center justify-center gap-2 transition-colors"
              >
                <span>Standalone Executable (.exe)</span>
              </a>
            </div>

          </div>

          {/* MACOS CARD (In Development) */}
          <div className="bg-[#080d09]/70 border border-white/[0.08] rounded-3xl p-6 sm:p-8 flex flex-col justify-between opacity-75 group">
            
            <div>
              <div className="flex items-center justify-between mb-6">
                <div className="w-12 h-12 rounded-2xl bg-white/[0.04] border border-white/[0.08] flex items-center justify-center text-zinc-400">
                  <AppleIcon className="w-6 h-6" />
                </div>
                <span className="font-pixel text-[9px] text-zinc-500 bg-white/[0.04] px-2.5 py-1 rounded border border-white/[0.08]">
                  IN ENTWICKLUNG
                </span>
              </div>

              <h3 className="text-2xl font-black text-white mb-2">
                macOS
              </h3>
              <p className="text-xs text-zinc-400 mb-6">
                Ein nativer macOS-Launcher befindet sich aktuell in Vorbereitung. Nutze vorerst die Fabric Mod JAR in Prism Launcher oder Modrinth App.
              </p>

              <div className="space-y-2 mb-6">
                {['Apple Silicon M-Series Support (Geplant)', 'Retina Interface Skalierung'].map((feat) => (
                  <div key={feat} className="flex items-center gap-2 text-xs text-zinc-500">
                    <Clock className="w-3.5 h-3.5 text-zinc-500 flex-shrink-0" />
                    <span>{feat}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-4 border-t border-white/[0.08]">
              <a
                href="#mod-jar"
                className="w-full py-3 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] border border-white/[0.08] text-zinc-300 text-xs font-medium flex items-center justify-center gap-2 transition-colors"
              >
                <span>Mod JAR für Mac nutzen ↓</span>
              </a>
            </div>

          </div>

          {/* LINUX CARD (In Development) */}
          <div className="bg-[#080d09]/70 border border-white/[0.08] rounded-3xl p-6 sm:p-8 flex flex-col justify-between opacity-75 group">
            
            <div>
              <div className="flex items-center justify-between mb-6">
                <div className="w-12 h-12 rounded-2xl bg-white/[0.04] border border-white/[0.08] flex items-center justify-center text-zinc-400">
                  <LinuxIcon className="w-6 h-6" />
                </div>
                <span className="font-pixel text-[9px] text-zinc-500 bg-white/[0.04] px-2.5 py-1 rounded border border-white/[0.08]">
                  IN ENTWICKLUNG
                </span>
              </div>

              <h3 className="text-2xl font-black text-white mb-2">
                Linux
              </h3>
              <p className="text-xs text-zinc-400 mb-6">
                Ein nativer Linux AppImage Build ist für zukünftige Versionen geplant. Nutze auf Linux vorerst die Standalone Fabric Mod JAR.
              </p>

              <div className="space-y-2 mb-6">
                {['Wayland & X11 Optimierung (Geplant)', 'Mesa / Vulkan GPU Treiber Support'].map((feat) => (
                  <div key={feat} className="flex items-center gap-2 text-xs text-zinc-500">
                    <Clock className="w-3.5 h-3.5 text-zinc-500 flex-shrink-0" />
                    <span>{feat}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-4 border-t border-white/[0.08]">
              <a
                href="#mod-jar"
                className="w-full py-3 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] border border-white/[0.08] text-zinc-300 text-xs font-medium flex items-center justify-center gap-2 transition-colors"
              >
                <span>Mod JAR für Linux nutzen ↓</span>
              </a>
            </div>

          </div>

        </div>

        {/* Java 25 Runtime Notice */}
        <div className="bg-emerald-950/25 border border-emerald-500/30 rounded-2xl p-4 sm:p-6 mb-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center flex-shrink-0 text-emerald-400">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-white">
                Java 25 JRE ist im Launcher integriert
              </h4>
              <p className="text-xs text-zinc-300">
                Keine separate Java-Installation erforderlich. EzClient konfiguriert die ideale Java 25 Laufzeitumgebung automatisch.
              </p>
            </div>
          </div>
          <span className="font-pixel text-[10px] text-emerald-300 bg-emerald-500/20 px-3 py-1.5 rounded-lg border border-emerald-500/30 whitespace-nowrap">
            JAVA 25 BUNDLED ✓
          </span>
        </div>

        {/* Direct Mod JAR Download (Universal for all Launchers & OS) */}
        <div id="mod-jar" className="bg-[#080d09] border border-emerald-900/40 rounded-2xl p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <FileCode className="w-6 h-6 text-emerald-400 flex-shrink-0" />
            <div>
              <h4 className="text-sm font-bold text-white">
                Standalone Fabric Mod JAR (Plattformunabhängig)
              </h4>
              <p className="text-xs text-zinc-400">
                Für Prism Launcher, Modrinth App, MultiMC oder den offiziellen Minecraft Launcher auf Windows, Mac &amp; Linux.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
            <a
              href="https://github.com/LuigiLetsPlay/EzClient/releases/latest/download/EzClient-1.8.2.jar"
              className="px-4 py-2.5 rounded-xl bg-black/60 hover:bg-emerald-500/10 border border-emerald-500/30 text-xs font-pixel text-emerald-300 hover:text-white flex items-center gap-2 transition-all"
            >
              <Download className="w-3.5 h-3.5 text-emerald-400" />
              <span>EzClient-1.8.2.jar</span>
            </a>
            <a
              href="https://github.com/LuigiLetsPlay/EzClient/releases/latest/download/EzClient-Lite-1.8.2.jar"
              className="px-4 py-2.5 rounded-xl bg-black/60 hover:bg-white/[0.08] border border-white/[0.08] text-xs font-pixel text-zinc-300 hover:text-white flex items-center gap-2 transition-all"
            >
              <Download className="w-3.5 h-3.5 text-zinc-400" />
              <span>EzClient-Lite-1.8.2.jar</span>
            </a>
          </div>
        </div>

      </div>
    </section>
  );
}
