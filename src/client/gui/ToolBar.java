package client.gui;

import client.model.Tool;
import util.*;

import javax.swing.*;
import java.awt.*;

/**
 * Drawing toolbar — tool buttons, color palette, stroke slider, round/timer info.
 */
public class ToolBar extends JPanel {
    private JButton penBtn, eraserBtn, clearBtn;
    private JLabel roundLabel, timerLabel, wordLabel;
    private JLabel sizeLabelRef;
    private int brushSize = 3;
    private int eraserSize = 12;
    private Tool currentToolForSize = Tool.BRUSH;
    private final JPanel colorPanel;
    private Color selectedColor = GameColors.CANVAS_DEFAULT_PEN;

    private ToolBarListener listener;

    public interface ToolBarListener {
        void onToolChanged(Tool tool);
        void onColorChanged(Color color);
        void onStrokeChanged(int width);
        void onClear();
    }

    public ToolBar() {
        setLayout(new BorderLayout());
        setBackground(GameColors.PAPER);
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GameColors.BORDER));
        setPreferredSize(new Dimension(0, 52)); // slightly taller for breathing room

        // Left: tools — vgap=0, use border padding for centering
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, GameDimensions.SPACE_XS, 0));
        toolsPanel.setOpaque(false);
        toolsPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        penBtn = GameStyles.createToolButton("Brush", true);
        eraserBtn = GameStyles.createToolButton("Eraser", false);
        clearBtn = GameStyles.createToolButton("Clear", false);

        penBtn.addActionListener(e -> selectTool(Tool.BRUSH));
        eraserBtn.addActionListener(e -> selectTool(Tool.ERASER));
        clearBtn.addActionListener(e -> { if (listener != null) listener.onClear(); });

        toolsPanel.add(penBtn);
        toolsPanel.add(eraserBtn);
        toolsPanel.add(clearBtn);

        // Color dots
        colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        colorPanel.setOpaque(false);
        for (Color c : GameColors.PEN_PRESETS) {
            JButton dot = new JButton();
            dot.setPreferredSize(new Dimension(20, 20));
            dot.setBackground(c);
            dot.setOpaque(true);
            dot.setBorderPainted(true);
            dot.setBorder(BorderFactory.createLineBorder(
                c.equals(selectedColor) ? GameColors.TEXT_PRIMARY : GameColors.BORDER, 1));
            dot.addActionListener(e -> {
                selectedColor = c;
                if (listener != null) listener.onColorChanged(c);
                refreshColorDots();
            });
            colorPanel.add(dot);
        }
        toolsPanel.add(Box.createHorizontalStrut(8));
        toolsPanel.add(colorPanel);

        // Stroke size — minus/plus buttons + label
        toolsPanel.add(Box.createHorizontalStrut(12));

        JButton sizeDown = new JButton("\u2212"); // −
        sizeDown.setFont(GameFonts.SUBTITLE);
        sizeDown.setPreferredSize(new Dimension(28, 28));
        sizeDown.setMargin(new java.awt.Insets(0,0,0,0));
        sizeDown.setFocusPainted(false);
        sizeDown.setBackground(GameColors.BG_SECONDARY);
        sizeDown.setBorder(BorderFactory.createLineBorder(GameColors.BORDER, 1));

        JLabel sizeLabel = new JLabel("3px");
        sizeLabel.setFont(GameFonts.MONO_LABEL);
        sizeLabel.setForeground(GameColors.TEXT_PRIMARY);
        sizeLabel.setPreferredSize(new Dimension(32, 28));
        sizeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        JButton sizeUp = new JButton("+");
        sizeUp.setFont(GameFonts.SUBTITLE);
        sizeUp.setPreferredSize(new Dimension(28, 28));
        sizeUp.setMargin(new java.awt.Insets(0,0,0,0));
        sizeUp.setFocusPainted(false);
        sizeUp.setBackground(GameColors.BG_SECONDARY);
        sizeUp.setBorder(BorderFactory.createLineBorder(GameColors.BORDER, 1));

        // Per-tool sizes: index 0 = BRUSH, 1 = ERASER
        // Store reference to the size label so we can update it when tool switches
        this.sizeLabelRef = sizeLabel;
        sizeDown.addActionListener(e -> {
            int v = (currentToolForSize == Tool.ERASER) ? eraserSize : brushSize;
            if (v > 1) {
                v--;
                if (currentToolForSize == Tool.ERASER) eraserSize = v; else brushSize = v;
                sizeLabel.setText(v + "px");
                if (listener != null) listener.onStrokeChanged(v);
            }
        });
        sizeUp.addActionListener(e -> {
            int v = (currentToolForSize == Tool.ERASER) ? eraserSize : brushSize;
            if (v < 20) {
                v++;
                if (currentToolForSize == Tool.ERASER) eraserSize = v; else brushSize = v;
                sizeLabel.setText(v + "px");
                if (listener != null) listener.onStrokeChanged(v);
            }
        });

        toolsPanel.add(sizeDown);
        toolsPanel.add(sizeLabel);
        toolsPanel.add(sizeUp);

        add(toolsPanel, BorderLayout.CENTER);

        // Right: game info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, GameDimensions.SPACE_SM, 0));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 8));

        wordLabel = new JLabel("");
        wordLabel.setFont(GameFonts.SUBTITLE);
        wordLabel.setPreferredSize(new Dimension(180, 20));

        roundLabel = new JLabel("R -/-");
        roundLabel.setFont(GameFonts.CAPTION);
        roundLabel.setForeground(GameColors.TEXT_SECONDARY);

        timerLabel = new JLabel("80");
        timerLabel.setFont(GameFonts.TITLE);

        infoPanel.add(wordLabel);
        infoPanel.add(Box.createHorizontalStrut(12));
        infoPanel.add(roundLabel);
        infoPanel.add(timerLabel);
        add(infoPanel, BorderLayout.EAST);
    }

    private void selectTool(Tool tool) {
        GameStyles.setToolButtonSelected(penBtn, tool == Tool.BRUSH);
        GameStyles.setToolButtonSelected(eraserBtn, tool == Tool.ERASER);

        // Update displayed size to match the now-active tool's stored size
        currentToolForSize = tool;
        int newSize = (tool == Tool.ERASER) ? eraserSize : brushSize;
        if (sizeLabelRef != null) sizeLabelRef.setText(newSize + "px");
        if (listener != null) {
            listener.onToolChanged(tool);
            listener.onStrokeChanged(newSize); // sync DrawingPanel to this tool's size
        }
    }

    private void refreshColorDots() {
        for (Component c : colorPanel.getComponents()) {
            if (c instanceof JButton) {
                JButton dot = (JButton) c;
                dot.setBorder(BorderFactory.createLineBorder(
                    dot.getBackground().equals(selectedColor) ? GameColors.TEXT_PRIMARY : GameColors.BORDER, 1));
            }
        }
    }

    // ═══ Public Updates ═══

    public void setTimerText(String text) { timerLabel.setText(text); }
    public void setTimerColor(Color c)    { timerLabel.setForeground(c); }
    public void setRoundText(String text) { roundLabel.setText(text); }
    public void setWordText(String text)  { wordLabel.setText(text); }
    public void setListener(ToolBarListener l) { this.listener = l; }

    public void setEnabled(boolean enabled) {
        penBtn.setEnabled(enabled);
        eraserBtn.setEnabled(enabled);
        clearBtn.setEnabled(enabled);
        for (Component c : colorPanel.getComponents()) c.setEnabled(enabled);
    }
}
