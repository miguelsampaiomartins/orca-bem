package br.com.bolsoclaro;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class TrendChart extends JPanel {
    private List<YearMonth> months = List.of();
    private Function<YearMonth, BigDecimal> primaryAmounts = m -> BigDecimal.ZERO;
    private Function<YearMonth, BigDecimal> secondaryAmounts;
    private final Color incomeColor = new Color(100, 218, 169);
    private final Color expenseColor = new Color(244, 173, 111);
    private final Color muted = new Color(161, 181, 171);
    private final Color grid = new Color(48, 78, 67);
    private final Color card = new Color(20, 42, 38);

    public TrendChart() { setOpaque(false); setPreferredSize(new Dimension(430, 230)); }

    public void setData(List<YearMonth> months, Function<YearMonth, BigDecimal> amounts) {
        this.months = List.copyOf(months);
        this.primaryAmounts = amounts;
        this.secondaryAmounts = null;
        repaint();
    }

    public void setCashFlowData(List<YearMonth> months, Function<YearMonth, BigDecimal> income, Function<YearMonth, BigDecimal> expenses) {
        this.months = List.copyOf(months);
        this.primaryAmounts = income;
        this.secondaryAmounts = expenses;
        repaint();
    }

    @Override protected void paintComponent(Graphics raw) {
        super.paintComponent(raw);
        Graphics2D g = (Graphics2D) raw.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 62, right = 16, top = 14, bottom = 34;
        int width = Math.max(10, getWidth() - left - right), height = Math.max(10, getHeight() - top - bottom);
        BigDecimal max = BigDecimal.ZERO;
        for (YearMonth month : months) {
            max = max.max(safeAmount(primaryAmounts, month));
            if (secondaryAmounts != null) max = max.max(safeAmount(secondaryAmounts, month));
        }
        if (max.signum() == 0) max = BigDecimal.valueOf(100);
        double ceiling = Math.ceil(max.doubleValue() / 500.0) * 500.0;
        if (ceiling == 0) ceiling = 500;
        NumberFormat compact = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        compact.setMaximumFractionDigits(0);
        for (int row = 0; row <= 4; row++) {
            int y = top + height * row / 4;
            g.setColor(grid); g.setStroke(new BasicStroke(1f)); g.drawLine(left, y, left + width, y);
            g.setColor(muted); g.setFont(getFont().deriveFont(Font.PLAIN, 9f));
            g.drawString("R$ " + compact.format(ceiling * (4 - row) / 4), 2, y + 4);
        }
        if (months.isEmpty()) { g.dispose(); return; }
        drawSeries(g, primaryAmounts, incomeColor, ceiling, left, top, width, height, true);
        if (secondaryAmounts != null) drawSeries(g, secondaryAmounts, expenseColor, ceiling, left, top, width, height, false);
        int labelStep = Math.max(1, (months.size() + 5) / 6);
        for (int i = 0; i < months.size(); i++) {
            if (i != 0 && i != months.size() - 1 && i % labelStep != 0) continue;
            double x = xAt(i, months.size(), left, width);
            g.setColor(muted); g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
            String label = months.get(i).getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pt-BR"));
            if (months.size() > 12) label += "/" + String.valueOf(months.get(i).getYear()).substring(2);
            g.drawString(label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1), (int)x - 14, top + height + 23);
        }
        g.dispose();
    }

    private void drawSeries(Graphics2D g, Function<YearMonth, BigDecimal> amounts, Color color, double ceiling,
                            int left, int top, int width, int height, boolean fillArea) {
        Path2D line = new Path2D.Double();
        for (int i = 0; i < months.size(); i++) {
            double x = xAt(i, months.size(), left, width);
            double y = yAt(amounts, months.get(i), ceiling, top, height);
            if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
        }
        if (fillArea) {
            Path2D area = (Path2D) line.clone();
            double lastX = xAt(months.size() - 1, months.size(), left, width);
            area.lineTo(lastX, top + height); area.lineTo(left, top + height); area.closePath();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 23)); g.fill(area);
        }
        g.setColor(color); g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(line);
        for (int i = 0; i < months.size(); i++) {
            double x = xAt(i, months.size(), left, width);
            double y = yAt(amounts, months.get(i), ceiling, top, height);
            g.setColor(card); g.fillOval((int)x - 4, (int)y - 4, 8, 8);
            g.setColor(color); g.setStroke(new BasicStroke(2f)); g.drawOval((int)x - 4, (int)y - 4, 8, 8);
        }
    }

    private double xAt(int index, int count, int left, int width) {
        return left + (count == 1 ? width / 2.0 : width * index / (count - 1.0));
    }

    private double yAt(Function<YearMonth, BigDecimal> amounts, YearMonth month, double ceiling, int top, int height) {
        return top + height - height * Math.min(1.0, safeAmount(amounts, month).doubleValue() / ceiling);
    }

    private BigDecimal safeAmount(Function<YearMonth, BigDecimal> amounts, YearMonth month) {
        if (amounts == null) return BigDecimal.ZERO;
        BigDecimal result = amounts.apply(month);
        return result == null ? BigDecimal.ZERO : result;
    }
}
