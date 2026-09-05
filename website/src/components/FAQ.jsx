import { useState } from 'react';
import { HelpCircle, ChevronDown, MessageSquare } from 'lucide-react';

const faqs = [
  {
    q: 'Ist EzClient auf Netzwerken wie GommeHD.net oder Hypixel erlaubt?',
    a: 'Ja, absolut! EzClient enthält ausschließlich erlaubte Performance-Mods (Sodium & Lithium), Connected Glass Texturen und kosmetische Client-Side Overlays (Keystrokes, ArmorStatus, CPS). Es enthält keinerlei unfaire Spielvorteile, Macros oder Hacks und ist 100% Anti-Cheat konform.',
  },
  {
    q: 'Wie installiere ich eigene Fabric-Mods in EzClient?',
    a: 'EzClient besitzt einen integrierten Modrinth-Marktplatz direkt in der PySide6/QML Oberfläche. Du kannst Mods mit nur einem Klick suchen und installieren, oder eigene .jar Dateien einfach in das mods-Verzeichnis deines Profils ziehen.',
  },
  {
    q: 'Wie funktioniert das animierte Cape-System?',
    a: 'Jeder EzClient-Spieler kann im Launcher unter "Cosmetics" ein animiertes Cape auswählen oder eine eigene Textur hochladen. Die Capes werden über unseren schnellen Community-Server in Echtzeit synchronisiert und sind für alle anderen EzClient-Spieler im Spiel sichtbar.',
  },
  {
    q: 'Wie sicher ist die Anmeldung mit meinem Minecraft-Account?',
    a: 'Die Authentifizierung erfolgt direkt und sicher über das offizielle Microsoft Xbox OAuth2 Protokoll. EzClient speichert niemals Passwörter oder sensible Zugangsdaten.',
  },
  {
    q: 'Muss ich Java separat installieren?',
    a: 'Nein! EzClient wird mit einer optimierten Java 25 Laufzeitumgebung ausgeliefert, sodass du direkt nach dem Download ohne manuelle Java-Einrichtung losspielen kannst.',
  },
];

export default function FAQ() {
  const [openIndex, setOpenIndex] = useState(0);

  return (
    <section id="faq" className="relative py-20 lg:py-28 bg-[#050805] border-b border-emerald-900/30 overflow-hidden">
      <div className="page-container">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-4">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30">
            <HelpCircle className="w-3.5 h-3.5 text-emerald-400" />
            <span className="font-pixel text-[10px] font-bold tracking-wider text-emerald-400">
              HÄUFIG GESTELLTE FRAGEN
            </span>
          </div>

          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-tight">
            Alles, was du wissen musst.
          </h2>

          <p className="text-base text-zinc-300 leading-relaxed">
            Antworten zu Anti-Cheat-Sicherheit, Installation und Funktionen von EzClient.
          </p>
        </div>

        {/* Accordion List */}
        <div className="max-w-3xl mx-auto space-y-3.5">
          {faqs.map((faq, idx) => {
            const isOpen = openIndex === idx;
            return (
              <div
                key={faq.q}
                className={`border-2 rounded-2xl transition-all duration-200 overflow-hidden ${
                  isOpen 
                    ? 'bg-[#080d09] border-emerald-500/40 shadow-[0_0_20px_rgba(34,197,94,0.1)]' 
                    : 'bg-[#080d09]/60 border-emerald-900/30 hover:border-emerald-500/20'
                }`}
              >
                <button
                  onClick={() => setOpenIndex(isOpen ? null : idx)}
                  className="w-full flex items-center justify-between p-5 text-left focus:outline-none"
                  aria-expanded={isOpen}
                >
                  <span className="text-base font-bold text-white pr-4">
                    {faq.q}
                  </span>
                  <div className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 transition-transform duration-300 ${
                    isOpen ? 'bg-emerald-500/20 text-emerald-400 rotate-180' : 'bg-white/[0.04] text-zinc-400'
                  }`}>
                    <ChevronDown className="w-4 h-4" />
                  </div>
                </button>

                <div 
                  className={`transition-all duration-300 ease-in-out px-5 overflow-hidden ${
                    isOpen ? 'max-h-60 pb-5 opacity-100' : 'max-h-0 pb-0 opacity-0'
                  }`}
                >
                  <p className="text-sm text-zinc-300 leading-relaxed pt-2 border-t border-emerald-900/30">
                    {faq.a}
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        {/* Discord Support Callout */}
        <div className="mt-12 text-center">
          <p className="text-sm text-zinc-400 flex items-center justify-center gap-2">
            <span>Noch Fragen?</span>
            <a
              href="https://discord.gg/ezclient"
              target="_blank"
              rel="noopener noreferrer"
              className="text-emerald-400 hover:text-emerald-300 font-bold underline flex items-center gap-1"
            >
              <MessageSquare className="w-3.5 h-3.5" />
              Frag die Community auf Discord
            </a>
          </p>
        </div>

      </div>
    </section>
  );
}
