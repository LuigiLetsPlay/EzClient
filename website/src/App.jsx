import { useState } from 'react'
import { ArrowDown, ArrowRight, Check, ChevronDown, Code2, Download, Gauge, Menu, MessageCircle, PackageSearch, ShieldCheck, Sparkles, X } from 'lucide-react'

const releaseUrl = 'https://github.com/LuigiLetsPlay/EzClient/releases/latest'

function Navbar() {
  const [open, setOpen] = useState(false)
  const links = [['Features', '#features'], ['Launcher', '#launcher'], ['Download', '#download'], ['FAQ', '#faq']]
  return <header className="nav-shell">
    <a className="brand" href="#top" aria-label="EzClient Startseite"><img src="/assets/logo.png" alt=""/><span>EZCLIENT</span><small>1.8.2</small></a>
    <nav className={open ? 'nav-links open' : 'nav-links'}>{links.map(([name, href]) => <a key={name} href={href} onClick={() => setOpen(false)}>{name}</a>)}<a className="nav-discord" href="https://discord.gg/ezclient"><MessageCircle size={16}/> Discord</a></nav>
    <a className="nav-cta" href="#download"><Download size={16}/> Download</a>
    <button className="menu-button" onClick={() => setOpen(!open)} aria-label="Menü öffnen">{open ? <X/> : <Menu/>}</button>
  </header>
}

function LauncherMockup() {
  return <div className="launcher-wrap launcher-screenshot" id="launcher">
    <img
      src="/assets/launcher-home.png"
      alt="EzClient Launcher mit Minecraft-Spieler, Profilinformationen und Spielen-Button"
    />
  </div>
}

const features = [
  { n:'01', icon:Gauge, title:'Mehr Frames. Weniger Lärm.', text:'Sodium, Lithium und sorgfältig abgestimmte Defaults holen mehr aus deinem System – ohne zehn Menüs zu durchsuchen.' },
  { n:'02', icon:PackageSearch, title:'Mods ohne Umwege.', text:'Finde und verwalte Modrinth-Mods direkt im Launcher. Ein Klick statt Datei-Chaos.' },
  { n:'03', icon:Sparkles, title:'Dein Look im Spiel.', text:'Animierte Capes, cleane HUD-Module und Connected Glass geben deinem Spiel eine eigene Handschrift.' },
  { n:'04', icon:ShieldCheck, title:'Fair by design.', text:'Nur erlaubte Performance- und Komfort-Features. Keine Cheats, keine unfairen Vorteile.' },
]
const faqs = [
  ['Ist EzClient kostenlos?', 'Ja. EzClient ist kostenlos und Open Source. Du kannst den Code und alle Releases auf GitHub einsehen.'],
  ['Muss ich Java installieren?', 'Nein. Die passende Java-Laufzeit ist im Windows-Launcher enthalten und wird automatisch eingerichtet.'],
  ['Kann ich eigene Mods verwenden?', 'Ja. Installiere Mods über den integrierten Modrinth-Katalog oder füge eigene Fabric-JARs zu deinem Profil hinzu.'],
  ['Ist EzClient auf Servern erlaubt?', 'EzClient setzt auf Performance, Komfort und Kosmetik. Prüfe trotzdem immer die individuellen Regeln des Servers, auf dem du spielst.'],
]

export default function App() {
  const [faq, setFaq] = useState(0)
  return <div id="top"><Navbar/><main>
    <section className="hero"><div className="hero-bg"/><div className="hero-content"><div className="hero-kicker"><span/> BUILT FOR FABRIC 26.2</div><h1>Spiel Minecraft.<br/><em>Nicht dein Setup.</em></h1><p>Ein schneller, fokussierter Minecraft-Client mit eigenem Launcher, starken Performance-Mods und genau der richtigen Menge Persönlichkeit.</p><div className="hero-actions"><a className="primary" href="#download"><Download/> Kostenlos herunterladen</a><a className="secondary" href="#features">Entdecken <ArrowDown/></a></div><div className="hero-proof"><span><Check/> Open Source</span><span><Check/> Java inklusive</span><span><Check/> 20+ Mods</span></div></div><div className="hero-version"><small>LATEST RELEASE</small><strong>v1.8.2</strong><span>Windows · Fabric</span></div></section>
    <section className="statement"><span>ONE CLIENT.</span><strong>EVERYTHING YOU NEED.</strong><span>NOTHING YOU DON'T.</span></section>
    <section className="features section" id="features"><div className="section-heading"><div><span className="eyebrow">WHY EZCLIENT</span><h2>Weniger konfigurieren.<br/>Mehr spielen.</h2></div><p>Performance zuerst, Komfort direkt dahinter. EzClient bündelt das Wesentliche in einem Erlebnis, das einfach funktioniert.</p></div><div className="feature-grid">{features.map(({n, icon:Icon, title, text}) => <article key={n}><div className="feature-meta"><span>{n}</span><Icon/></div><h3>{title}</h3><p>{text}</p><div className="feature-line"/></article>)}</div></section>
    <section className="launcher-section section"><div className="launcher-copy"><span className="eyebrow">THE LAUNCHER</span><h2>Dein Spiel.<br/>Ein sauberer Start.</h2><p>Profile, Mods und Cosmetics an einem Ort. Kein überladener Hub, sondern ein Launcher, der dich schnell ins Spiel bringt.</p><ul><li><Check/> Automatische Java-Konfiguration</li><li><Check/> Modrinth direkt integriert</li><li><Check/> Profile für jeden Spielstil</li></ul></div><LauncherMockup/></section>
    <section className="download section" id="download"><div className="download-glow"/><span className="eyebrow">READY WHEN YOU ARE</span><h2>Bereit für<br/><em>mehr FPS?</em></h2><p>Hol dir EzClient für Windows oder nutze die Standalone Fabric Mod in deinem bevorzugten Launcher.</p><div className="download-actions"><a className="primary light" href={releaseUrl}><Download/> Download für Windows <small>.EXE</small></a><a className="secondary" href={releaseUrl}>Standalone Mod .JAR <ArrowRight/></a></div><div className="requirements"><span>v1.8.2</span><span>Windows 10 / 11</span><span>Fabric 26.2</span><span>Java 25 inklusive</span></div></section>
    <section className="faq section" id="faq"><div><span className="eyebrow">FAQ</span><h2>Kurz erklärt.</h2><p>Noch etwas unklar? Frag uns einfach auf Discord.</p><a href="https://discord.gg/ezclient">Zur Community <ArrowRight/></a></div><div className="faq-list">{faqs.map(([q,a],i) => <article className={faq===i?'open':''} key={q}><button onClick={()=>setFaq(faq===i?null:i)}><span>{q}</span><ChevronDown/></button><p>{a}</p></article>)}</div></section>
  </main><footer><div className="footer-brand"><img src="/assets/logo.png" alt=""/><strong>EZCLIENT</strong><span>Play more.</span></div><div className="footer-links"><a href={releaseUrl}><Code2/> GitHub</a><a href="https://discord.gg/ezclient"><MessageCircle/> Discord</a></div><p>Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.</p><small>© {new Date().getFullYear()} EzClient</small></footer></div>
}
