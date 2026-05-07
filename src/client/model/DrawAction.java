package client.model;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * A single drawing stroke — serialized as a pipe-delimited string:
 * DRAW|BRUSH|16711680|3|120,45;121,46;123,48
 */
public class DrawAction {
    public final Tool tool;
    public final Color color;
    public final int strokeWidth;
    public final List<Point> points;

    public DrawAction(Tool tool, Color color, int strokeWidth, List<Point> points) {
        this.tool = tool;
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.points = new ArrayList<>(points);
    }

    /**
     * Serialize to protocol format (without the DRAW| prefix).
     * e.g. "BRUSH|16711680|3|120,45;121,46;123,48"
     */
    public String serialize() {
        StringBuilder pts = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) pts.append(";");
            pts.append(points.get(i).x).append(",").append(points.get(i).y);
        }
        return String.format("DRAW|%s|%d|%d|%s",
            tool.name(),
            color.getRGB(),
            strokeWidth,
            pts.toString()
        );
    }

    /**
     * Deserialize from raw protocol message.
     * @param raw e.g. "DRAW|BRUSH|16711680|3|120,45;121,46"
     */
    public static DrawAction deserialize(String raw) {
        String[] parts = raw.split("\\|");
        // parts[0] = "DRAW"
        Tool tool = Tool.valueOf(parts[1]);
        Color color = new Color(Integer.parseInt(parts[2]));
        int strokeWidth = Integer.parseInt(parts[3]);

        List<Point> points = new ArrayList<>();
        if (parts.length > 4 && !parts[4].isEmpty()) {
            for (String pt : parts[4].split(";")) {
                String[] xy = pt.split(",");
                points.add(new Point(
                    Integer.parseInt(xy[0]),
                    Integer.parseInt(xy[1])
                ));
            }
        }
        return new DrawAction(tool, color, strokeWidth, points);
    }
}
