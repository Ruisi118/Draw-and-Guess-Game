package client.gui;

import client.model.DrawAction;
import client.model.Tool;
import util.GameColors;
import util.GameDimensions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The drawing canvas — freehand brush strokes rendered via Graphics2D.
 * Uses BufferedImage caching for performance.
 */
public class DrawingPanel extends JPanel {
    private BufferedImage canvasImage;
    private Graphics2D canvasG2;

    // Current stroke state
    private Tool currentTool = Tool.BRUSH;
    private Color currentColor = GameColors.CANVAS_DEFAULT_PEN;
    // Per-tool sizes (BRUSH defaults to 3, ERASER defaults to 12 — eraser is usually bigger)
    private int brushStrokeWidth = 3;
    private int eraserStrokeWidth = 12;
    private List<Point> currentStroke = new ArrayList<>();
    private Point lastPoint;
    private boolean drawingEnabled = false;

    // Batching: buffer points, flush every 50ms
    private final List<Point> pendingPoints = new ArrayList<>();
    private Timer batchTimer;

    // Callback to send draw actions to server
    private DrawActionListener drawListener;

    public interface DrawActionListener {
        void onDrawAction(DrawAction action);
        void onClearCanvas();
    }

    public DrawingPanel() {
        setBackground(GameColors.CANVAS_BG);
        initCanvas();

        batchTimer = new Timer(50, e -> flushPendingPoints());
        batchTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!drawingEnabled) return;
                lastPoint = screenToLogical(e.getPoint());
                currentStroke.clear();
                currentStroke.add(lastPoint);
                pendingPoints.add(lastPoint);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!drawingEnabled) return;
                flushPendingPoints();
                // Commit full stroke to canvas image
                if (!currentStroke.isEmpty()) {
                    DrawAction action = new DrawAction(currentTool, getDrawColor(), getCurrentStrokeWidth(), new ArrayList<>(currentStroke));
                    commitStroke(action);
                    if (drawListener != null) drawListener.onDrawAction(action);
                }
                currentStroke.clear();
                lastPoint = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!drawingEnabled || lastPoint == null) return;
                Point p = screenToLogical(e.getPoint());
                currentStroke.add(p);
                pendingPoints.add(p);

                // Immediate local render (optimistic)
                Graphics2D g = (Graphics2D) getGraphics();
                if (g != null) {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(getDrawColor());
                    g.setStroke(new BasicStroke(getCurrentStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Point s1 = logicalToScreen(lastPoint);
                    Point s2 = logicalToScreen(p);
                    g.drawLine(s1.x, s1.y, s2.x, s2.y);
                    g.dispose();
                }
                lastPoint = p;
            }
        });
    }

    private void initCanvas() {
        canvasImage = new BufferedImage(
            GameDimensions.CANVAS_WIDTH, GameDimensions.CANVAS_HEIGHT,
            BufferedImage.TYPE_INT_ARGB
        );
        canvasG2 = canvasImage.createGraphics();
        canvasG2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        clearCanvas();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvasImage, 0, 0, getWidth(), getHeight(), null);
    }

    /** Commit a completed stroke onto the cached BufferedImage. */
    public void commitStroke(DrawAction action) {
        Color c = action.tool == Tool.ERASER ? GameColors.CANVAS_ERASER : action.color;
        canvasG2.setColor(c);
        canvasG2.setStroke(new BasicStroke(action.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 1; i < action.points.size(); i++) {
            Point p1 = action.points.get(i - 1);
            Point p2 = action.points.get(i);
            canvasG2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        repaint();
    }

    /** Receive a remote stroke from another client. */
    public void addRemoteStroke(DrawAction action) {
        commitStroke(action);
    }

    public void clearCanvas() {
        canvasG2.setColor(GameColors.CANVAS_BG);
        canvasG2.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
        repaint();
    }

    public void clearAndNotify() {
        clearCanvas();
        if (drawListener != null) drawListener.onClearCanvas();
    }

    private void flushPendingPoints() {
        // Batching handled on mouse release — pendingPoints used for future network optimization
        pendingPoints.clear();
    }

    private Color getDrawColor() {
        return currentTool == Tool.ERASER ? GameColors.CANVAS_ERASER : currentColor;
    }

    // ═══ Coordinate Transforms ═══

    private Point screenToLogical(Point screen) {
        double scaleX = (double) GameDimensions.CANVAS_WIDTH / getWidth();
        double scaleY = (double) GameDimensions.CANVAS_HEIGHT / getHeight();
        return new Point((int)(screen.x * scaleX), (int)(screen.y * scaleY));
    }

    private Point logicalToScreen(Point logical) {
        double scaleX = (double) getWidth() / GameDimensions.CANVAS_WIDTH;
        double scaleY = (double) getHeight() / GameDimensions.CANVAS_HEIGHT;
        return new Point((int)(logical.x * scaleX), (int)(logical.y * scaleY));
    }

    // ═══ Custom Cursor ═══

    private void updateCursor() {
        if (!drawingEnabled) {
            setCursor(Cursor.getDefaultCursor());
            return;
        }

        int size = Math.max(getCurrentStrokeWidth(), 6);
        int imgSize = size + 4;
        BufferedImage cursorImg = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cursorImg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int offset = 2;
        if (currentTool == Tool.BRUSH) {
            g.setColor(new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), 100));
            g.fillOval(offset, offset, size, size);
            g.setColor(currentColor.darker());
            g.setStroke(new BasicStroke(1));
            g.drawOval(offset, offset, size, size);
        } else if (currentTool == Tool.ERASER) {
            g.setColor(new Color(150, 150, 150, 80));
            g.fillOval(offset, offset, size, size);
            g.setColor(Color.GRAY);
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2, 2}, 0));
            g.drawOval(offset, offset, size, size);
        }
        g.dispose();

        Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(
            cursorImg, new java.awt.Point(imgSize / 2, imgSize / 2), "brush-cursor");
        setCursor(cursor);
    }

    // ═══ Setters ═══

    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
        updateCursor();
    }
    public void setCurrentColor(Color color) {
        this.currentColor = color;
        updateCursor();
    }
    /** Sets stroke width for the CURRENT tool only. Each tool keeps its own size. */
    public void setCurrentStrokeWidth(int width) {
        if (currentTool == Tool.ERASER) eraserStrokeWidth = width;
        else                              brushStrokeWidth = width;
        updateCursor();
    }
    /** Returns the active stroke width for the current tool. */
    public int getCurrentStrokeWidth() {
        return currentTool == Tool.ERASER ? eraserStrokeWidth : brushStrokeWidth;
    }
    public void setDrawingEnabled(boolean on) {
        this.drawingEnabled = on;
        updateCursor();
    }
    public void setDrawListener(DrawActionListener l) { this.drawListener = l; }
}
