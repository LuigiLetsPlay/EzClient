import { useState } from 'react';
import { 
  Download, 
  ArrowRight, 
  Sparkles, 
  Cpu, 
  Layers, 
  CheckCircle2, 
  Play, 
  Box, 
  FolderDown, 
  Sliders, 
  User, 
  Zap
} from 'lucide-react';

const WindowsIcon = ({ className = "w-5 h-5" }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.901-1.799" />
  </svg>
);

export default function Hero() {
  const [activeRoute, setActiveRoute] = useState('home');

  return (
    <section className="relative overflow-hidden pt-8 pb-16 lg:py-20 border-b border-emerald-900/30 bg-mc-grid">
      {/* Background ambient lighting */}
      <div className="absolute top-1/4 left-1/4 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-emerald-500/10 rounded-full blur-[150px] pointer-events-none" />
      <div className="absolute top-1/2 right-10 -translate-y-1/2 w-[650px] h-[650px] bg-emerald-400/10 rounded-full blur-[180px] pointer-events-none" />

      <div className="page-container relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center min-h-[75vh]">
          
          {/* Left Column (5 of 12 cols) */}
          <div className="lg:col-span-5 flex flex-col items-start space-y-6">
            
            {/* Pixel Badge */}
            <div className="inline-flex items-center gap-2.5 px-3.5 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 shadow-[0_0_15px_rgba(34,197,94,0.2)]">
              <span className="w-2 h-2 rounded-sm bg-emerald-400 animate-mc-live" />
              <span className="font-pixel text-[11px] font-bold tracking-wider text-emerald-400">
                [ ⚡ FABRIC 26.2 • JAVA 25 ]
              </span>
            </div>

            {/* Headline */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-[1.08] text-white">
              Maximale FPS.
              <br />
              <span className="bg-gradient-to-r from-emerald-400 via-teal-300 to-green-200 text-transparent bg-clip-text">
                Pure Kontrolle.
              </span>
            </h1>

            {/* Subtitle */}
            <p className="text-base text-zinc-300 leading-relaxed max-w-xl">
              Der moderne Minecraft Client &amp; PySide6 Launcher optimiert auf <strong className="text-white">Fabric 26.2</strong> mit <strong className="text-emerald-400">Java 25</strong>. Ausgestattet mit Sodium &amp; Lithium, 20+ integrierten Mods, animierten Capes und 1-Klick Modrinth-Verwaltung.
            </p>

            {/* XP Level Bar Indicator */}
            <div className="w-full max-w-md bg-black/60 border border-emerald-900/50 rounded-xl p-3 space-y-2">
              <div className="flex justify-between items-center text-xs">
                <span className="font-pixel text-[10px] text-emerald-400 font-bold flex items-center gap-1.5">
                  <Zap className="w-3.5 h-3.5 text-emerald-400" />
                  PERFORMANCE LEVEL
                </span>
                <span className="font-pixel text-[11px] text-emerald-300 font-bold bg-emerald-500/20 px-2 py-0.5 rounded border border-emerald-500/30">
                  LVL 100
                </span>
              </div>
              <div className="w-full h-3 bg-zinc-900 rounded-full overflow-hidden p-0.5 border border-emerald-900/40">
                <div className="h-full xp-bar-fill rounded-full w-full" />
              </div>
              <div className="flex justify-between text-[10px] font-mono text-zinc-400">
                <span>Sodium • Lithium • Java 25</span>
                <span className="text-emerald-400 font-bold">Ultra-Low Latency</span>
              </div>
            </div>

            {/* CTAs */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 w-full sm:w-auto pt-2">
              <a
                href="#download"
                className="btn-emerald inline-flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl text-base"
              >
                <WindowsIcon className="w-5 h-5" />
                <span>Download für Windows</span>
                <span className="font-pixel text-[10px] bg-black/20 px-2 py-0.5 rounded text-black font-bold">
                  .exe
                </span>
              </a>

              <a
                href="https://discord.gg/ezclient"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 px-5 py-3.5 rounded-xl bg-white/[0.04] hover:bg-emerald-500/10 border border-emerald-900/40 text-zinc-300 hover:text-white font-semibold text-base transition-all duration-200"
              >
                <span>Discord</span>
                <ArrowRight className="w-4 h-4 text-emerald-400" />
              </a>
            </div>

            {/* Trust Badges */}
            <div className="grid grid-cols-2 sm:flex sm:flex-wrap gap-2.5 pt-2 w-full">
              {[
                { label: '100% Kostenlos', icon: Sparkles },
                { label: 'Fabric 26.2', icon: Cpu },
                { label: '20+ Mods', icon: Box },
                { label: 'Java 25 Ready', icon: CheckCircle2 },
              ].map((badge) => {
                const Icon = badge.icon;
                return (
                  <div
                    key={badge.label}
                    className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-950/30 border border-emerald-500/20 text-xs font-medium text-emerald-300"
                  >
                    <Icon className="w-3.5 h-3.5 text-emerald-400" />
                    <span>{badge.label}</span>
                  </div>
                );
              })}
            </div>

          </div>

          {/* Right Column (7 of 12 cols - 1:1 Recreated Launcher QML Interface) */}
          <div className="lg:col-span-7 relative">
            
            {/* Emerald Ambient Aura */}
            <div className="absolute -inset-4 bg-gradient-to-r from-emerald-500/20 via-teal-500/10 to-transparent rounded-3xl blur-2xl -z-10" />

            {/* Launcher Window Frame */}
            <div className="w-full bg-[#0A0A0F] border-2 border-emerald-500/30 rounded-2xl shadow-[0_0_40px_rgba(34,197,94,0.15)] overflow-hidden">
              
              {/* Window TitleBar (QML TitleBar.qml) */}
              <div className="h-10 bg-[#07130D] border-b border-[#1A1A28] px-4 flex items-center justify-between select-none">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full bg-[#F43F5E] hover:opacity-80 transition-opacity" />
                  <div className="w-3 h-3 rounded-full bg-[#FBBF24] hover:opacity-80 transition-opacity" />
                  <div className="w-3 h-3 rounded-full bg-[#22C55E] hover:opacity-80 transition-opacity" />
                  <span className="font-pixel text-[10px] text-zinc-400 ml-3 tracking-wider">
                    EZCLIENT • V1.8.2
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-mc-live" />
                  <span className="font-pixel text-[9px] text-emerald-400 bg-emerald-500/15 px-2 py-0.5 rounded border border-emerald-500/30">
                    FABRIC 26.2
                  </span>
                </div>
              </div>

              {/* Launcher Body: Sidebar + Main Content View */}
              <div className="flex flex-col sm:flex-row min-h-[380px]">
                
                {/* QML Sidebar (Sidebar.qml) */}
                <div className="w-full sm:w-48 bg-[#0A1710] border-b sm:border-b-0 sm:border-r border-[#1A1A28] p-3 flex flex-col justify-between flex-shrink-0">
                  <div className="space-y-4">
                    {/* Brand */}
                    <div className="flex items-center gap-2.5 px-2 py-1">
                      <div className="w-6 h-6 rounded-md bg-gradient-to-r from-[#22C55E] to-[#2BE88A] flex items-center justify-center text-black font-black text-xs">
                        E
                      </div>
                      <span className="font-bold text-sm text-white">EzClient</span>
                    </div>

                    {/* Section: EZCLIENT */}
                    <div>
                      <span className="font-pixel text-[8px] text-zinc-500 px-2 block mb-1">EZCLIENT</span>
                      <button
                        onClick={() => setActiveRoute('home')}
                        className={`w-full flex items-center gap-2.5 px-2.5 py-1.5 rounded-md text-xs transition-all ${
                          activeRoute === 'home'
                            ? 'bg-[#123323] text-[#86EFAC] font-bold border-l-2 border-emerald-400'
                            : 'text-zinc-400 hover:text-white hover:bg-white/[0.03]'
                        }`}
                      >
                        <Play className="w-3.5 h-3.5 text-emerald-400" />
                        <span>Home</span>
                      </button>
                    </div>

                    {/* Section: BIBLIOTHEK */}
                    <div>
                      <span className="font-pixel text-[8px] text-zinc-500 px-2 block mb-1">BIBLIOTHEK</span>
                      <div className="space-y-1">
                        {[
                          { id: 'profiles', label: 'Profile', icon: Box },
                          { id: 'mods', label: 'Mods', icon: Sliders },
                          { id: 'modrinth', label: 'Mod-Katalog', icon: FolderDown },
                          { id: 'capes', label: 'Capes', icon: Layers },
                        ].map((nav) => {
                          const Icon = nav.icon;
                          const active = activeRoute === nav.id;
                          return (
                            <button
                              key={nav.id}
                              onClick={() => setActiveRoute(nav.id)}
                              className={`w-full flex items-center gap-2.5 px-2.5 py-1.5 rounded-md text-xs transition-all ${
                                active
                                  ? 'bg-[#123323] text-[#86EFAC] font-bold border-l-2 border-emerald-400'
                                  : 'text-zinc-400 hover:text-white hover:bg-white/[0.03]'
                              }`}
                            >
                              <Icon className={`w-3.5 h-3.5 ${active ? 'text-emerald-400' : 'text-zinc-500'}`} />
                              <span>{nav.label}</span>
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  </div>

                  {/* Account Tile */}
                  <div className="pt-3 border-t border-[#1A1A28] flex items-center gap-2 px-1">
                    <div className="w-6 h-6 rounded bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
                      <User className="w-3.5 h-3.5" />
                    </div>
                    <div className="flex flex-col text-left overflow-hidden">
                      <span className="text-[11px] font-bold text-white truncate">Player</span>
                      <span className="text-[9px] text-emerald-400 font-mono">Microsoft Auth ✓</span>
                    </div>
                  </div>
                </div>

                {/* Main View Area (HomePage.qml / ProfileDetailPage.qml) */}
                <div className="flex-1 p-5 bg-[#0A0A0F] relative overflow-hidden flex flex-col justify-between">
                  
                  {/* Route: HOME */}
                  {activeRoute === 'home' && (
                    <div className="space-y-4">
                      {/* Active Profile Card */}
                      <div className="relative rounded-xl border border-emerald-500/30 bg-gradient-to-b from-[#12121B] to-[#0A0A0F] p-4 overflow-hidden shadow-lg">
                        <div className="flex items-start justify-between">
                          <div>
                            <span className="font-pixel text-[8px] text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                              AKTIVES PROFIL
                            </span>
                            <h3 className="text-base font-bold text-white mt-1.5">
                              26.2 - Performance Profil
                            </h3>
                            <p className="text-xs text-zinc-400 mt-0.5">
                              Fabric 26.2 • Java 25 • 4096 MB RAM
                            </p>
                          </div>
                          <span className="font-pixel text-[9px] text-emerald-300 bg-emerald-950/60 px-2 py-1 rounded border border-emerald-500/30">
                            SODIUM AKTIV
                          </span>
                        </div>

                        {/* Big SPIELEN Launch Button */}
                        <button className="btn-emerald w-full mt-4 py-3 rounded-xl text-sm font-black flex items-center justify-center gap-2 tracking-wide">
                          <Play className="w-4 h-4 fill-black stroke-none" />
                          <span>SPIELEN (26.2)</span>
                        </button>
                      </div>

                      {/* Quick Module Status List */}
                      <div className="grid grid-cols-2 gap-2 text-xs">
                        <div className="p-2.5 rounded-lg bg-[#12121B] border border-[#1A1A28] flex items-center justify-between">
                          <span className="text-zinc-300 text-[11px]">20+ Mods</span>
                          <span className="font-pixel text-[8px] text-emerald-400">GELADEN</span>
                        </div>
                        <div className="p-2.5 rounded-lg bg-[#12121B] border border-[#1A1A28] flex items-center justify-between">
                          <span className="text-zinc-300 text-[11px]">Capes Live-Sync</span>
                          <span className="font-pixel text-[8px] text-emerald-400">ONLINE</span>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Route: PROFILES */}
                  {activeRoute === 'profiles' && (
                    <div className="space-y-3">
                      <div className="flex justify-between items-center pb-2 border-b border-[#1A1A28]">
                        <span className="font-pixel text-[10px] text-emerald-400">PROFILE VERWALTEN</span>
                        <span className="text-[10px] text-zinc-500 font-mono">2 Instanzen</span>
                      </div>
                      <div className="space-y-2">
                        <div className="p-3 rounded-lg bg-[#123323] border border-emerald-500/30 flex items-center justify-between">
                          <div>
                            <span className="font-bold text-xs text-white block">26.2 - Performance Profil</span>
                            <span className="text-[10px] text-emerald-300 font-mono">Fabric 26.2 • Standard</span>
                          </div>
                          <span className="font-pixel text-[8px] text-emerald-400 bg-emerald-500/20 px-2 py-0.5 rounded">AKTIV</span>
                        </div>
                        <div className="p-3 rounded-lg bg-[#12121B] border border-[#1A1A28] flex items-center justify-between opacity-70">
                          <div>
                            <span className="font-bold text-xs text-white block">26.1 - Legacy Profile</span>
                            <span className="text-[10px] text-zinc-400 font-mono">Fabric 26.1 • Clean</span>
                          </div>
                          <button className="text-[10px] text-zinc-300 hover:text-white bg-white/[0.05] px-2 py-1 rounded">Wählen</button>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Route: MODS */}
                  {activeRoute === 'mods' && (
                    <div className="space-y-3">
                      <div className="flex justify-between items-center pb-2 border-b border-[#1A1A28]">
                        <span className="font-pixel text-[10px] text-emerald-400">INTEGRIERTE MODS</span>
                        <span className="text-[10px] text-emerald-400 font-mono">20+ Mods</span>
                      </div>
                      <div className="space-y-1.5 text-xs">
                        {[
                          { name: 'Sodium', desc: 'Shader-Rendering & FPS Boost' },
                          { name: 'Lithium', desc: 'Physik- & Chunk-Engine' },
                          { name: 'Iris Shaders', desc: 'Shaderpack-Unterstützung' },
                          { name: 'FerriteCore', desc: 'Reduzierter RAM-Bedarf' },
                        ].map((mod) => (
                          <div key={mod.name} className="p-2 rounded bg-[#12121B] border border-[#1A1A28] flex items-center justify-between">
                            <span className="font-bold text-white text-[11px]">{mod.name}</span>
                            <span className="font-pixel text-[8px] text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded">AN</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Route: MODRINTH */}
                  {activeRoute === 'modrinth' && (
                    <div className="space-y-3">
                      <div className="flex justify-between items-center pb-2 border-b border-[#1A1A28]">
                        <span className="font-pixel text-[10px] text-emerald-400">MODRINTH KATALOG</span>
                        <span className="text-[10px] text-zinc-400 font-mono">API v2</span>
                      </div>
                      <div className="space-y-2">
                        <div className="p-2.5 rounded bg-[#12121B] border border-[#1A1A28] flex items-center justify-between text-xs">
                          <div>
                            <span className="font-bold text-white block text-[11px]">Entity Culling</span>
                            <span className="text-[10px] text-zinc-400">Blockiert unsichtbare Mobs</span>
                          </div>
                          <button className="btn-emerald text-[10px] px-2.5 py-1 rounded">Installieren</button>
                        </div>
                        <div className="p-2.5 rounded bg-[#12121B] border border-[#1A1A28] flex items-center justify-between text-xs">
                          <div>
                            <span className="font-bold text-white block text-[11px]">ImmediatelyFast</span>
                            <span className="text-[10px] text-zinc-400">HUD &amp; GUI Beschleuniger</span>
                          </div>
                          <button className="btn-emerald text-[10px] px-2.5 py-1 rounded">Installieren</button>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Route: CAPES */}
                  {activeRoute === 'capes' && (
                    <div className="space-y-3 text-center">
                      <span className="font-pixel text-[10px] text-emerald-400 block pb-1 border-b border-[#1A1A28]">
                        ANIMIERTER CAPE-EDITOR
                      </span>
                      <div className="w-16 h-24 mx-auto rounded-lg bg-gradient-to-b from-emerald-500 via-teal-500 to-green-900 border border-emerald-400 shadow-[0_0_20px_rgba(34,197,94,0.4)] flex items-center justify-center animate-pulse">
                        <span className="font-pixel text-[10px] font-black text-white">EZ</span>
                      </div>
                      <span className="text-xs text-zinc-300 block">Live-Sync mit dem Community-Server</span>
                    </div>
                  )}

                  {/* Bottom Bar Info in Launcher */}
                  <div className="pt-3 border-t border-[#1A1A28] flex items-center justify-between text-[10px] font-mono text-zinc-500">
                    <span>%APPDATA%\.ezclient</span>
                    <span className="text-emerald-400 font-bold">Bereit</span>
                  </div>

                </div>

              </div>

            </div>

          </div>

        </div>
      </div>
    </section>
  );
}
