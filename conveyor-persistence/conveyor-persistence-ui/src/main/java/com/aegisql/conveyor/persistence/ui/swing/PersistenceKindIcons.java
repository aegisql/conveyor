package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

final class PersistenceKindIcons {

    private static final float BASE_SIZE = 24f;
    private static final int SIZE = 30;

    private PersistenceKindIcons() {
    }

    static Icon forKind(PersistenceKind kind) {
        return switch (kind) {
            case MYSQL -> mysql();
            case MARIADB -> mariaDb();
            case POSTGRES -> postgres();
            case ORACLE -> oracle();
            case SQLSERVER -> sqlServer();
            case SQLITE -> sqlite();
            case DERBY -> derby();
            case REDIS -> redis();
        };
    }

    private static Icon mysql() {
        return new PaintedIcon(new Color(8, 104, 172), new Color(243, 156, 18, 44)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                GeneralPath dolphin = new GeneralPath();
                dolphin.moveTo(4.8f, 14.2f);
                dolphin.curveTo(7.2f, 8.0f, 12.0f, 5.2f, 17.2f, 6.0f);
                dolphin.curveTo(15.9f, 6.9f, 15.1f, 8.1f, 14.8f, 9.1f);
                dolphin.curveTo(16.7f, 8.9f, 18.1f, 9.6f, 19.6f, 10.9f);
                dolphin.curveTo(17.0f, 10.9f, 15.2f, 11.8f, 13.6f, 13.4f);
                dolphin.curveTo(11.6f, 15.4f, 9.2f, 16.6f, 6.2f, 16.8f);
                g2.draw(dolphin);
                g2.setColor(new Color(243, 156, 18));
                g2.draw(new Line2D.Float(14.5f, 17.8f, 19.5f, 17.8f));
                g2.draw(new Line2D.Float(14.5f, 19.8f, 18.6f, 19.8f));
            }
        };
    }

    private static Icon mariaDb() {
        return new PaintedIcon(new Color(8, 111, 131), new Color(193, 154, 107, 44)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                GeneralPath feather = new GeneralPath();
                feather.moveTo(6.2f, 18.0f);
                feather.curveTo(7.5f, 9.5f, 12.2f, 5.4f, 18.6f, 5.2f);
                feather.curveTo(16.1f, 8.0f, 14.8f, 10.7f, 14.1f, 14.4f);
                feather.curveTo(13.2f, 18.0f, 10.6f, 19.7f, 6.2f, 18.0f);
                g2.draw(feather);
                g2.setColor(new Color(193, 154, 107));
                g2.draw(new Line2D.Float(9.5f, 9.0f, 12.2f, 17.8f));
                g2.draw(new Line2D.Float(13.8f, 18.6f, 18.8f, 18.6f));
            }
        };
    }

    private static Icon postgres() {
        return new PaintedIcon(new Color(51, 102, 153), new Color(51, 102, 153, 36)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Float(5.0f, 5.5f, 6.0f, 7.0f, 80f, 220f, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(13.0f, 5.5f, 6.0f, 7.0f, -120f, 220f, Arc2D.OPEN));
                g2.draw(new RoundRectangle2D.Float(8.0f, 8.2f, 8.0f, 6.0f, 2.8f, 2.8f));
                g2.draw(new Line2D.Float(12.0f, 13.9f, 12.0f, 18.6f));
                g2.draw(new Line2D.Float(10.4f, 18.6f, 12.0f, 20.0f));
                g2.draw(new Line2D.Float(13.6f, 18.6f, 12.0f, 20.0f));
            }
        };
    }

    private static Icon oracle() {
        return new PaintedIcon(new Color(207, 45, 32), new Color(207, 45, 32, 30)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(4.8f, 8.0f, 14.4f, 8.0f, 8.0f, 8.0f));
            }
        };
    }

    private static Icon sqlServer() {
        return new PaintedIcon(new Color(193, 39, 45), new Color(193, 39, 45, 28)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                CubicCurve2D.Float left = new CubicCurve2D.Float(6.0f, 6.5f, 10.0f, 8.0f, 10.4f, 16.0f, 6.4f, 18.3f);
                CubicCurve2D.Float right = new CubicCurve2D.Float(17.8f, 6.5f, 13.8f, 8.0f, 13.4f, 16.0f, 17.4f, 18.3f);
                g2.draw(left);
                g2.draw(right);
                g2.draw(new Line2D.Float(8.2f, 8.0f, 15.6f, 16.8f));
                g2.draw(new Line2D.Float(15.8f, 8.0f, 8.4f, 16.8f));
            }
        };
    }

    private static Icon sqlite() {
        return new PaintedIcon(new Color(54, 134, 190), new Color(54, 134, 190, 34)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Ellipse2D.Float(5.2f, 5.2f, 13.6f, 4.0f));
                g2.draw(new Line2D.Float(5.2f, 7.2f, 5.2f, 17.0f));
                g2.draw(new Line2D.Float(18.8f, 7.2f, 18.8f, 17.0f));
                g2.draw(new Arc2D.Float(5.2f, 15.0f, 13.6f, 4.0f, 180f, 180f, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(5.2f, 9.8f, 13.6f, 4.0f, 180f, 180f, Arc2D.OPEN));
            }
        };
    }

    private static Icon derby() {
        return new PaintedIcon(new Color(184, 91, 32), new Color(184, 91, 32, 34)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Float(5.6f, 5.6f, 12.8f, 12.8f, 50f, 260f, Arc2D.OPEN));
                g2.draw(new Line2D.Float(9.6f, 8.2f, 14.2f, 8.2f));
                g2.draw(new Line2D.Float(9.6f, 12.0f, 14.8f, 12.0f));
                g2.draw(new Line2D.Float(9.6f, 15.8f, 14.0f, 15.8f));
                g2.setColor(new Color(56, 142, 60));
                g2.draw(new Line2D.Float(17.2f, 8.0f, 19.6f, 8.0f));
                g2.draw(new Line2D.Float(18.4f, 6.8f, 18.4f, 9.2f));
            }
        };
    }

    private static Icon redis() {
        return new PaintedIcon(new Color(220, 38, 38), new Color(220, 38, 38, 30)) {
            @Override
            protected void paintGlyph(Graphics2D g2) {
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(5.2f, 6.0f, 13.6f, 3.2f, 1.2f, 1.2f));
                g2.draw(new RoundRectangle2D.Float(4.0f, 10.4f, 16.0f, 3.2f, 1.2f, 1.2f));
                g2.draw(new RoundRectangle2D.Float(5.2f, 14.8f, 13.6f, 3.2f, 1.2f, 1.2f));
                g2.fill(new Rectangle2D.Float(10.8f, 5.0f, 2.2f, 2.2f));
                g2.fill(new Rectangle2D.Float(7.0f, 9.4f, 2.2f, 2.2f));
                g2.fill(new Rectangle2D.Float(14.8f, 9.4f, 2.2f, 2.2f));
            }
        };
    }

    private abstract static class PaintedIcon implements Icon {
        private final Color glyphColor;
        private final Color backgroundColor;

        private PaintedIcon(Color glyphColor, Color backgroundColor) {
            this.glyphColor = glyphColor;
            this.backgroundColor = backgroundColor;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float scale = SIZE / BASE_SIZE;
                g2.scale(scale, scale);
                g2.setColor(backgroundColor);
                g2.fill(new RoundRectangle2D.Float(1.4f, 1.4f, 21.2f, 21.2f, 6f, 6f));
                g2.setColor(glyphColor);
                paintGlyph(g2);
            } finally {
                g2.dispose();
            }
        }

        protected abstract void paintGlyph(Graphics2D g2);

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
