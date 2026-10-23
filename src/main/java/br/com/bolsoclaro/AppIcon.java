package br.com.bolsoclaro;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Vetor reutilizável para o ícone do Bolso Claro, renderizado sem dependências externas. */
public final class AppIcon {
    private static final Color FOREST = new Color(16, 43, 40);
    private static final Color FOREST_LIGHT = new Color(27, 75, 63);
    private static final Color IVORY = new Color(248, 242, 225);
    private static final Color GREEN = new Color(54, 132, 97);
    private static final Color GOLD = new Color(244, 185, 99);

    private AppIcon() { }

    public static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 256.0, size / 256.0);

        g.setPaint(new GradientPaint(18, 15, FOREST_LIGHT, 238, 238, FOREST));
        g.fillRoundRect(8, 8, 240, 240, 58, 58);
        g.setColor(new Color(244, 185, 99, 190)); g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(10, 10, 236, 236, 56, 56);

        // Carteira clara, faixa frontal escura e moeda âmbar legíveis em 16 px.
        g.setColor(new Color(0, 0, 0, 48)); g.fillRoundRect(42, 70, 174, 140, 30, 30);
        g.setColor(IVORY); g.fillRoundRect(39, 62, 174, 140, 30, 30);
        g.setColor(new Color(229, 221, 198)); g.fillRoundRect(53, 75, 82, 10, 5, 5);
        g.setColor(GREEN); g.fillRoundRect(39, 100, 174, 38, 5, 5);
        g.setColor(FOREST_LIGHT); g.fillRoundRect(119, 125, 94, 66, 20, 20);
        g.setColor(GOLD);
        g.fillOval(143, 139, 42, 42);
        g.setColor(new Color(255, 221, 155)); g.setStroke(new BasicStroke(3f)); g.drawOval(146, 142, 36, 36);
        g.setColor(FOREST); g.fillOval(159, 155, 10, 10);

        // Traço ascendente sinalizando progresso financeiro.
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D path = new Path2D.Double();
        path.moveTo(61, 174); path.lineTo(80, 156); path.lineTo(96, 165); path.lineTo(116, 146);
        g.draw(path);
        g.drawLine(106, 146, 116, 146);
        g.drawLine(116, 146, 116, 156);

        g.dispose();
        return image;
    }

    public static List<Image> windowImages() {
        return List.of(render(16), render(24), render(32), render(48), render(64), render(128), render(256));
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0 ? Path.of(args[0]) : Path.of("src", "main", "resources", "bolso-claro-icon.png");
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) java.nio.file.Files.createDirectories(parent);
        ImageIO.write(render(256), "png", output.toFile());
        System.out.println("Ícone criado em " + output.toAbsolutePath());
    }
}
