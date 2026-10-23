package br.com.bolsoclaro;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class OrcaBemApp extends JFrame {
    private static final Color INK = new Color(242, 246, 241);
    private static final Color MUTED = new Color(161, 181, 171);
    private static final Color GREEN = new Color(100, 218, 169);
    private static final Color ACCENT = new Color(244, 185, 99);
    private static final Color GREEN_LIGHT = new Color(37, 72, 59);
    private static final Color CANVAS = new Color(12, 28, 26);
    private static final Color CARD = new Color(20, 42, 38);
    private static final Color CARD_ALT = new Color(28, 55, 49);
    private static final Color BORDER = new Color(48, 78, 67);
    private static final Color RED = new Color(255, 140, 126);
    private static final Color PURPLE = new Color(185, 163, 232);
    private static final Color SIDEBAR = new Color(8, 22, 20);
    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Path dataDir = Path.of(System.getProperty("user.home"), ".bolso-claro");
    private final Path transactionsFile = dataDir.resolve("transacoes.csv");
    private final List<Transaction> transactions = new ArrayList<>();
    private BudgetStore budgetStore;
    private CategoryStore categoryStore;
    private SavingsGoalStore savingsGoalStore;
    private YearMonth selectedMonth = YearMonth.now();
    private String currentPage = "dashboard";
    private final JPanel pageHost = new JPanel(new CardLayout());
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel eyebrowLabel = new JLabel();
    private final JLabel pageTitle = new JLabel();
    private final JPanel sidebar = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public OrcaBemApp() throws IOException {
        super("OrçaBem — seu dinheiro em foco");
        transactions.addAll(CsvStore.load(transactionsFile));
        budgetStore = new BudgetStore(dataDir.resolve("orcamentos.properties"));
        categoryStore = new CategoryStore(dataDir.resolve("categorias.properties"));
        savingsGoalStore = new SavingsGoalStore(dataDir.resolve("metas.properties"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 700));
        setSize(1340, 860);
        setIconImages(AppIcon.windowImages());
        setLocationRelativeTo(null);
        getContentPane().setBackground(CANVAS);
        getContentPane().setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildMain(), BorderLayout.CENTER);
        refreshPage();
    }

    private JPanel buildSidebar() {
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(232, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 16, 18, 16));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); brand.setOpaque(false); brand.setMaximumSize(new Dimension(205, 52));
        JLabel mark = new JLabel(new javax.swing.ImageIcon(AppIcon.render(43)));
        JPanel brandCopy = new JPanel(); brandCopy.setOpaque(false); brandCopy.setLayout(new BoxLayout(brandCopy, BoxLayout.Y_AXIS)); brandCopy.setBorder(BorderFactory.createEmptyBorder(5, 11, 0, 0));
        JLabel brandName = new JLabel("OrçaBem"); brandName.setFont(new Font("Segoe UI", Font.BOLD, 15)); brandName.setForeground(INK);
        JLabel brandTagline = new JLabel("SEU DINHEIRO, BEM PLANEJADO"); brandTagline.setFont(new Font("Segoe UI", Font.BOLD, 8)); brandTagline.setForeground(ACCENT);
        brandCopy.add(brandName); brandCopy.add(Box.createVerticalStrut(3)); brandCopy.add(brandTagline); brand.add(mark); brand.add(brandCopy);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT); sidebar.add(brand); sidebar.add(Box.createVerticalStrut(31));
        addNav("dashboard", "⌂", "Visão geral");
        addNav("transactions", "⇄", "Lançamentos");
        addNav("budgets", "▤", "Orçamentos");
        addNav("categories", "◈", "Categorias");
        addNav("insights", "✧", "Insights");
        addNav("privacy", "◉", "Dados e privacidade");
        sidebar.add(Box.createVerticalGlue());
        JLabel privateMark = new JLabel("•", SwingConstants.CENTER); privateMark.setForeground(GREEN); privateMark.setFont(new Font("Segoe UI", Font.BOLD, 26));
        privateMark.setToolTipText("Privado por design · guardado neste computador"); privateMark.setAlignmentX(Component.CENTER_ALIGNMENT); sidebar.add(privateMark);
        return sidebar;
    }

    private void addNav(String key, String glyph, String accessibleName) {
        JButton button = new ThemeButton(glyph + "    " + accessibleName, SIDEBAR, new Color(31, 57, 47), SIDEBAR);
        button.setHorizontalAlignment(SwingConstants.LEFT); button.setFocusPainted(false); button.setBorderPainted(false); button.setOpaque(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12)); button.setPreferredSize(new Dimension(198, 44)); button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setAlignmentX(Component.LEFT_ALIGNMENT); button.setToolTipText(accessibleName); button.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
        button.addActionListener(e -> { currentPage = key; refreshPage(); });
        navButtons.put(key, button); sidebar.add(button); sidebar.add(Box.createVerticalStrut(10)); updateNavStyles();
    }

    private JPanel buildMain() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(CANVAS);
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false); header.setBorder(BorderFactory.createEmptyBorder(24, 30, 17, 30));
        JPanel titles = new JPanel(); titles.setOpaque(false); titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        eyebrowLabel.setFont(new Font("Segoe UI", Font.BOLD, 10)); eyebrowLabel.setForeground(ACCENT);
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 25)); pageTitle.setForeground(INK);
        JLabel subtitle = new JLabel("Uma leitura mais clara das entradas, saídas e do que vem pela frente."); subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12)); subtitle.setForeground(MUTED);
        titles.add(eyebrowLabel); titles.add(Box.createVerticalStrut(5)); titles.add(pageTitle); titles.add(Box.createVerticalStrut(4)); titles.add(subtitle);
        JPanel monthPicker = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0)); monthPicker.setOpaque(false);
        JButton previous = smallButton("‹"); previous.addActionListener(e -> { selectedMonth = selectedMonth.minusMonths(1); refreshPage(); });
        JButton next = smallButton("›"); next.addActionListener(e -> { selectedMonth = selectedMonth.plusMonths(1); refreshPage(); });
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 13)); monthLabel.setForeground(INK); monthLabel.setPreferredSize(new Dimension(142, 38));
        monthPicker.add(previous); monthPicker.add(monthLabel); monthPicker.add(next);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); headerActions.setOpaque(false);
        JButton quickAdd = primaryButton("+ Novo lançamento"); quickAdd.setToolTipText("Registrar uma receita ou despesa"); quickAdd.addActionListener(e -> addTransactionDialog());
        headerActions.add(monthPicker); headerActions.add(quickAdd);
        header.add(titles, BorderLayout.WEST); header.add(headerActions, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        pageHost.setOpaque(false); pageHost.setBorder(BorderFactory.createEmptyBorder(0, 30, 28, 30));
        root.add(pageHost, BorderLayout.CENTER);
        return root;
    }

    private void refreshPage() {
        monthLabel.setText(monthName(selectedMonth));
        pageTitle.setText(switch (currentPage) { case "transactions" -> "Movimentações"; case "budgets" -> "Orçamentos"; case "categories" -> "Categorias"; case "insights" -> "Insights"; case "privacy" -> "Dados e privacidade"; default -> "Seu dinheiro em foco."; });
        eyebrowLabel.setText("SEU RESUMO FINANCEIRO   /   " + monthName(selectedMonth).toUpperCase(PT));
        updateNavStyles();
        pageHost.removeAll();
        if (currentPage.equals("transactions")) pageHost.add(buildTransactionsPage());
        else if (currentPage.equals("budgets")) pageHost.add(buildBudgetsPage());
        else if (currentPage.equals("categories")) pageHost.add(buildCategoriesPage());
        else if (currentPage.equals("insights")) pageHost.add(buildInsightsPage());
        else if (currentPage.equals("privacy")) pageHost.add(buildPrivacyPage());
        else pageHost.add(buildDashboardPage());
        pageHost.revalidate(); pageHost.repaint();
    }

    private void updateNavStyles() {
        navButtons.forEach((key, button) -> {
            boolean active = currentPage.equals(key);
            button.setBackground(active ? new Color(32, 58, 47) : SIDEBAR);
            button.setForeground(active ? ACCENT : new Color(163, 181, 171));
        });
    }

    private JPanel buildDashboardPage() {
        JPanel page = new JPanel(new BorderLayout(0, 17)); page.setOpaque(false);
        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        BigDecimal income = total(selectedMonth, Transaction.Type.INCOME);
        BigDecimal expense = total(selectedMonth, Transaction.Type.EXPENSE);
        BigDecimal balance = income.subtract(expense);
        Map<String, BigDecimal> budgets = budgetStore.forMonth(selectedMonth);
        BigDecimal planned = budgets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal budgetRemaining = planned.subtract(expense);
        String budgetNote = planned.signum() == 0 ? "Defina limites por categoria" : percent(expense, planned) + " de " + money(planned) + " planejado";
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0)); stats.setOpaque(false); stats.setPreferredSize(new Dimension(1000, 132)); stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        stats.add(statCard("SOBROU DEPOIS DOS GASTOS", money(balance), monthName(selectedMonth) + " · receitas menos despesas", balance.signum() >= 0 ? GREEN : RED));
        stats.add(statCard("RECEITAS", money(income), "Entradas neste mês", GREEN));
        stats.add(statCard("DESPESAS", money(expense), expensesByCategory(selectedMonth).size() + " categorias", RED));
        stats.add(statCard("ORÇAMENTO RESTANTE", money(budgetRemaining), budgetNote, budgetRemaining.signum() >= 0 ? PURPLE : RED));
        content.add(stats); content.add(Box.createVerticalStrut(14));
        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0)); charts.setOpaque(false); charts.setPreferredSize(new Dimension(800, 315)); charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 315));
        charts.add(categoryCard()); charts.add(trendCard());
        content.add(charts); content.add(Box.createVerticalStrut(14));
        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0)); lower.setOpaque(false); lower.setPreferredSize(new Dimension(800, 210)); lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        lower.add(analysisCard()); lower.add(recentActivityCard()); content.add(lower);
        content.add(Box.createVerticalStrut(14)); content.add(savingsGoalCard());
        JScrollPane dashboardScroll = new JScrollPane(content); dashboardScroll.setBorder(BorderFactory.createEmptyBorder()); dashboardScroll.setOpaque(false); dashboardScroll.getViewport().setOpaque(false); dashboardScroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(dashboardScroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel statCard(String title, String value, String note, Color accent) {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout());
        JPanel stripe = new JPanel(); stripe.setBackground(accent); stripe.setPreferredSize(new Dimension(4, 1)); card.add(stripe, BorderLayout.WEST);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS)); copy.setBorder(BorderFactory.createEmptyBorder(15, 16, 13, 12)); card.add(copy, BorderLayout.CENTER);
        JLabel eyebrow = new JLabel(title); eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 10)); eyebrow.setForeground(MUTED);
        JLabel number = new JLabel(value); number.setFont(new Font("Segoe UI", Font.BOLD, 24)); number.setForeground(INK);
        JLabel description = new JLabel(note); description.setFont(new Font("Segoe UI", Font.PLAIN, 11)); description.setForeground(accent);
        copy.add(eyebrow); copy.add(Box.createVerticalStrut(10)); copy.add(number); copy.add(Box.createVerticalStrut(6)); copy.add(description);
        return card;
    }

    private JPanel savingsGoalCard() {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(14, 8)); card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(15, 18, 15, 18)));
        BigDecimal goal = savingsGoalStore.get(selectedMonth);
        BigDecimal net = total(selectedMonth, Transaction.Type.INCOME).subtract(total(selectedMonth, Transaction.Type.EXPENSE));
        BigDecimal saved = net.max(BigDecimal.ZERO);
        double fraction = goal.signum() > 0 ? Math.min(1.0, saved.divide(goal, 4, RoundingMode.HALF_UP).doubleValue()) : 0.0;
        String description = goal.signum() > 0
                ? "Saldo líquido de " + money(saved) + " de uma meta de " + money(goal) + " (" + percent(saved, goal) + ")."
                : "Defina um valor-alvo e acompanhe o progresso do mês sem perder o controle do saldo.";
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(sectionHeading("Meta de economia", "Objetivo pessoal para " + monthName(selectedMonth)));
        copy.add(Box.createVerticalStrut(8));
        JLabel detail = new JLabel(description); detail.setFont(new Font("Segoe UI", Font.PLAIN, 11)); detail.setForeground(MUTED); copy.add(detail);
        JPanel progress = progressTrack(fraction, goal.signum() > 0 && saved.compareTo(goal) >= 0 ? GREEN : ACCENT, 9);
        copy.add(Box.createVerticalStrut(10)); copy.add(progress);
        JButton edit = secondaryButton(goal.signum() > 0 ? "Alterar meta" : "Definir meta"); edit.addActionListener(e -> editSavingsGoalDialog());
        card.add(copy, BorderLayout.CENTER); card.add(edit, BorderLayout.EAST); return card;
    }

    private void editSavingsGoalDialog() {
        JDialog dialog = new JDialog(this, "Meta de economia", true); dialog.setSize(430, 235); dialog.setLocationRelativeTo(this);
        JPanel content = new JPanel(new BorderLayout(0, 14)); content.setBackground(CARD); content.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));
        JLabel title = new JLabel("Objetivo para " + monthName(selectedMonth)); title.setFont(new Font("Segoe UI", Font.BOLD, 15)); title.setForeground(INK);
        JLabel help = new JLabel("Quanto você quer manter de saldo positivo neste mês?"); help.setFont(new Font("Segoe UI", Font.PLAIN, 11)); help.setForeground(MUTED);
        JPanel prompt = new JPanel(); prompt.setOpaque(false); prompt.setLayout(new BoxLayout(prompt, BoxLayout.Y_AXIS)); prompt.add(title); prompt.add(Box.createVerticalStrut(5)); prompt.add(help);
        JTextField amount = new JTextField(savingsGoalStore.get(selectedMonth).signum() > 0 ? savingsGoalStore.get(selectedMonth).toPlainString() : ""); styleInput(amount); installCurrencyFormatting(amount);
        JPanel field = new JPanel(new BorderLayout(9, 0)); field.setOpaque(false); JLabel currency = new JLabel("R$", SwingConstants.CENTER); currency.setForeground(MUTED); currency.setPreferredSize(new Dimension(34, 32)); field.add(currency, BorderLayout.WEST); field.add(amount, BorderLayout.CENTER);
        JPanel center = new JPanel(new BorderLayout(0, 8)); center.setOpaque(false); center.add(prompt, BorderLayout.NORTH); center.add(field, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        JButton cancel = secondaryButton("Cancelar"); cancel.addActionListener(e -> dialog.dispose());
        JButton remove = secondaryButton("Remover meta"); remove.addActionListener(e -> { try { savingsGoalStore.set(selectedMonth, BigDecimal.ZERO); dialog.dispose(); refreshPage(); } catch (IOException ex) { JOptionPane.showMessageDialog(dialog, "Não foi possível salvar: " + ex.getMessage()); } });
        JButton save = primaryButton("Salvar meta"); save.addActionListener(e -> {
            try {
                String text = amount.getText().trim(); BigDecimal value = text.isEmpty() ? BigDecimal.ZERO : parseMoney(text);
                if (value.signum() < 0) throw new IllegalArgumentException();
                savingsGoalStore.set(selectedMonth, value); dialog.dispose(); refreshPage();
            } catch (Exception ex) { JOptionPane.showMessageDialog(dialog, "Informe um valor positivo, como 1500,00.", "Meta inválida", JOptionPane.WARNING_MESSAGE); }
        });
        actions.add(remove); actions.add(cancel); actions.add(save); content.add(center, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH);
        dialog.setLayout(new BorderLayout()); dialog.add(content); dialog.setVisible(true);
    }

    private JPanel progressTrack(double fraction, Color color, int height) {
        JPanel track = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics raw) {
                java.awt.Graphics2D g = (java.awt.Graphics2D) raw.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - 1, h = getHeight() - 1;
                g.setColor(CARD_ALT); g.fillRoundRect(0, 0, w, h, h, h);
                int fillWidth = fraction <= 0 ? 0 : Math.max(h, (int)(w * Math.max(0, Math.min(1, fraction))));
                g.setColor(color); if (fillWidth > 0) g.fillRoundRect(0, 0, fillWidth, h, h, h);
                g.dispose();
            }
        };
        track.setOpaque(false); track.setPreferredSize(new Dimension(500, height)); track.setMinimumSize(new Dimension(80, height));
        return track;
    }

    private JPanel categoryCard() {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 10)); card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(17, 18, 14, 18)));
        Map<String, BigDecimal> byCategory = expensesByCategory(selectedMonth);
        BigDecimal totalSpent = total(selectedMonth, Transaction.Type.EXPENSE);
        card.add(sectionHeading("Distribuição dos gastos", "Por categoria neste mês · " + money(totalSpent)), BorderLayout.NORTH);
        JPanel rows = new JPanel(); rows.setOpaque(false); rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        if (byCategory.isEmpty()) rows.add(emptyMessage("Nenhuma despesa neste mês ainda."));
        else byCategory.entrySet().stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()).limit(6).forEach(entry -> rows.add(categoryBar(entry.getKey(), entry.getValue(), totalSpent)));
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel categoryBar(String category, BigDecimal amount, BigDecimal totalSpent) {
        JPanel row = new JPanel(new BorderLayout(8, 3)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(1, 0, 5, 0));
        JLabel name = new JLabel(category); name.setFont(new Font("Segoe UI", Font.PLAIN, 11)); name.setForeground(INK);
        JLabel value = new JLabel(percent(amount, totalSpent) + "  ·  " + money(amount)); value.setFont(new Font("Segoe UI", Font.BOLD, 10)); value.setForeground(MUTED);
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(name, BorderLayout.WEST); top.add(value, BorderLayout.EAST);
        JPanel track = new JPanel(new BorderLayout()); track.setBackground(CARD_ALT); track.setPreferredSize(new Dimension(100, 6));
        JPanel fill = new JPanel(); fill.setBackground(GREEN); int width = Math.max(4, (int)(290 * amount.doubleValue() / Math.max(1, totalSpent.doubleValue()))); fill.setPreferredSize(new Dimension(width, 6)); track.add(fill, BorderLayout.WEST);
        row.add(top, BorderLayout.NORTH); row.add(track, BorderLayout.SOUTH); return row;
    }

    private JPanel trendCard() {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 6)); card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(17, 18, 11, 14)));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(sectionHeading("Fluxo de caixa ao longo do tempo", "Seis meses de entradas e saídas"), BorderLayout.WEST);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 3)); legend.setOpaque(false);
        JLabel incomeLegend = new JLabel("● Receitas"); incomeLegend.setForeground(GREEN); incomeLegend.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JLabel expenseLegend = new JLabel("● Despesas"); expenseLegend.setForeground(PURPLE); expenseLegend.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        legend.add(incomeLegend); legend.add(expenseLegend); header.add(legend, BorderLayout.EAST); card.add(header, BorderLayout.NORTH);
        List<YearMonth> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) months.add(selectedMonth.minusMonths(i));
        TrendChart chart = new TrendChart(); chart.setCashFlowData(months, m -> total(m, Transaction.Type.INCOME), m -> total(m, Transaction.Type.EXPENSE));
        card.add(chart, BorderLayout.CENTER); return card;
    }

    private JPanel analysisCard() {
        return analysisCard(3);
    }

    private JPanel fullAnalysisCard() {
        return analysisCard(Integer.MAX_VALUE);
    }

    private JPanel analysisCard(int maximumItems) {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 9)); card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(16, 18, 15, 18)));
        card.add(sectionHeading("Vale saber", "Observações úteis sobre este mês"), BorderLayout.NORTH);
        JPanel list = new JPanel(); list.setOpaque(false); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        List<String> insights = buildInsights();
        if (insights.isEmpty()) insights.add("Defina um orçamento mensal por categoria para receber análises personalizadas.");
        for (String text : insights.stream().limit(maximumItems).toList()) {
            JPanel line = new JPanel(new BorderLayout(9, 0)); line.setOpaque(false); line.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            JLabel bullet = new JLabel("●"); bullet.setForeground(GREEN); bullet.setFont(new Font("Segoe UI", Font.BOLD, 10));
            JLabel message = new JLabel(text); message.setForeground(INK); message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            line.add(bullet, BorderLayout.WEST); line.add(message, BorderLayout.CENTER); list.add(line);
        }
        card.add(list, BorderLayout.CENTER); return card;
    }

    private JPanel recentActivityCard() {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 8)); card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(16, 18, 13, 18)));
        JPanel heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        heading.add(sectionHeading("Atividade recente", "Últimos lançamentos deste mês"), BorderLayout.WEST);
        JButton viewAll = secondaryButton("Ver todos"); viewAll.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        viewAll.addActionListener(e -> { currentPage = "transactions"; refreshPage(); }); heading.add(viewAll, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        JPanel rows = new JPanel(); rows.setOpaque(false); rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        List<Transaction> recent = transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(selectedMonth)).sorted(Comparator.comparing(Transaction::getDate).reversed()).limit(4).collect(Collectors.toList());
        if (recent.isEmpty()) rows.add(emptyMessage("Seus lançamentos aparecerão aqui."));
        else for (Transaction t : recent) {
            JPanel row = new JPanel(new BorderLayout(10, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JPanel details = new JPanel(); details.setOpaque(false); details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
            JLabel title = new JLabel(t.getDescription()); title.setFont(new Font("Segoe UI", Font.BOLD, 11)); title.setForeground(INK);
            JLabel subtitle = new JLabel(t.getCategory() + " · " + t.getDate().format(DateTimeFormatter.ofPattern("dd MMM", PT))); subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 10)); subtitle.setForeground(MUTED);
            details.add(title); details.add(Box.createVerticalStrut(3)); details.add(subtitle);
            JLabel amount = new JLabel((t.getType() == Transaction.Type.INCOME ? "+ " : "− ") + money(t.getAmount())); amount.setFont(new Font("Segoe UI", Font.BOLD, 11)); amount.setForeground(t.getType() == Transaction.Type.INCOME ? GREEN : INK);
            row.add(details, BorderLayout.WEST); row.add(amount, BorderLayout.EAST); rows.add(row);
        }
        card.add(rows, BorderLayout.CENTER); return card;
    }

    private List<String> buildInsights() {
        List<String> out = new ArrayList<>();
        Map<String, BigDecimal> spends = expensesByCategory(selectedMonth);
        Map<String, BigDecimal> budgets = budgetStore.forMonth(selectedMonth);
        for (Map.Entry<String, BigDecimal> entry : spends.entrySet()) {
            BigDecimal budget = budgets.get(entry.getKey());
            if (budget == null || budget.signum() <= 0) continue;
            BigDecimal ratio = entry.getValue().multiply(BigDecimal.valueOf(100)).divide(budget, 0, RoundingMode.HALF_UP);
            if (entry.getValue().compareTo(budget) > 0) {
                BigDecimal excess = entry.getValue().subtract(budget).multiply(BigDecimal.valueOf(100)).divide(budget, 0, RoundingMode.HALF_UP);
                out.add(entry.getKey() + " está " + excess.toPlainString() + "% acima do orçamento (" + money(entry.getValue()) + " de " + money(budget) + ").");
            } else if (ratio.compareTo(BigDecimal.valueOf(80)) >= 0) {
                out.add(entry.getKey() + " já usou " + ratio.toPlainString() + "% do orçamento; restam " + money(budget.subtract(entry.getValue())) + ".");
            }
        }
        BigDecimal income = total(selectedMonth, Transaction.Type.INCOME);
        BigDecimal expense = total(selectedMonth, Transaction.Type.EXPENSE);
        if (income.signum() > 0 && expense.compareTo(income) > 0) out.add("Suas despesas superam as receitas em " + money(expense.subtract(income)) + " neste mês.");
        else if (income.signum() > 0 && expense.compareTo(income) <= 0) out.add("Você guardou " + percent(expense.signum() == 0 ? BigDecimal.ZERO : income.subtract(expense), income) + " das receitas deste mês.");
        YearMonth nowMonth = YearMonth.now();
        if (!selectedMonth.isAfter(nowMonth)) {
            int day = selectedMonth.equals(nowMonth) ? LocalDate.now().getDayOfMonth() : selectedMonth.lengthOfMonth();
            LocalDate currentEnd = selectedMonth.atDay(Math.min(day, selectedMonth.lengthOfMonth()));
            YearMonth previousMonth = selectedMonth.minusMonths(1);
            LocalDate previousEnd = previousMonth.atDay(Math.min(day, previousMonth.lengthOfMonth()));
            BigDecimal currentPeriod = transactions.stream().filter(t -> t.getType() == Transaction.Type.EXPENSE && YearMonth.from(t.getDate()).equals(selectedMonth) && !t.getDate().isAfter(currentEnd)).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal previousPeriod = transactions.stream().filter(t -> t.getType() == Transaction.Type.EXPENSE && YearMonth.from(t.getDate()).equals(previousMonth) && !t.getDate().isAfter(previousEnd)).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (previousPeriod.signum() > 0) {
                BigDecimal change = currentPeriod.subtract(previousPeriod).abs().multiply(BigDecimal.valueOf(100)).divide(previousPeriod, 0, RoundingMode.HALF_UP);
                int comparison = currentPeriod.compareTo(previousPeriod);
                String comparisonText = comparison > 0 ? change + "% a mais" : comparison < 0 ? change + "% a menos" : "no mesmo nível";
                out.add("Até o dia " + day + ", você gastou " + money(currentPeriod) + " neste mês e " + money(previousPeriod) + " no anterior (" + comparisonText + ").");
            }
        }
        if (out.isEmpty() && !budgets.isEmpty()) out.add("Tudo dentro do planejado até agora. Continue registrando os lançamentos para acompanhar o mês.");
        return out;
    }

    private JPanel buildInsightsPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(fullAnalysisCard()); content.add(Box.createVerticalStrut(14));
        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0)); charts.setOpaque(false); charts.setPreferredSize(new Dimension(800, 315)); charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 315));
        charts.add(categoryCard()); charts.add(trendCard()); content.add(charts);
        JScrollPane scroll = new JScrollPane(content); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scroll, BorderLayout.CENTER); return page;
    }

    private JPanel buildPrivacyPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel privacyCard = cardPanel(); privacyCard.setLayout(new BoxLayout(privacyCard, BoxLayout.Y_AXIS));
        privacyCard.setBorder(BorderFactory.createCompoundBorder(privacyCard.getBorder(), BorderFactory.createEmptyBorder(22, 24, 22, 24))); privacyCard.setAlignmentX(Component.LEFT_ALIGNMENT); privacyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        JLabel badge = new JLabel("●  ARMAZENAMENTO LOCAL"); badge.setFont(new Font("Segoe UI", Font.BOLD, 10)); badge.setForeground(GREEN);
        JLabel heading = new JLabel("Seus dados ficam com você."); heading.setFont(new Font("Segoe UI", Font.BOLD, 19)); heading.setForeground(INK);
        JLabel explanation = new JLabel("<html><div style='width:760px'>O OrçaBem salva seus lançamentos neste computador. Não é necessária uma conta e os dados não são enviados a um servidor.</div></html>"); explanation.setFont(new Font("Segoe UI", Font.PLAIN, 12)); explanation.setForeground(MUTED);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT); heading.setAlignmentX(Component.LEFT_ALIGNMENT); explanation.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea location = new JTextArea(dataDir.toString()); location.setRows(2); location.setColumns(72); location.setEditable(false); location.setLineWrap(true); location.setWrapStyleWord(true); location.setBackground(CARD_ALT); location.setForeground(INK); location.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11)); location.setPreferredSize(new Dimension(700, 52)); location.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52)); location.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0)); actions.setOpaque(false); actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton export = primaryButton("Exportar lançamentos CSV"); export.addActionListener(e -> exportCsv());
        JButton importButton = secondaryButton("Importar CSV"); importButton.addActionListener(e -> importCsv());
        JButton open = secondaryButton("Abrir pasta local"); open.addActionListener(e -> openDataFolder());
        actions.add(export); actions.add(importButton); actions.add(open);
        privacyCard.add(badge); privacyCard.add(Box.createVerticalStrut(13)); privacyCard.add(heading); privacyCard.add(Box.createVerticalStrut(7)); privacyCard.add(explanation); privacyCard.add(Box.createVerticalStrut(17)); privacyCard.add(location); privacyCard.add(Box.createVerticalStrut(17)); privacyCard.add(actions);
        content.add(privacyCard);
        JScrollPane scroll = new JScrollPane(content); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        page.add(scroll, BorderLayout.CENTER); return page;
    }

    private void openDataFolder() {
        try {
            Files.createDirectories(dataDir);
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(dataDir.toFile());
            else JOptionPane.showMessageDialog(this, "A pasta local fica em:\n" + dataDir);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Não foi possível abrir a pasta local:\n" + ex.getMessage(), "Pasta de dados", JOptionPane.WARNING_MESSAGE); }
    }

    private JPanel buildTransactionsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14)); page.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout()); toolbar.setOpaque(false);
        JLabel label = new JLabel("Registre e acompanhe suas entradas e saídas."); label.setFont(new Font("Segoe UI", Font.PLAIN, 12)); label.setForeground(MUTED);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        JButton importButton = secondaryButton("Importar CSV"); importButton.addActionListener(e -> importCsv());
        JButton exportButton = secondaryButton("Exportar CSV"); exportButton.addActionListener(e -> exportCsv());
        actions.add(importButton); actions.add(exportButton);
        toolbar.add(label, BorderLayout.WEST); toolbar.add(actions, BorderLayout.EAST);
        JTextField search = new JTextField(); search.setPreferredSize(new Dimension(270, 34)); search.setToolTipText("Buscar por descrição ou categoria");
        styleInput(search);
        javax.swing.JComboBox<String> typeFilter = new ThemeComboBox<>(new String[]{"Todos os tipos", "Despesas", "Receitas"}); typeFilter.setPreferredSize(new Dimension(150, 34)); styleInput(typeFilter);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 9)); filters.setOpaque(false);
        JLabel searchLabel = new JLabel("Buscar"); searchLabel.setForeground(MUTED); searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        filters.add(searchLabel); filters.add(search); filters.add(typeFilter);
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(toolbar, BorderLayout.NORTH); top.add(filters, BorderLayout.SOUTH);
        page.add(top, BorderLayout.NORTH);
        String[] columns = {"Data", "Descrição", "Categoria", "Tipo", "Valor"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
        JTable table = new JTable(model); table.setRowHeight(42); table.setFont(new Font("Segoe UI", Font.PLAIN, 12)); table.setForeground(INK); table.setBackground(CARD); table.setGridColor(BORDER); table.setShowVerticalLines(false); table.setFillsViewportHeight(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setSelectionBackground(GREEN_LIGHT); table.setSelectionForeground(INK); table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11)); table.getTableHeader().setForeground(MUTED); table.getTableHeader().setBackground(CARD_ALT); table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setUI(new javax.swing.plaf.basic.BasicTableHeaderUI());
        table.getTableHeader().setDefaultRenderer((source, value, selected, focused, row, column) -> {
            JLabel headerCell = new JLabel("  " + String.valueOf(value)); headerCell.setOpaque(true); headerCell.setBackground(CARD_ALT); headerCell.setForeground(INK); headerCell.setFont(new Font("Segoe UI", Font.BOLD, 11));
            headerCell.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), BorderFactory.createEmptyBorder(0, 6, 0, 6)));
            return headerCell;
        });
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(BORDER)); scroll.setBackground(CARD); scroll.getViewport().setBackground(CARD);
        JPanel wrapper = cardPanel(); wrapper.setLayout(new BorderLayout()); wrapper.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); wrapper.add(scroll, BorderLayout.CENTER);
        JLabel empty = emptyMessage("Nenhum lançamento neste mês. Adicione um lançamento ou importe um CSV."); empty.setVisible(false); wrapper.add(empty, BorderLayout.SOUTH);
        page.add(wrapper, BorderLayout.CENTER);
        List<Transaction> filtered = new ArrayList<>();
        Runnable populate = () -> {
            model.setRowCount(0); filtered.clear();
            String query = search.getText().trim().toLowerCase(PT);
            String kind = String.valueOf(typeFilter.getSelectedItem());
            transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(selectedMonth))
                    .filter(t -> kind.equals("Todos os tipos") || (kind.equals("Despesas") && t.getType() == Transaction.Type.EXPENSE) || (kind.equals("Receitas") && t.getType() == Transaction.Type.INCOME))
                    .filter(t -> query.isEmpty() || t.getDescription().toLowerCase(PT).contains(query) || t.getCategory().toLowerCase(PT).contains(query))
                    .sorted(Comparator.comparing(Transaction::getDate).reversed())
                    .forEach(t -> { filtered.add(t); model.addRow(new Object[]{t.getDate().format(DATE_BR), t.getDescription(), t.getCategory(), t.getType() == Transaction.Type.INCOME ? "Receita" : "Despesa", (t.getType() == Transaction.Type.INCOME ? "+ " : "− ") + money(t.getAmount())}); });
            empty.setText(filtered.isEmpty() && !query.isEmpty() ? "Nenhum lançamento corresponde à busca. Tente outro termo ou filtro." : "Nenhum lançamento neste mês. Adicione um lançamento ou importe um CSV.");
            empty.setVisible(filtered.isEmpty());
        };
        populate.run();
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { populate.run(); }
            public void removeUpdate(DocumentEvent e) { populate.run(); }
            public void changedUpdate(DocumentEvent e) { populate.run(); }
        });
        typeFilter.addActionListener(e -> populate.run());
        JButton edit = secondaryButton("Editar selecionado"); edit.setEnabled(false);
        JButton delete = secondaryButton("Excluir selecionado"); delete.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) { boolean selected = table.getSelectedRow() >= 0; edit.setEnabled(selected); delete.setEnabled(selected); } });
        edit.addActionListener(e -> { int row = table.getSelectedRow(); if (row >= 0 && row < filtered.size()) addTransactionDialog(filtered.get(row)); });
        delete.addActionListener(e -> { int row = table.getSelectedRow(); if (row < 0 || row >= filtered.size()) return;
            Transaction chosen = filtered.get(row); int confirm = JOptionPane.showConfirmDialog(this, "Excluir “" + chosen.getDescription() + "”?");
            if (confirm == JOptionPane.YES_OPTION) { transactions.removeIf(t -> t.getId().equals(chosen.getId())); persistTransactions(); refreshPage(); }
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); bottom.setOpaque(false); bottom.add(edit); bottom.add(delete); page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildBudgetsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 15)); page.setOpaque(false);
        JPanel intro = new JPanel(); intro.setOpaque(false); intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
        JLabel lead = new JLabel("Dê um limite mensal para cada categoria de despesa."); lead.setForeground(INK); lead.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel help = new JLabel("Deixe em branco ou zero quando não quiser definir um limite. Os limites são salvos por mês."); help.setForeground(MUTED); help.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        intro.add(lead); intro.add(Box.createVerticalStrut(5)); intro.add(help); page.add(intro, BorderLayout.NORTH);
        JPanel form = cardPanel(); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS)); form.setBorder(BorderFactory.createCompoundBorder(form.getBorder(), BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        Map<String, BigDecimal> currentBudgets = budgetStore.forMonth(selectedMonth);
        List<String> categories = new ArrayList<>(categoryStore.categories(Transaction.Type.EXPENSE));
        transactions.stream().filter(t -> t.getType() == Transaction.Type.EXPENSE).map(Transaction::getCategory).distinct().forEach(c -> { if (!categories.contains(c)) categories.add(c); });
        currentBudgets.keySet().forEach(c -> { if (!categories.contains(c)) categories.add(c); });
        Map<String, BigDecimal> spentByCategory = expensesByCategory(selectedMonth);
        Map<String, JTextField> fields = new LinkedHashMap<>();
        for (String category : categories) {
            JPanel row = new JPanel(new BorderLayout(10, 0)); row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 63)); row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            JLabel name = new JLabel(category); name.setFont(new Font("Segoe UI", Font.PLAIN, 13)); name.setForeground(INK);
            BigDecimal spent = spentByCategory.getOrDefault(category, BigDecimal.ZERO); BigDecimal limit = currentBudgets.getOrDefault(category, BigDecimal.ZERO);
            String usageText = limit.signum() > 0 ? percent(spent, limit) + " usado · " + money(spent) : money(spent) + " gastos · sem limite";
            JLabel usage = new JLabel(usageText); usage.setFont(new Font("Segoe UI", Font.PLAIN, 10)); usage.setForeground(limit.signum() > 0 && spent.compareTo(limit) > 0 ? RED : MUTED);
            JPanel categoryCopy = new JPanel(); categoryCopy.setOpaque(false); categoryCopy.setLayout(new BoxLayout(categoryCopy, BoxLayout.Y_AXIS)); categoryCopy.add(name); categoryCopy.add(Box.createVerticalStrut(3)); categoryCopy.add(usage);
            JTextField field = new JTextField(currentBudgets.containsKey(category) ? currentBudgets.get(category).toPlainString() : ""); field.setHorizontalAlignment(SwingConstants.RIGHT); field.setPreferredSize(new Dimension(170, 33)); field.setFont(new Font("Segoe UI", Font.PLAIN, 12)); styleInput(field); installCurrencyFormatting(field); fields.put(category, field);
            JLabel currency = new JLabel("R$", SwingConstants.CENTER); currency.setForeground(MUTED); currency.setPreferredSize(new Dimension(30, 30));
            JPanel input = new JPanel(new BorderLayout()); input.setOpaque(false); input.add(currency, BorderLayout.WEST); input.add(field, BorderLayout.CENTER);
            row.setBorder(BorderFactory.createCompoundBorder(row.getBorder(), BorderFactory.createEmptyBorder(7, 2, 7, 3))); row.add(categoryCopy, BorderLayout.WEST); row.add(input, BorderLayout.EAST); form.add(row);
        }
        JScrollPane scroll = new JScrollPane(form); scroll.setBorder(BorderFactory.createLineBorder(BORDER)); scroll.setBackground(CARD); scroll.getViewport().setBackground(CARD); page.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        BigDecimal assigned = currentBudgets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentSpent = total(selectedMonth, Transaction.Type.EXPENSE);
        JLabel sum = new JLabel("Planejado: " + money(assigned) + "   ·   Gasto: " + money(currentSpent) + "   ·   Restante: " + money(assigned.subtract(currentSpent))); sum.setFont(new Font("Segoe UI", Font.BOLD, 12)); sum.setForeground(INK);
        Runnable updateTotal = () -> {
            BigDecimal total = BigDecimal.ZERO;
            for (JTextField field : fields.values()) {
                if (!field.getText().trim().isEmpty()) try { total = total.add(parseMoney(field.getText())); } catch (NumberFormatException ignored) { }
            }
            sum.setText("Planejado: " + money(total) + "   ·   Gasto: " + money(currentSpent) + "   ·   Restante: " + money(total.subtract(currentSpent)));
        };
        fields.values().forEach(field -> field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateTotal.run(); }
            public void removeUpdate(DocumentEvent e) { updateTotal.run(); }
            public void changedUpdate(DocumentEvent e) { updateTotal.run(); }
        }));
        JButton save = primaryButton("Salvar orçamentos");
        save.addActionListener(e -> {
            try {
                Map<String, BigDecimal> values = new LinkedHashMap<>();
                for (Map.Entry<String, JTextField> entry : fields.entrySet()) {
                    String text = entry.getValue().getText().trim();
                    if (!text.isEmpty()) { BigDecimal amount = parseMoney(text); if (amount.signum() < 0) throw new IllegalArgumentException("Use valores positivos ou zero."); if (amount.signum() > 0) values.put(entry.getKey(), amount); }
                }
                budgetStore.setAll(selectedMonth, values); JOptionPane.showMessageDialog(this, "Orçamentos de " + monthName(selectedMonth) + " salvos."); refreshPage();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Confira os valores informados. Use, por exemplo, 450,00.", "Valor inválido", JOptionPane.WARNING_MESSAGE); }
        });
        footer.add(sum, BorderLayout.WEST); footer.add(save, BorderLayout.EAST); page.add(footer, BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildCategoriesPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16)); page.setOpaque(false);
        JPanel intro = new JPanel(); intro.setOpaque(false); intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
        JLabel lead = new JLabel("Organize os nomes que fazem sentido para a sua rotina."); lead.setFont(new Font("Segoe UI", Font.BOLD, 14)); lead.setForeground(INK);
        JLabel help = new JLabel("Categorias removidas deixam de aparecer em novos lançamentos; o histórico e os orçamentos já salvos são preservados."); help.setFont(new Font("Segoe UI", Font.PLAIN, 11)); help.setForeground(MUTED);
        intro.add(lead); intro.add(Box.createVerticalStrut(5)); intro.add(help); page.add(intro, BorderLayout.NORTH);
        JPanel columns = new JPanel(new GridLayout(1, 2, 14, 0)); columns.setOpaque(false);
        columns.add(categoryManagementCard(Transaction.Type.EXPENSE)); columns.add(categoryManagementCard(Transaction.Type.INCOME));
        page.add(columns, BorderLayout.CENTER); return page;
    }

    private JPanel categoryManagementCard(Transaction.Type type) {
        boolean expense = type == Transaction.Type.EXPENSE;
        String title = expense ? "Despesas" : "Receitas";
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 13)); card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(17, 18, 16, 18)));
        JPanel header = new JPanel(new BorderLayout(10, 0)); header.setOpaque(false);
        header.add(sectionHeading(title, expense ? "Onde o dinheiro sai" : "De onde o dinheiro vem"), BorderLayout.WEST);
        JButton add = primaryButton("+ Adicionar"); add.addActionListener(e -> addCategory(type)); header.add(add, BorderLayout.EAST);
        JPanel rows = new JPanel(); rows.setOpaque(false); rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        List<String> values = categoryStore.categories(type);
        for (String value : values) {
            JPanel row = new JPanel(new BorderLayout(9, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            JLabel name = new JLabel(value); name.setFont(new Font("Segoe UI", Font.PLAIN, 12)); name.setForeground(INK);
            JLabel badge = new JLabel(categoryStore.isDefault(type, value) ? "PADRÃO" : "PERSONALIZADA"); badge.setFont(new Font("Segoe UI", Font.BOLD, 8)); badge.setForeground(categoryStore.isDefault(type, value) ? MUTED : ACCENT); badge.setOpaque(true); badge.setBackground(CARD_ALT); badge.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0)); info.setOpaque(false); info.add(name); info.add(badge);
            JButton remove = secondaryButton("Remover"); remove.setFont(new Font("Segoe UI", Font.PLAIN, 10)); remove.setEnabled(values.size() > 1); remove.addActionListener(e -> removeCategory(type, value));
            row.add(info, BorderLayout.WEST); row.add(remove, BorderLayout.EAST); rows.add(row);
        }
        JScrollPane list = new JScrollPane(rows); list.setBorder(BorderFactory.createLineBorder(BORDER)); list.setBackground(CARD); list.getViewport().setBackground(CARD); list.getVerticalScrollBar().setUnitIncrement(16);
        card.add(header, BorderLayout.NORTH); card.add(list, BorderLayout.CENTER); return card;
    }

    private void addCategory(Transaction.Type type) {
        String kind = type == Transaction.Type.EXPENSE ? "despesa" : "receita";
        String name = JOptionPane.showInputDialog(this, "Nome da nova categoria de " + kind + ":", "Adicionar categoria", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        try { categoryStore.add(type, name); refreshPage(); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Não foi possível adicionar", JOptionPane.WARNING_MESSAGE); }
    }

    private void removeCategory(Transaction.Type type, String name) {
        long usage = transactions.stream().filter(t -> t.getType() == type && t.getCategory().equalsIgnoreCase(name)).count();
        String message = "Remover “" + name + "” da lista de categorias?\n" +
                (usage > 0 ? usage + " lançamento(s) antigo(s) continuarão no histórico.\n" : "") +
                "Os orçamentos já salvos não serão apagados.";
        int confirm = JOptionPane.showConfirmDialog(this, message, "Remover categoria", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try { categoryStore.remove(type, name); refreshPage(); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Não foi possível remover", JOptionPane.WARNING_MESSAGE); }
    }

    private void addTransactionDialog() { addTransactionDialog(null); }

    private void addTransactionDialog(Transaction editing) {
        boolean editMode = editing != null;
        JDialog dialog = new JDialog(this, editMode ? "Editar lançamento" : "Novo lançamento", true); dialog.setSize(500, 480); dialog.setLocationRelativeTo(this);
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 13)); form.setBorder(BorderFactory.createEmptyBorder(22, 26, 12, 26)); form.setBackground(CARD);
        LocalDate defaultDate = YearMonth.now().equals(selectedMonth) ? LocalDate.now() : selectedMonth.atDay(1);
        JTextField date = new JTextField((editMode ? editing.getDate() : defaultDate).format(DATE_BR));
        javax.swing.JComboBox<String> type = new ThemeComboBox<>(new String[]{"Despesa", "Receita"});
        type.setSelectedIndex(editMode && editing.getType() == Transaction.Type.INCOME ? 1 : 0);
        Transaction.Type initialType = type.getSelectedIndex() == 1 ? Transaction.Type.INCOME : Transaction.Type.EXPENSE;
        List<String> initialCategories = categoryStore.categories(initialType);
        javax.swing.JComboBox<String> category = new ThemeComboBox<>(initialCategories.toArray(String[]::new));
        category.setEditable(true);
        String initialCategory = editMode ? editing.getCategory()
                : initialType == Transaction.Type.EXPENSE && categoryStore.contains(initialType, "Restaurantes") ? "Restaurantes" : initialCategories.get(0);
        category.setSelectedItem(initialCategory);
        styleInput(date); styleInput(type); styleInput(category);
        type.addActionListener(e -> {
            Transaction.Type selectedType = type.getSelectedIndex() == 1 ? Transaction.Type.INCOME : Transaction.Type.EXPENSE;
            String[] choices = categoryStore.categories(selectedType).toArray(String[]::new);
            category.setModel(new DefaultComboBoxModel<>(choices)); category.setEditable(true); category.setSelectedItem(choices[0]);
            styleInput(category);
        });
        JTextField description = new JTextField(editMode ? editing.getDescription() : "");
        JTextField amount = new JTextField(editMode ? editing.getAmount().toPlainString() : "");
        styleInput(description); styleInput(amount);
        installCurrencyFormatting(amount);
        addField(form, "Data (dd/MM/aaaa)", date); addField(form, "Tipo", type); addField(form, "Categoria", category); addField(form, "Descrição (opcional)", description); addField(form, "Valor (R$)", amount);
        JLabel hintTitle = new JLabel("Dica de valor"); hintTitle.setForeground(MUTED); hintTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel hint = new JLabel("Digite 10000 → 10000,00"); hint.setForeground(ACCENT); hint.setFont(new Font("Segoe UI", Font.BOLD, 11));
        form.add(hintTitle); form.add(hint);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 9)); buttons.setBackground(CARD);
        JButton cancel = secondaryButton("Cancelar"); cancel.addActionListener(e -> dialog.dispose());
        JButton save = primaryButton(editMode ? "Salvar alterações" : "Salvar lançamento");
        save.addActionListener(e -> {
            try {
                LocalDate parsedDate = LocalDate.parse(date.getText().trim(), DATE_BR);
                BigDecimal parsedAmount = parseMoney(amount.getText());
                if (parsedAmount.signum() <= 0) throw new IllegalArgumentException("O valor deve ser maior que zero.");
                String categoryName = String.valueOf(category.getEditor().getItem()).trim();
                if (categoryName.isEmpty()) throw new IllegalArgumentException("Informe uma categoria.");
                Transaction.Type parsedType = type.getSelectedIndex() == 1 ? Transaction.Type.INCOME : Transaction.Type.EXPENSE;
                if (!categoryStore.contains(parsedType, categoryName)) categoryStore.add(parsedType, categoryName);
                Transaction updated = editMode
                        ? new Transaction(editing.getId(), parsedDate, parsedType, categoryName, description.getText(), parsedAmount)
                        : new Transaction(parsedDate, parsedType, categoryName, description.getText(), parsedAmount);
                if (editMode) {
                    int index = -1;
                    for (int i = 0; i < transactions.size(); i++) if (transactions.get(i).getId().equals(editing.getId())) { index = i; break; }
                    if (index < 0) throw new IllegalStateException("Este lançamento já não está disponível.");
                    transactions.set(index, updated);
                } else transactions.add(updated);
                persistTransactions(); dialog.dispose(); refreshPage();
            } catch (Exception ex) { JOptionPane.showMessageDialog(dialog, "Revise a data, a categoria e o valor. A descrição é opcional. Ex.: 22/10/2026 e 250,00.", "Confira os dados", JOptionPane.WARNING_MESSAGE); }
        });
        buttons.add(cancel); buttons.add(save);
        dialog.getContentPane().setBackground(CARD); dialog.setLayout(new BorderLayout()); dialog.add(form, BorderLayout.CENTER); dialog.add(buttons, BorderLayout.SOUTH); dialog.setVisible(true);
        SwingUtilities.invokeLater(amount::requestFocusInWindow);
    }

    private void installCurrencyFormatting(JTextField field) {
        field.setToolTipText("Digite 10000; ao sair do campo, o valor ficará como 10000,00.");
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent event) {
                String text = field.getText().trim();
                if (text.isEmpty()) return;
                try { field.setText(normalizeCurrencyInput(text)); }
                catch (NumberFormatException ignored) { }
            }
        });
    }

    private void addField(JPanel form, String label, Component field) {
        JLabel l = new JLabel(label); l.setFont(new Font("Segoe UI", Font.PLAIN, 12)); l.setForeground(MUTED); form.add(l); form.add(field);
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Importar lançamentos CSV");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            List<Transaction> imported = CsvStore.load(chooser.getSelectedFile().toPath());
            for (Transaction transaction : imported) if (!categoryStore.contains(transaction.getType(), transaction.getCategory())) categoryStore.add(transaction.getType(), transaction.getCategory());
            transactions.addAll(imported); persistTransactions(); refreshPage();
            JOptionPane.showMessageDialog(this, imported.size() + " lançamento(s) importado(s).\nOs registros foram adicionados aos dados existentes.");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Não foi possível importar o CSV:\n" + ex.getMessage(), "Erro na importação", JOptionPane.ERROR_MESSAGE); }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Exportar todos os lançamentos"); chooser.setSelectedFile(new java.io.File("orcabem-lancamentos.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) file = file.resolveSibling(file.getFileName() + ".csv");
        try { Files.writeString(file, CsvStore.exportText(transactions), StandardCharsets.UTF_8); JOptionPane.showMessageDialog(this, "CSV exportado com " + transactions.size() + " lançamento(s).\n" + file); }
        catch (IOException ex) { JOptionPane.showMessageDialog(this, "Falha ao salvar o CSV: " + ex.getMessage(), "Erro na exportação", JOptionPane.ERROR_MESSAGE); }
    }

    private void persistTransactions() {
        try { CsvStore.save(transactionsFile, transactions); }
        catch (IOException ex) { JOptionPane.showMessageDialog(this, "Não foi possível salvar os dados locais: " + ex.getMessage(), "Erro ao salvar", JOptionPane.ERROR_MESSAGE); }
    }

    private BigDecimal total(YearMonth month, Transaction.Type type) {
        return transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(month) && t.getType() == type).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> expensesByCategory(YearMonth month) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(month) && t.getType() == Transaction.Type.EXPENSE)
                .forEach(t -> map.merge(t.getCategory(), t.getAmount(), BigDecimal::add));
        return map;
    }

    private JPanel sectionHeading(String title, String subtitle) {
        JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(title); h.setFont(new Font("Segoe UI", Font.BOLD, 14)); h.setForeground(INK);
        JLabel s = new JLabel(subtitle); s.setFont(new Font("Segoe UI", Font.PLAIN, 10)); s.setForeground(MUTED);
        panel.add(h); panel.add(Box.createVerticalStrut(4)); panel.add(s); return panel;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                super.paintComponent(graphics);
                java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(CARD); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.setColor(BORDER); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.dispose();
            }
        };
        panel.setOpaque(false); panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); return panel;
    }

    private JLabel emptyMessage(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER); label.setForeground(MUTED); label.setFont(new Font("Segoe UI", Font.PLAIN, 11)); label.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8)); return label;
    }

    private JButton primaryButton(String text) { return styleButton(text, ACCENT, new Color(30, 35, 28), new Color(255, 207, 131)); }
    private JButton secondaryButton(String text) { return styleButton(text, CARD_ALT, INK, new Color(39, 72, 59)); }
    private JButton smallButton(String text) { JButton b = styleButton(text, CARD_ALT, INK, new Color(39, 72, 59)); b.setPreferredSize(new Dimension(37, 37)); b.setFont(new Font("Segoe UI", Font.BOLD, 18)); return b; }
    private JButton styleButton(String text, Color bg, Color fg, Color hover) {
        JButton b = new ThemeButton(text, bg, hover, BORDER); b.setFont(new Font("Segoe UI", Font.BOLD, 11)); b.setForeground(fg); b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12)); b.setMargin(new Insets(0, 0, 0, 0));
        return b;
    }

    private static void styleInput(javax.swing.JComponent input) {
        input.setBackground(CARD_ALT); input.setForeground(INK);
        if (input instanceof javax.swing.JComboBox<?> combo) {
            combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton arrow = new ThemeButton("▾", CARD_ALT, new Color(39, 72, 59), BORDER);
                    arrow.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12)); arrow.setPreferredSize(new Dimension(34, 28));
                    arrow.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3)); return arrow;
                }
            });
            combo.setOpaque(false); combo.setBackground(CARD_ALT); combo.setForeground(INK); combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focused) {
                    JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                    cell.setOpaque(true); cell.setFont(new Font("Segoe UI", Font.PLAIN, 12)); cell.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
                    cell.setBackground(selected ? new Color(39, 72, 59) : CARD_ALT); cell.setForeground(selected ? ACCENT : INK);
                    return cell;
                }
            });
            if (combo.isEditable() && combo.getEditor().getEditorComponent() instanceof javax.swing.text.JTextComponent editor) {
                editor.setOpaque(false); editor.setBackground(CARD_ALT); editor.setForeground(INK); editor.setCaretColor(GREEN); editor.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            }
        }
        input.setBorder(BorderFactory.createCompoundBorder(new javax.swing.border.LineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        if (input instanceof javax.swing.text.JTextComponent text) text.setCaretColor(GREEN);
    }

    private String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(PT); format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(2); return format.format(amount);
    }

    private String monthName(YearMonth month) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, PT);
        return name.substring(0, 1).toUpperCase(PT) + name.substring(1) + " de " + month.getYear();
    }

    private String percent(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) return "0%";
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    static String normalizeCurrencyInput(String value) {
        return parseMoney(value).setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static BigDecimal parseMoney(String value) {
        String cleaned = value.trim().replace("R$", "").replace(" ", "");
        if (cleaned.contains(",")) cleaned = cleaned.replace(".", "").replace(',', '.');
        return new BigDecimal(cleaned);
    }

    private static final class ThemeComboBox<E> extends javax.swing.JComboBox<E> {
        ThemeComboBox(E[] items) { super(items); }

        @Override protected void paintComponent(java.awt.Graphics raw) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) raw.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CARD_ALT); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
            if (!isEditable() && getSelectedItem() != null) {
                g.setFont(getFont()); g.setColor(INK);
                java.awt.FontMetrics metrics = g.getFontMetrics(); String label = String.valueOf(getSelectedItem());
                int maxWidth = Math.max(0, getWidth() - 48);
                while (!label.isEmpty() && metrics.stringWidth(label) > maxWidth) label = label.substring(0, label.length() - 1);
                if (!label.equals(String.valueOf(getSelectedItem())) && label.length() > 3) label = label.substring(0, label.length() - 3) + "…";
                g.drawString(label, 10, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            }
            g.dispose();
        }
    }

    private static final class ThemeButton extends JButton {
        private final Color hoverColor;
        private final Color outlineColor;

        ThemeButton(String text, Color background, Color hoverColor, Color outlineColor) {
            super(text); this.hoverColor = hoverColor; this.outlineColor = outlineColor;
            setBackground(background); setContentAreaFilled(false); setBorderPainted(false); setOpaque(false); setFocusPainted(false);
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
        }

        @Override public Color getForeground() {
            return isEnabled() ? super.getForeground() : new Color(166, 183, 172);
        }

        @Override protected void paintComponent(java.awt.Graphics raw) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) raw.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = !isEnabled() ? new Color(25, 43, 38) : getModel().isPressed() ? getBackground().darker() : getModel().isRollover() ? hoverColor : getBackground();
            g.setColor(fill); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.setColor(outlineColor); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.dispose();
            super.paintComponent(raw);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> {
            try { new OrcaBemApp().setVisible(true); }
            catch (IOException ex) { JOptionPane.showMessageDialog(null, "Não foi possível carregar os dados locais: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE); }
        });
    }
}
