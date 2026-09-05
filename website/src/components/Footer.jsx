const DiscordIcon = ({ className = "w-4 h-4" }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057.101 18.08.114 18.1.132 18.112a19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z" />
  </svg>
);

const GitHubIcon = ({ className = "w-4 h-4" }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
  </svg>
);

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-[#030503] border-t border-emerald-900/40 pt-16 pb-12 text-xs text-zinc-400">
      <div className="page-container space-y-12">
        
        {/* Top Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          
          {/* Brand Info */}
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center p-1">
                <img src="/assets/logo.png" alt="EzClient Logo" className="w-full h-full object-contain" />
              </div>
              <span className="text-lg font-bold text-white tracking-tight">EzClient</span>
              <span className="font-pixel text-[9px] bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 px-1.5 py-0.5 rounded">
                v1.8.2
              </span>
            </div>

            <p className="text-xs text-zinc-400 leading-relaxed">
              Der moderne Next-Gen Minecraft Fabric Client &amp; PySide6 Launcher mit echter Minecraft-DNA.
            </p>

            {/* System Status */}
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-pixel text-[9px]">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-mc-live" />
              <span>SERVER &amp; CAPES ONLINE</span>
            </div>
          </div>

          {/* Navigation */}
          <div className="space-y-3">
            <h4 className="font-pixel text-[10px] text-white font-bold tracking-wider">
              NAVIGATION
            </h4>
            <ul className="space-y-2">
              <li><a href="#features" className="hover:text-emerald-400 transition-colors">Features &amp; Sodium</a></li>
              <li><a href="#module" className="hover:text-emerald-400 transition-colors">Clear Glass &amp; HUD</a></li>
              <li><a href="#cosmetics" className="hover:text-emerald-400 transition-colors">Animierte Capes</a></li>
              <li><a href="#download" className="hover:text-emerald-400 transition-colors">Download Center</a></li>
              <li><a href="#faq" className="hover:text-emerald-400 transition-colors">FAQ</a></li>
            </ul>
          </div>

          {/* Community Links */}
          <div className="space-y-3">
            <h4 className="font-pixel text-[10px] text-white font-bold tracking-wider">
              COMMUNITY
            </h4>
            <ul className="space-y-2">
              <li>
                <a href="https://discord.gg/ezclient" target="_blank" rel="noopener noreferrer" className="hover:text-emerald-400 transition-colors flex items-center gap-1.5">
                  <DiscordIcon className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Discord Server</span>
                </a>
              </li>
              <li>
                <a href="https://github.com/LuigiLetsPlay/EzClient" target="_blank" rel="noopener noreferrer" className="hover:text-emerald-400 transition-colors flex items-center gap-1.5">
                  <GitHubIcon className="w-3.5 h-3.5 text-zinc-300" />
                  <span>GitHub Repository</span>
                </a>
              </li>
              <li>
                <a href="https://github.com/LuigiLetsPlay/EzClient/releases" target="_blank" rel="noopener noreferrer" className="hover:text-emerald-400 transition-colors">
                  Releases &amp; Changelogs
                </a>
              </li>
            </ul>
          </div>

          {/* Socials & Tech */}
          <div className="space-y-3">
            <h4 className="font-pixel text-[10px] text-white font-bold tracking-wider">
              OPEN SOURCE
            </h4>
            <p className="text-xs text-zinc-400 leading-relaxed">
              EzClient wird von der Community für die Minecraft-Community entwickelt.
            </p>
            <div className="flex items-center gap-2 pt-1">
              <a
                href="https://discord.gg/ezclient"
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-lg bg-white/[0.04] hover:bg-emerald-500/20 border border-emerald-900/40 flex items-center justify-center text-zinc-300 hover:text-emerald-400 transition-colors"
                aria-label="Discord"
              >
                <DiscordIcon className="w-4 h-4" />
              </a>
              <a
                href="https://github.com/LuigiLetsPlay/EzClient"
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-lg bg-white/[0.04] hover:bg-emerald-500/20 border border-emerald-900/40 flex items-center justify-center text-zinc-300 hover:text-emerald-400 transition-colors"
                aria-label="GitHub"
              >
                <GitHubIcon className="w-4 h-4" />
              </a>
            </div>
          </div>

        </div>

        {/* Mojang Disclaimer */}
        <div className="pt-8 border-t border-emerald-900/30 space-y-4">
          <p className="text-[11px] text-zinc-400 leading-relaxed max-w-4xl">
            <strong>Disclaimer:</strong> Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft. Minecraft ist ein eingetragenes Warenzeichen von Mojang Synergies AB.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px] text-zinc-400 pt-2 font-pixel">
            <span>© {currentYear} EzClient. All rights reserved.</span>
            <span className="text-emerald-400">FABRIC 26.2 • MINECRAFT CLIENT</span>
          </div>
        </div>

      </div>
    </footer>
  );
}
