package util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/**
 * Lucide-style stroke icons drawn with Graphics2D.
 * Consistent 1.5px stroke, round caps, 24x24 viewbox.
 */
public final class GameIcons {
    private GameIcons() {}

    private static final int SIZE = 20;
    private static final float STROKE = 1.8f;

    /** Pencil icon (brush tool) */
    public static ImageIcon pencil(Color color) {
        return makeIcon(color, g -> {
            // Pencil body (diagonal line with tip)
            g.draw(new Line2D.Float(4, 16, 16, 4));      // main diagonal
            g.draw(new Line2D.Float(12, 4, 16, 4));      // top serif
            g.draw(new Line2D.Float(16, 4, 16, 8));      // right serif
            g.draw(new Line2D.Float(4, 16, 7, 16));      // bottom
            g.draw(new Line2D.Float(4, 16, 4, 13));      // left
            // Pencil tip
            g.fill(new Ellipse2D.Float(3, 15, 3, 3));
        });
    }

    /** Eraser icon */
    public static ImageIcon eraser(Color color) {
        return makeIcon(color, g -> {
            // Eraser body (rotated rectangle)
            GeneralPath p = new GeneralPath();
            p.moveTo(6, 16);
            p.lineTo(3, 12);
            p.lineTo(11, 4);
            p.lineTo(17, 4);
            p.lineTo(17, 10);
            p.lineTo(9, 18);
            p.closePath();
            g.draw(p);
            // Division line
            g.draw(new Line2D.Float(7, 14, 14, 7));
            // Bottom line
            g.draw(new Line2D.Float(6, 18, 17, 18));
        });
    }

    /** Trash icon (clear) */
    public static ImageIcon trash(Color color) {
        return makeIcon(color, g -> {
            // Lid
            g.draw(new Line2D.Float(3, 5, 17, 5));
            // Handle
            g.draw(new Line2D.Float(8, 5, 8, 3));
            g.draw(new Line2D.Float(8, 3, 12, 3));
            g.draw(new Line2D.Float(12, 3, 12, 5));
            // Body
            g.draw(new Line2D.Float(5, 5, 6, 17));
            g.draw(new Line2D.Float(15, 5, 14, 17));
            g.draw(new Line2D.Float(6, 17, 14, 17));
            // Lines inside
            g.draw(new Line2D.Float(8, 8, 8, 14));
            g.draw(new Line2D.Float(12, 8, 12, 14));
        });
    }

    private static ImageIcon makeIcon(Color color, java.util.function.Consumer<Graphics2D> painter) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(color);
        g.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        painter.accept(g);
        g.dispose();
        return new ImageIcon(img);
    }
}
