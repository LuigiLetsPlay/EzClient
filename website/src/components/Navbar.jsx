import { useState } from 'react';
import { Download, Menu, X } from 'lucide-react';

const DiscordIcon = ({ className = "w-4 h-4" }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057.101 18.08.114 18.1.132 18.112a19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z" />
  </svg>
);

const navItems = [
  { name: 'Features', href: '#features' },
  { name: 'Mods', href: '#features' },
  { name: 'Download', href: '#download' },
  { name: 'FAQ', href: '#faq' },
];

export default function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 w-full border-b border-emerald-900/40 bg-[#050805]/90 backdrop-blur-md transition-all duration-300">
      <div className="page-container h-18 flex items-center justify-between">
        
        {/* Left: Real Logo & Name */}
        <a href="#" className="flex items-center gap-3 group">
          <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center shadow-[0_0_20px_rgba(34,197,94,0.3)] group-hover:scale-105 transition-transform overflow-hidden p-1">
            <img src="/assets/logo.png" alt="EzClient Logo" className="w-full h-full object-contain" />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xl font-bold tracking-tight text-white group-hover:text-emerald-400 transition-colors">
              EzClient
            </span>
            <span className="font-pixel text-[10px] bg-emerald-500/10 text-emerald-400 px-2 py-0.5 rounded border border-emerald-500/30 tracking-wider">
              26.2
            </span>
          </div>
        </a>

        {/* Center: Navigation */}
        <nav className="hidden md:flex items-center gap-8">
          {navItems.map((item) => (
            <a
              key={item.name}
              href={item.href}
              className="text-sm font-medium text-zinc-300 hover:text-emerald-400 transition-colors relative py-1 group"
            >
              {item.name}
              <span className="absolute bottom-0 left-0 w-0 h-[2px] bg-emerald-400 group-hover:w-full transition-all duration-200 rounded-full shadow-[0_0_8px_#22c55e]" />
            </a>
          ))}
        </nav>

        {/* Right: Discord + Download Button */}
        <div className="hidden md:flex items-center gap-4">
          <a
            href="https://discord.gg/ezclient"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-3.5 py-2 text-sm font-medium text-zinc-300 hover:text-white bg-white/[0.03] hover:bg-emerald-500/10 border border-emerald-900/30 rounded-xl transition-all duration-200"
          >
            <DiscordIcon className="w-4 h-4 text-indigo-400" />
            <span>Community</span>
            <span className="flex items-center gap-1 font-pixel text-[10px] text-emerald-400 bg-emerald-500/15 px-1.5 py-0.5 rounded border border-emerald-500/30">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-mc-live" />
              ONLINE
            </span>
          </a>

          <a
            href="#download"
            className="btn-emerald px-5 py-2.5 rounded-xl font-bold text-sm flex items-center gap-2"
          >
            <Download className="w-4 h-4 stroke-[2.5]" />
            <span>Download</span>
          </a>
        </div>

        {/* Mobile Hamburger Button */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="md:hidden p-2 text-zinc-400 hover:text-emerald-400 rounded-lg focus:outline-none"
          aria-label="Menü umschalten"
        >
          {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-emerald-900/40 bg-[#050805]/98 backdrop-blur-2xl px-6 py-6 space-y-4">
          <nav className="flex flex-col space-y-3">
            {navItems.map((item) => (
              <a
                key={item.name}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                className="text-base font-medium text-zinc-300 hover:text-emerald-400 py-2 border-b border-emerald-950/40 transition-colors"
              >
                {item.name}
              </a>
            ))}
          </nav>
          <div className="pt-2 flex flex-col gap-3">
            <a
              href="https://discord.gg/ezclient"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center justify-center gap-2 py-3 text-sm font-medium text-zinc-300 bg-white/[0.04] border border-emerald-900/30 rounded-xl"
            >
              <DiscordIcon className="w-4 h-4 text-indigo-400" />
              Discord Community beitreten
            </a>
            <a
              href="#download"
              onClick={() => setMobileMenuOpen(false)}
              className="btn-emerald flex items-center justify-center gap-2 py-3 text-sm rounded-xl"
            >
              <Download className="w-4 h-4 stroke-[2.5]" />
              EzClient herunterladen
            </a>
          </div>
        </div>
      )}
    </header>
  );
}
