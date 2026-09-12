package app.ezclient.gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Windows Media Session (GSMTC) overlay with per-provider filters,
 * active playback detection (playing vs paused/idle), and browser media support.
 */
public final class SpotifyOverlayModule extends FeatureModule {
    private record Track(String source, String title) {}

    private static final String SCRIPT =
            "$ProgressPreference='SilentlyContinue';" +
            "Add-Type -AssemblyName System.Runtime.WindowsRuntime;" +
            "$asTaskGeneric=([System.WindowsRuntimeSystemExtensions].GetMethods()|Where-Object{" +
                "$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'" +
            "})[0];" +
            "function Await($t,$r){" +
                "$m=$asTaskGeneric.MakeGenericMethod($r);" +
                "$nt=$m.Invoke($null,@($t));" +
                "$nt.Wait(1200)|Out-Null;" +
                "$nt.Result" +
            "};" +
            "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]|Out-Null;" +
            "$mgr=Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]);" +
            "if($mgr){" +
                "$sessions=$mgr.GetSessions();$playing=$null;" +
                "foreach($s in $sessions){" +
                    "$info=$s.GetPlaybackInfo();" +
                    "if($info -and $info.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing){" +
                        "$playing=$s;break" +
                    "}" +
                "};" +
                "if(-not $playing){" +
                    "$cur=$mgr.GetCurrentSession();" +
                    "if($cur){" +
                        "$info=$cur.GetPlaybackInfo();" +
                        "if($info -and $info.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing){" +
                            "$playing=$cur" +
                        "}" +
                    "}" +
                "};" +
                "if($playing){" +
                    "$props=Await ($playing.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]);" +
                    "if($props -and $props.Title){" +
                        "$app=$playing.SourceAppUserModelId;" +
                        "$artist=$props.Artist;" +
                        "$title=$props.Title;" +
                        "Write-Output ($app+'|'+$artist+'|'+$title);" +
                        "exit" +
                    "}" +
                "}" +
            "};" +
            "$p=Get-Process -Name 'spotify' -ErrorAction SilentlyContinue|Where-Object{$_.MainWindowTitle -and $_.MainWindowTitle -notmatch '^(Spotify( (Premium|Free))?)$'}|Select-Object -First 1;" +
            "if($p){Write-Output ('Spotify||'+$p.MainWindowTitle)}";

    private static final String ENCODED_SCRIPT = Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE));

    private final AtomicBoolean polling = new AtomicBoolean();
    private volatile Track current;
    private long nextPoll;

    public SpotifyOverlayModule() {
        super("Spotify Overlay", true, 296);
        flag("Sources", "spotify", "Spotify", "Erlaubt Spotify-Wiedergabe.", true);
        flag("Sources", "appleMusic", "Apple Music", "Erlaubt Apple Music.", true);
        flag("Sources", "youtube", "YouTube", "Erlaubt YouTube und YouTube Music.", true);
        flag("Sources", "amazonMusic", "Amazon Music", "Erlaubt Amazon Music.", true);
        flag("Sources", "soundcloud", "SoundCloud", "Erlaubt SoundCloud.", true);
        flag("Sources", "tidal", "Tidal", "Erlaubt Tidal.", true);
        flag("Sources", "cider", "Cider", "Erlaubt Cider.", true);
        flag("Sources", "deezer", "Deezer", "Erlaubt Deezer.", true);
        flag("Sources", "allowOther", "Allow other", "Erlaubt nicht erkannte Medienquellen.", true);
        flag("Darstellung", "showSource", "Show source", "Zeigt den Namen des Players.", true);
        flag("Darstellung", "hideWhenIdle", "Hide when idle", "Blendet das Overlay ohne aktive Wiedergabe aus.", false);
        option("Darstellung", "maxLength", "Maximum title length", "Begrenzt lange Songtitel.", 52.0, 16, 120);
        option("Performance", "refresh", "Refresh seconds", "Intervall für die Abfrage der Medienfenster.", 3.0, 1, 15);
        colorOption("Farben", "accent", "Accent", "Farbe des Player-Namens.", "FF1ED760");
        colorOption("Farben", "titleColor", "Title", "Farbe des Songtitels.", "FFFFFFFF");
    }

    @Override
    public void onTick() {
        if (!isEnabled() || System.currentTimeMillis() < nextPoll || !polling.compareAndSet(false, true)) return;
        nextPoll = System.currentTimeMillis() + (long)(number("refresh") * 1000);
        Thread.ofVirtual().name("EzClient-MediaProbe").start(() -> {
            try { current = probe(); } finally { polling.set(false); }
        });
    }

    private Track probe() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return null;
        try {
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", ENCODED_SCRIPT)
                    .redirectErrorStream(true)
                    .start();
            String resultLine = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null;) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#<")) {
                        resultLine = trimmed;
                        break;
                    }
                }
            }
            if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly();
            if (resultLine == null || resultLine.isBlank()) return null;

            String[] parts = resultLine.split("\\|", 3);
            String app = parts[0].trim();
            String artist = parts.length > 1 ? parts[1].trim() : "";
            String title = parts.length > 2 ? parts[2].trim() : "";
            if (title.isEmpty() && !artist.isEmpty()) {
                title = artist;
                artist = "";
            }
            if (title.isEmpty()) return null;

            Track track = classify(app, artist, title);
            if (track != null && allowed(track.source())) {
                return track;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isBrowser(String app) {
        if (app == null) return false;
        String lower = app.toLowerCase(Locale.ROOT);
        return lower.contains("chrome") || lower.contains("brave") || lower.contains("msedge")
                || lower.contains("edge") || lower.contains("firefox") || lower.contains("opera")
                || lower.contains("vivaldi") || lower.contains("arc") || lower.contains("browser")
                || lower.contains("waterfox") || lower.contains("librewolf");
    }

    private static String cleanAppName(String app) {
        if (app == null || app.isBlank()) return "Media";
        String clean = app.replaceAll("(?i)\\.exe$", "").replaceAll("!App$", "");
        int lastDot = clean.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < clean.length() - 1) clean = clean.substring(lastDot + 1);
        return clean.isBlank() ? "Media" : clean;
    }

    private Track classify(String app, String artist, String rawTitle) {
        String all = (app + " " + artist + " " + rawTitle).toLowerCase(Locale.ROOT);
        String source;
        if (all.contains("spotify")) source = "Spotify";
        else if (all.contains("apple music") || app.equalsIgnoreCase("AppleMusic")) source = "Apple Music";
        else if (all.contains("youtube")) source = "YouTube";
        else if (all.contains("amazon music")) source = "Amazon Music";
        else if (all.contains("soundcloud")) source = "SoundCloud";
        else if (all.contains("tidal")) source = "Tidal";
        else if (all.contains("cider")) source = "Cider";
        else if (all.contains("deezer")) source = "Deezer";
        else if (isBrowser(app)) {
            source = all.contains("twitch") ? "Twitch" : "YouTube";
        } else {
            source = cleanAppName(app);
        }

        String cleanedTitle = rawTitle.replaceAll("\\s[-–—]\\s(Spotify|YouTube Music|YouTube|SoundCloud|TIDAL|Deezer)$", "").trim();
        if (cleanedTitle.isBlank() || cleanedTitle.equalsIgnoreCase("Spotify") || cleanedTitle.equalsIgnoreCase("Spotify Premium")) {
            return null;
        }

        String displayTitle;
        if (artist != null && !artist.isBlank() && !artist.equalsIgnoreCase(cleanedTitle)) {
            displayTitle = artist.trim() + " — " + cleanedTitle;
        } else {
            displayTitle = cleanedTitle;
        }

        return new Track(source, displayTitle);
    }

    private boolean allowed(String source) {
        return switch (source) {
            case "Spotify" -> flag("spotify");
            case "Apple Music" -> flag("appleMusic");
            case "YouTube" -> flag("youtube") || flag("allowOther");
            case "Amazon Music" -> flag("amazonMusic");
            case "SoundCloud" -> flag("soundcloud");
            case "Tidal" -> flag("tidal");
            case "Cider" -> flag("cider");
            case "Deezer" -> flag("deezer");
            default -> flag("allowOther");
        };
    }

    @Override
    public List<String> lines(Minecraft mc, boolean editor) {
        Track track = editor ? new Track("YouTube", "Kanal — Beispiel Video") : current;
        if (track == null) {
            return flag("hideWhenIdle") && !editor ? List.of()
                    : List.of("Medien", polling.get() ? "Suche nach Medien …" : "Keine Wiedergabe");
        }
        String title = track.title();
        int limit = (int)number("maxLength");
        if (title.length() > limit) title = title.substring(0, Math.max(1, limit - 1)) + "…";
        return flag("showSource") ? List.of(track.source(), title) : List.of(title);
    }

    @Override
    public net.minecraft.network.chat.Component styledText(String value) {
        Track track = current;
        boolean source = (track != null && value.equals(track.source()))
                || value.equals("Medien") || value.equals("Spotify") || value.equals("YouTube");
        return super.styledText(value).copy().withStyle(style ->
                style.withColor((source ? tint("accent", false) : tint("titleColor", false)) & 0xffffff));
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/spotify.png");
    }
}
