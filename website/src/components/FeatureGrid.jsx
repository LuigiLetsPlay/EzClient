import { useState } from 'react';
import { Zap, Box, FolderDown, Sliders, Check, Eye, Crosshair, ShieldAlert, Gamepad2, Move, Sparkles } from 'lucide-react';

const clientModules = [
  { id: 'fps', name: 'FPS Counter', desc: 'Echtzeit-Framerate Overlay', defaultOn: true },
  { id: 'glass', name: 'Clear Glass', desc: 'Connected Textures ohne Mittelkanten', defaultOn: true },
  { id: 'keystrokes', name: 'Keystrokes', desc: 'WASD & Mausklicks Visualisierung', defaultOn: true },
  { id: 'cps', name: 'CPS Modul', desc: 'Clicks Per Second (LMB & RMB)', defaultOn: true },
  { id: 'armor', name: 'Armor Status', desc: 'Rüstung & Haltbarkeitsanzeige', defaultOn: true },
  { id: 'zoom', name: 'OptiFine Zoom', desc: 'Stufenloser Kamerazoom', defaultOn: true },
  { id: 'fullbright', name: 'Fullbright', desc: 'Optimierte Gamma-Helligkeit', defaultOn: true },
  { id: 'sprint', name: 'Toggle Sprint', desc: 'Automatisches Sprinten & Sneaken', defaultOn: true },
];

export default function FeatureGrid() {
  const [activeModules, setActiveModules] = useState(
    clientModules.reduce((acc, m) => ({ ...acc, [m.id]: m.defaultOn }), {})
  );

  const toggleModule = (id) => {
    setActiveModules((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <section id="features" className="relative py-16 lg:py-20 bg-[#050805] border-b border-emerald-900/30 overflow-hidden">
      <div className="page-container relative z-10">
        
        {/* Minimalist Section Header */}
        <div className="text-center max-w-xl mx-auto mb-12 space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-mc-live" />
            <span className="font-pixel text-[10px] font-bold tracking-wider text-emerald-400">
              DIE 3 KERN-SÄULEN
            </span>
          </div>

          <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-white tracking-tight">
            Alles in einem Client.
          </h2>

          <p className="text-xs sm:text-sm text-zinc-400">
            Von der hauseigenen EzRenderer-Engine bis zu 20+ integrierten Modulen.
          </p>
        </div>

        {/* 3 Main Pillars Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

          {/* SÄULE 1: EzRenderer & Extreme Performance */}
          <div className="bg-[#080d09] border border-emerald-900/40 hover:border-emerald-500/40 rounded-2xl p-6 transition-all duration-200 flex flex-col justify-between shadow-xl">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                  <Zap className="w-5 h-5" />
                </div>
                <span className="font-pixel text-[9px] text-emerald-400 bg-emerald-500/15 px-2.5 py-1 rounded border border-emerald-500/30">
                  EZRENDERER
                </span>
              </div>

              <h3 className="text-xl font-bold text-white mb-2">
                EzRenderer &amp; FPS-Engine
              </h3>
              <p className="text-xs text-zinc-400 leading-relaxed mb-5">
                Die hauseigene <strong className="text-white">EzRenderer</strong>-Technologie bündelt vorinstalliertes <strong className="text-emerald-400">Sodium &amp; Lithium</strong> mit entkoppelten Render-Pipelines für maximale Bildraten und verzögerungsfreie Frametimes auf Java 25.
              </p>
            </div>

            {/* Performance Stats */}
            <div className="space-y-2 pt-2 border-t border-emerald-900/30">
              <div className="flex items-center justify-between text-xs py-1.5 px-2 bg-black/40 rounded-lg border border-emerald-900/20">
                <span className="text-zinc-400 text-[11px]">Sodium &amp; Lithium Integration</span>
                <span className="font-pixel text-[9px] text-emerald-400">AKTIV</span>
              </div>
              <div className="flex items-center justify-between text-xs py-1.5 px-2 bg-black/40 rounded-lg border border-emerald-900/20">
                <span className="text-zinc-400 text-[11px]">Render-Thread Entkopplung</span>
                <span className="font-pixel text-[9px] text-emerald-400">0 MS LAG</span>
              </div>
              <div className="flex items-center justify-between text-xs py-1.5 px-2 bg-black/40 rounded-lg border border-emerald-900/20">
                <span className="text-zinc-400 text-[11px]">Java 25 JRE Optimierung</span>
                <span className="font-pixel text-[9px] text-emerald-400">+350% FPS</span>
              </div>
            </div>
          </div>

          {/* SÄULE 2: 20+ Schaltbare Module */}
          <div className="bg-[#080d09] border border-emerald-900/40 hover:border-emerald-500/40 rounded-2xl p-6 transition-all duration-200 flex flex-col justify-between shadow-xl">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                  <Sliders className="w-5 h-5" />
                </div>
                <span className="font-pixel text-[9px] text-emerald-400 bg-emerald-500/15 px-2.5 py-1 rounded border border-emerald-500/30">
                  20+ MODULE
                </span>
              </div>

              <h3 className="text-xl font-bold text-white mb-2">
                20+ Integrierte Module
              </h3>
              <p className="text-xs text-zinc-400 leading-relaxed mb-3">
                Direkt im Client und Launcher mit einem Klick aktivierbar – von Clear Glass bis Keystrokes.
              </p>
            </div>

            {/* Interactive Modules List */}
            <div className="space-y-1.5 bg-[#050805] p-2 rounded-xl border border-emerald-900/30 max-h-[175px] overflow-y-auto">
              {clientModules.map((mod) => {
                const on = activeModules[mod.id];
                return (
                  <button
                    key={mod.id}
                    onClick={() => toggleModule(mod.id)}
                    className="w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg bg-black/40 hover:bg-black/80 border border-emerald-900/20 text-xs transition-colors text-left"
                  >
                    <div>
                      <span className="font-bold text-white text-[11px] block leading-tight">{mod.name}</span>
                      <span className="text-[9px] text-zinc-500">{mod.desc}</span>
                    </div>
                    <span className={`font-pixel text-[8px] px-1.5 py-0.5 rounded transition-colors ${
                      on ? 'bg-emerald-500 text-black font-bold' : 'bg-zinc-800 text-zinc-500'
                    }`}>
                      {on ? 'AN' : 'AUS'}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* SÄULE 3: Mod-Katalog (Modrinth & CurseForge) */}
          <div className="bg-[#080d09] border border-emerald-900/40 hover:border-emerald-500/40 rounded-2xl p-6 transition-all duration-200 flex flex-col justify-between shadow-xl">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                  <FolderDown className="w-5 h-5" />
                </div>
                <span className="font-pixel text-[9px] text-emerald-400 bg-emerald-500/15 px-2.5 py-1 rounded border border-emerald-500/30">
                  MODRINTH &amp; CURSE
                </span>
              </div>

              <h3 className="text-xl font-bold text-white mb-2">
                1-Klick Mod-Katalog
              </h3>
              <p className="text-xs text-zinc-400 leading-relaxed mb-5">
                Installiere zusätzliche Fabric-Mods, Shaderpacks und Texturen direkt im Launcher aus <strong className="text-white">Modrinth</strong> und <strong className="text-white">CurseForge</strong> – ohne manuelles Kopieren von Dateien.
              </p>
            </div>

            {/* Catalog Preview Box */}
            <div className="space-y-2 pt-2 border-t border-emerald-900/30">
              <div className="p-2.5 rounded-lg bg-black/50 border border-emerald-900/20 flex items-center justify-between text-xs">
                <div>
                  <span className="font-bold text-white block text-[11px]">Iris Shaders</span>
                  <span className="text-[10px] text-zinc-500">Nativer Shaderpack-Support</span>
                </div>
                <span className="font-pixel text-[8px] text-emerald-400 bg-emerald-500/15 px-2 py-0.5 rounded border border-emerald-500/30">
                  1-KLICK
                </span>
              </div>

              <div className="p-2.5 rounded-lg bg-black/50 border border-emerald-900/20 flex items-center justify-between text-xs">
                <div>
                  <span className="font-bold text-white block text-[11px]">Animierte Capes</span>
                  <span className="text-[10px] text-zinc-500">Live-Sync mit Community</span>
                </div>
                <span className="font-pixel text-[8px] text-emerald-400 bg-emerald-500/15 px-2 py-0.5 rounded border border-emerald-500/30">
                  SERVER-SYNC
                </span>
              </div>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
}
