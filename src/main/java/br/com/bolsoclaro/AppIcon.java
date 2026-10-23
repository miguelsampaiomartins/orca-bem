package br.com.bolsoclaro;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Carrega o ícone oficial do app e o redimensiona para as áreas da interface. */
public final class AppIcon {
    private static final String RESOURCE = "/orcabem-icon.png";
    private static BufferedImage cachedSource;

    private AppIcon() { }

    private static synchronized BufferedImage source() {
        if (cachedSource != null) return cachedSource;
        try (InputStream input = AppIcon.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Recurso de ícone não encontrado: " + RESOURCE);
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IllegalStateException("Recurso de ícone inválido: " + RESOURCE);
            cachedSource = image;
            return cachedSource;
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível carregar o ícone do OrçaBem.", ex);
        }
    }

    public static BufferedImage render(int size) {
        if (size < 1) throw new IllegalArgumentException("O tamanho do ícone precisa ser positivo.");
        BufferedImage original = source();
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(original, 0, 0, size, size, null);
        graphics.dispose();
        return resized;
    }

    public static List<Image> windowImages() {
        return List.of(render(16), render(20), render(24), render(32), render(48), render(64), render(128), render(256));
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0 ? Path.of(args[0]) : Path.of("src", "main", "resources", "orcabem-icon.png");
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        ImageIO.write(render(256), "png", output.toFile());
        System.out.println("Ícone criado em " + output.toAbsolutePath());
    }
}
