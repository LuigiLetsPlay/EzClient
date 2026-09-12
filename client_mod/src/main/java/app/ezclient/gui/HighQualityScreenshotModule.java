package app.ezclient.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** F2 capture pipeline with configurable output resolution and chat actions. */
public final class HighQualityScreenshotModule extends FeatureModule {
    private static volatile Path lastFile;
    private static volatile Path lastGalleryFile;
    public HighQualityScreenshotModule() {
        super("High Quality Screenshot", false, 0);
        option("Auflösung", "resolution", "Output resolution", "Zielauflösung für F2-Aufnahmen.", "Native", 0, 0,
                "Native", "2x", "4K", "8K");
        option("Auflösung", "filter", "Upscale filter", "Filter für die hochauflösende Ausgabe.", "Bicubic", 0, 0,
                "Nearest", "Bilinear", "Bicubic");
        flag("Datei", "copyEzFolder", "Copy to EzClient gallery", "Speichert zusätzlich in der EzClient-Galerie.", true);
        flag("Chat", "open", "Open action", "Zeigt eine Öffnen-Aktion im Chat.", true);
        flag("Chat", "copy", "Copy action", "Zeigt eine Kopieren-Aktion im Chat.", true);
        flag("Chat", "delete", "Delete action", "Zeigt eine Löschen-Aktion im Chat.", true);
    }

    public void capture(File gameDirectory, RenderTarget target, Consumer<Component> callback) {
        Screenshot.takeScreenshot(target, nativeImage -> {
            int sourceWidth = nativeImage.getWidth(), sourceHeight = nativeImage.getHeight();
            int[] pixels;
            try (nativeImage) { pixels = nativeImage.getPixels(); }
            Thread.ofVirtual().name("EzClient-ScreenshotEncoder").start(() -> {
              try {
                int[] size = targetSize(sourceWidth, sourceHeight);
                BufferedImage src = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB);
                src.setRGB(0, 0, sourceWidth, sourceHeight, pixels, 0, sourceWidth);
                BufferedImage output = src;
                if (size[0] != src.getWidth() || size[1] != src.getHeight()) {
                    output = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
                    var graphics = output.createGraphics();
                    Object hint = switch (text("filter")) {
                        case "Nearest" -> java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
                        case "Bilinear" -> java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                        default -> java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                    };
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, hint);
                    graphics.drawImage(src, 0, 0, size[0], size[1], null); graphics.dispose();
                }
                Path dir = gameDirectory.toPath().resolve("screenshots"); Files.createDirectories(dir);
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"));
                Path file = unique(dir, stamp + "_ezclient.png");
                ImageIO.write(output, "PNG", file.toFile()); lastFile = file;
                if (flag("copyEzFolder")) {
                    Path gallery = app.ezclient.EzClientMod.getEzClientDataDir().resolve("screenshots"); Files.createDirectories(gallery);
                    lastGalleryFile = gallery.resolve(file.getFileName());
                    Files.copy(file, lastGalleryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                net.minecraft.client.Minecraft.getInstance().execute(() -> callback.accept(successMessage(file, size[0], size[1])));
              } catch (Exception ex) {
                net.minecraft.client.Minecraft.getInstance().execute(() -> callback.accept(Component.literal("Screenshot failed: " + ex.getMessage()).withStyle(ChatFormatting.RED)));
              }
            });
        });
    }

    private int[] targetSize(int width, int height) {
        return switch (text("resolution")) {
            case "2x" -> new int[]{Math.min(8192, width * 2), Math.min(8192, height * 2)};
            case "4K" -> fit(width, height, 3840, 2160);
            case "8K" -> fit(width, height, 7680, 4320);
            default -> new int[]{width, height};
        };
    }
    private static int[] fit(int width, int height, int boxW, int boxH) {
        double ratio = Math.min(boxW / (double)width, boxH / (double)height);
        return new int[]{Math.max(1, (int)Math.round(width * ratio)), Math.max(1, (int)Math.round(height * ratio))};
    }
    private static Path unique(Path dir, String name) {
        Path path = dir.resolve(name); int index = 2;
        while (Files.exists(path)) path = dir.resolve(name.replace(".png", "_" + index++ + ".png"));
        return path;
    }
    private Component successMessage(Path file, int width, int height) {
        var message = Component.literal("Saved " + file.getFileName() + " (" + width + "×" + height + ")").withStyle(ChatFormatting.GREEN);
        if (flag("open")) message.append(Component.literal(" [Open]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(file))));
        if (flag("copy")) message.append(Component.literal(" [Copy]").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/ezshot copy"))));
        if (flag("delete")) message.append(Component.literal(" [Delete]").withStyle(s -> s.withColor(ChatFormatting.RED).withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/ezshot delete"))));
        return message;
    }
    public static int copyLast() {
        Path file = lastFile; if (file == null || !Files.isRegularFile(file)) return 0;
        try {
            Image image = ImageIO.read(file.toFile());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
                public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
                public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
                public Object getTransferData(DataFlavor flavor) { return image; }
            }, null);
            return 1;
        } catch (Exception ignored) { return 0; }
    }
    public static int deleteLast() {
        Path file = lastFile; if (file == null) return 0;
        try {
            boolean deleted = Files.deleteIfExists(file);
            Path gallery = lastGalleryFile; if (gallery != null) Files.deleteIfExists(gallery);
            if (deleted) { lastFile = null; lastGalleryFile = null; }
            return deleted ? 1 : 0;
        }
        catch (Exception ignored) { return 0; }
    }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/screenshot.png"); }
}
