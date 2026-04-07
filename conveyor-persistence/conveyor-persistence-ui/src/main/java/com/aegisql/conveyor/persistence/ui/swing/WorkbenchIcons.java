package com.aegisql.conveyor.persistence.ui.swing;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

final class WorkbenchIcons {

    private static final float BASE_SIZE = 18f;
    private static final int SIZE = 24;
    private static final Color DISABLED_COLOR = new Color(156, 163, 175);

    private WorkbenchIcons() {
    }

    static Icon add() {
        return new PaintedIcon(SIZE, new Color(47, 133, 90)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(9f, 3.5f, 9f, 14.5f));
                g2.draw(new Line2D.Float(3.5f, 9f, 14.5f, 9f));
            }
        };
    }

    static Icon edit() {
        return new GlyphIcon(SIZE, new Color(44, 82, 130), "\u270E", 20f);
    }

    static Icon duplicate() {
        return new PaintedIcon(SIZE, new Color(74, 85, 104)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(4.2f, 5.2f, 7.0f, 7.8f, 1.6f, 1.6f));
                g2.draw(new RoundRectangle2D.Float(6.8f, 2.8f, 7.0f, 7.8f, 1.6f, 1.6f));
            }
        };
    }

    static Icon delete() {
        return new GlyphIcon(SIZE, new Color(192, 57, 43), "\uD83D\uDDD1", 19f);
    }

    static Icon connect() {
        return new PaintedIcon(SIZE, new Color(45, 125, 154)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Float(2.8f, 6f, 6.2f, 6.2f, 300f, 220f, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(8.8f, 6f, 6.2f, 6.2f, 120f, 220f, Arc2D.OPEN));
                g2.draw(new Line2D.Float(7.2f, 9.1f, 10.8f, 9.1f));
            }
        };
    }

    static Icon refresh() {
        return new PaintedIcon(SIZE, new Color(49, 130, 206)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Float(3.5f, 3.5f, 10.5f, 10.5f, 30f, 250f, Arc2D.OPEN));
                GeneralPath arrow = new GeneralPath();
                arrow.moveTo(12.3f, 3.8f);
                arrow.lineTo(14.8f, 4.1f);
                arrow.lineTo(13.6f, 6.4f);
                arrow.closePath();
                g2.fill(arrow);
            }
        };
    }

    static Icon previousPage() {
        return new PaintedIcon(SIZE, new Color(75, 85, 99)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(11.8f, 4.2f, 6.2f, 9f));
                g2.draw(new Line2D.Float(6.2f, 9f, 11.8f, 13.8f));
            }
        };
    }

    static Icon nextPage() {
        return new PaintedIcon(SIZE, new Color(75, 85, 99)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(6.2f, 4.2f, 11.8f, 9f));
                g2.draw(new Line2D.Float(11.8f, 9f, 6.2f, 13.8f));
            }
        };
    }

    static Icon info() {
        return new PaintedIcon(SIZE, new Color(45, 125, 154)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Ellipse2D.Float(4.2f, 4.2f, 9.6f, 9.6f));
                g2.draw(new Line2D.Float(9f, 7.4f, 9f, 11.4f));
                g2.draw(new Line2D.Float(9f, 6.2f, 9f, 6.2f));
            }
        };
    }

    static Icon closeTab() {
        return new PaintedIcon(18, new Color(107, 114, 128)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Float(4.4f, 4.4f, 13.6f, 13.6f));
                g2.draw(new Line2D.Float(13.6f, 4.4f, 4.4f, 13.6f));
            }
        };
    }

    static Icon initialize() {
        return new PaintedIcon(SIZE, new Color(128, 90, 213)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Ellipse2D.Float(4.5f, 4.5f, 9f, 9f));
                g2.draw(new Line2D.Float(9f, 2.2f, 9f, 4.2f));
                g2.draw(new Line2D.Float(9f, 13.8f, 9f, 15.8f));
                g2.draw(new Line2D.Float(2.2f, 9f, 4.2f, 9f));
                g2.draw(new Line2D.Float(13.8f, 9f, 15.8f, 9f));
                g2.draw(new Line2D.Float(6.4f, 9f, 8.1f, 10.8f));
                g2.draw(new Line2D.Float(8.1f, 10.8f, 11.6f, 7.2f));
            }
        };
    }

    static Icon script() {
        return new PaintedIcon(SIZE, new Color(75, 85, 99)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(4f, 2.8f, 9.5f, 12.4f, 2f, 2f));
                g2.draw(new Line2D.Float(6f, 6.2f, 11.4f, 6.2f));
                g2.draw(new Line2D.Float(6f, 9f, 11.4f, 9f));
                g2.draw(new Line2D.Float(6f, 11.8f, 9.2f, 11.8f));
            }
        };
    }

    static Icon archiveExpired() {
        return new PaintedIcon(SIZE, new Color(214, 158, 46)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Rectangle2D.Float(4.2f, 5.2f, 9.6f, 7.8f));
                g2.draw(new Line2D.Float(7f, 3.2f, 11f, 3.2f));
                g2.draw(new Arc2D.Float(6.1f, 7.1f, 5.8f, 5.8f, 90f, -260f, Arc2D.OPEN));
            }
        };
    }

    static Icon archiveAll() {
        return new PaintedIcon(SIZE, new Color(22, 101, 52)) {
            @Override
            protected void paintIcon(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Rectangle2D.Float(3.8f, 5f, 10.4f, 8.2f));
                g2.draw(new Line2D.Float(7f, 3.2f, 11f, 3.2f));
                g2.draw(new Line2D.Float(6.2f, 9f, 8.2f, 11f));
                g2.draw(new Line2D.Float(8.2f, 11f, 12f, 7.2f));
            }
        };
    }

    private abstract static class PaintedIcon implements Icon {
        private final int size;
        private final Color activeColor;

        private PaintedIcon(int size, Color activeColor) {
            this.size = size;
            this.activeColor = activeColor;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float scale = size / BASE_SIZE;
                g2.scale(scale, scale);
                g2.setColor(c != null && c.isEnabled() ? activeColor : DISABLED_COLOR);
                paintIcon(g2);
            } finally {
                g2.dispose();
            }
        }

        protected abstract void paintIcon(Graphics2D g2);

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static final class GlyphIcon implements Icon {
        private final int size;
        private final Color activeColor;
        private final String glyph;
        private final float fontSize;

        private GlyphIcon(int size, Color activeColor, String glyph, float fontSize) {
            this.size = size;
            this.activeColor = activeColor;
            this.glyph = glyph;
            this.fontSize = fontSize;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(c != null && c.isEnabled() ? activeColor : DISABLED_COLOR);
                Font font = g2.getFont().deriveFont(Font.PLAIN, fontSize);
                g2.setFont(font);
                FontMetrics metrics = g2.getFontMetrics(font);
                int textX = (size - metrics.stringWidth(glyph)) / 2;
                int textY = (size - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(glyph, textX, textY);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
