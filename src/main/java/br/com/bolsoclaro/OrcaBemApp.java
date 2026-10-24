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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public final class OrcaBemApp extends JFrame {
    private static Color INK = new Color(242, 246, 241);
    private static Color MUTED = new Color(161, 181, 171);
    private static Color GREEN = new Color(100, 218, 169);
    private static Color ACCENT = new Color(100, 218, 169);
    private static Color ACCENT_TEXT = new Color(19, 38, 31);
    private static Color GREEN_LIGHT = new Color(37, 72, 59);
    private static Color ACCENT_HOVER = new Color(125, 230, 186);
    private static Color CANVAS = new Color(12, 28, 26);
    private static Color CARD = new Color(20, 42, 38);
    private static Color CARD_ALT = new Color(28, 55, 49);
    private static Color BORDER = new Color(48, 78, 67);
    private static Color RED = new Color(255, 140, 126);
    private static Color PURPLE = new Color(185, 163, 232);
    private static Color SIDEBAR = new Color(8, 22, 20);
    private static final Preferences THEME_PREFERENCES = Preferences.userNodeForPackage(OrcaBemApp.class);
    private static AppTheme selectedTheme = AppTheme.fromKey(THEME_PREFERENCES.get("theme", "VERDE"));
    static { applyPalette(paletteFor(selectedTheme)); }
    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    // Incrementar antes de gerar o executável de cada versão publicada.
    private static final String APP_VERSION = "1.0.10";
    private static final DateTimeFormatter DATE_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Path dataDir = Path.of(System.getProperty("orcabem.data.dir", Path.of(System.getProperty("user.home"), ".bolso-claro").toString()));
    private Path activeDataDir;
    private Path transactionsFile;
    private final List<Transaction> transactions = new ArrayList<>();
    private final ProfileStore profileStore;
    private ProfileStore.Profile activeProfile;
    private BudgetStore budgetStore;
    private CategoryStore categoryStore;
    private SavingsGoalStore savingsGoalStore;
    private InvestmentStore investmentStore;
    private javax.swing.JComboBox<ProfileStore.Profile> profileChooser;
    private boolean suppressProfileSelection;
    enum ChartRange {
        CURRENT("Atual", 1), THREE_MONTHS("3 meses", 3), SIX_MONTHS("6 meses", 6), ALL_TIME("Todo", 0);
        final String label;
        final int monthCount;
        ChartRange(String label, int monthCount) { this.label = label; this.monthCount = monthCount; }
    }

    private enum AppTheme {
        BRANCO("Branco", new Palette(new Color(35, 51, 43), new Color(91, 108, 98), new Color(45, 139, 87),
                new Color(31, 122, 74), Color.WHITE, new Color(213, 238, 222), new Color(45, 151, 89),
                new Color(244, 248, 245), Color.WHITE, new Color(232, 240, 235), new Color(204, 218, 209),
                new Color(213, 238, 222), new Color(196, 67, 79), new Color(117, 83, 158), new Color(233, 240, 235))),
        VERDE("Verde (padrão)", new Palette(new Color(242, 246, 241), new Color(161, 181, 171), new Color(100, 218, 169),
                new Color(100, 218, 169), new Color(19, 38, 31), new Color(37, 72, 59), new Color(125, 230, 186),
                new Color(12, 28, 26), new Color(20, 42, 38), new Color(28, 55, 49), new Color(48, 78, 67),
                new Color(37, 72, 59), new Color(255, 140, 126), new Color(185, 163, 232), new Color(8, 22, 20))),
        VERMELHO("Vermelho", new Palette(new Color(246, 239, 239), new Color(184, 159, 162), new Color(100, 218, 169),
                new Color(255, 125, 125), new Color(51, 22, 27), new Color(83, 43, 51), new Color(255, 157, 153),
                new Color(29, 17, 20), new Color(43, 25, 30), new Color(57, 34, 41), new Color(91, 54, 63),
                new Color(83, 43, 51), new Color(255, 140, 126), new Color(205, 161, 224), new Color(20, 12, 15))),
        ROSA("Rosa", new Palette(new Color(247, 239, 246), new Color(185, 162, 182), new Color(100, 218, 169),
                new Color(244, 139, 194), new Color(54, 26, 45), new Color(79, 43, 68), new Color(249, 165, 211),
                new Color(28, 17, 27), new Color(42, 25, 40), new Color(56, 34, 54), new Color(87, 53, 83),
                new Color(79, 43, 68), new Color(255, 140, 150), new Color(207, 166, 238), new Color(19, 12, 19))),
        AZUL("Azul", new Palette(new Color(239, 244, 250), new Color(158, 177, 198), new Color(100, 218, 169),
                new Color(119, 183, 247), new Color(17, 39, 61), new Color(42, 68, 98), new Color(147, 200, 255),
                new Color(13, 24, 39), new Color(20, 36, 57), new Color(29, 49, 74), new Color(48, 75, 105),
                new Color(42, 68, 98), new Color(255, 140, 126), new Color(184, 170, 242), new Color(9, 17, 29))),
        PERSONALIZADA("Personalizada", null);

        final String label;
        final Palette palette;
        AppTheme(String label, Palette palette) { this.label = label; this.palette = palette; }
        static AppTheme fromKey(String key) {
            try { return valueOf(key); }
            catch (IllegalArgumentException | NullPointerException ignored) { return VERDE; }
        }
        @Override public String toString() { return label; }
    }

    private record Palette(Color ink, Color muted, Color green, Color accent, Color accentText, Color selection,
                           Color accentHover, Color canvas, Color card, Color cardAlt, Color border, Color greenLight,
                           Color red, Color purple, Color sidebar) { }

    private static void applyPalette(Palette palette) {
        INK = palette.ink(); MUTED = palette.muted(); GREEN = palette.green(); ACCENT = palette.accent();
        ACCENT_TEXT = palette.accentText(); GREEN_LIGHT = palette.greenLight(); ACCENT_HOVER = palette.accentHover();
        CANVAS = palette.canvas(); CARD = palette.card(); CARD_ALT = palette.cardAlt(); BORDER = palette.border();
        RED = palette.red(); PURPLE = palette.purple(); SIDEBAR = palette.sidebar();
    }

    private static Palette paletteFor(AppTheme theme) {
        if (theme != AppTheme.PERSONALIZADA) return theme.palette;
        Color custom = new Color(THEME_PREFERENCES.getInt("customAccentRgb", AppTheme.VERDE.palette.accent().getRGB()), true);
        return paletteWithAccent(AppTheme.VERDE.palette, custom);
    }

    private static Palette paletteWithAccent(Palette base, Color accent) {
        double luminance = relativeLuminance(accent);
        Color text = luminance > 0.18 ? new Color(19, 38, 31) : Color.WHITE;
        Color hover = blend(accent, luminance < 0.5 ? Color.WHITE : Color.BLACK, 0.16);
        Color selection = blend(base.canvas(), accent, luminance > 0.72 ? 0.18 : 0.34);
        return new Palette(base.ink(), base.muted(), base.green(), accent, text, selection, hover,
                base.canvas(), base.card(), base.cardAlt(), base.border(), selection, base.red(), base.purple(), base.sidebar());
    }

    private static Color blend(Color first, Color second, double secondWeight) {
        double firstWeight = 1.0 - secondWeight;
        return new Color((int)Math.round(first.getRed() * firstWeight + second.getRed() * secondWeight),
                (int)Math.round(first.getGreen() * firstWeight + second.getGreen() * secondWeight),
                (int)Math.round(first.getBlue() * firstWeight + second.getBlue() * secondWeight));
    }

    private static double relativeLuminance(Color color) {
        return 0.2126 * linearChannel(color.getRed()) + 0.7152 * linearChannel(color.getGreen()) + 0.0722 * linearChannel(color.getBlue());
    }

    private static double linearChannel(int component) {
        double value = component / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private ChartRange cashFlowRange = ChartRange.SIX_MONTHS;
    private ChartRange investmentRange = ChartRange.ALL_TIME;
    private YearMonth selectedMonth = YearMonth.now();
    private String currentPage = "dashboard";
    private final JPanel pageHost = new JPanel(new CardLayout());
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel eyebrowLabel = new JLabel();
    private final JLabel pageTitle = new JLabel();
    private final JPanel sidebar = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private javax.swing.Timer updateCheckTimer;
    private boolean automaticUpdateCheckRunning;
    private final java.util.Set<String> announcedUpdateVersions = new java.util.HashSet<>();

    public OrcaBemApp() throws IOException {
        super("OrçaBem — seu dinheiro em foco");
        profileStore = new ProfileStore(dataDir);
        loadProfile(profileStore.activeProfile());
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

    private void loadProfile(ProfileStore.Profile profile) throws IOException {
        Path profileDirectory = profileStore.directory(profile);
        Path profileTransactions = profileDirectory.resolve("transacoes.csv");
        List<Transaction> loadedTransactions = CsvStore.load(profileTransactions);
        BudgetStore loadedBudgets = new BudgetStore(profileDirectory.resolve("orcamentos.properties"));
        CategoryStore loadedCategories = new CategoryStore(profileDirectory.resolve("categorias.properties"));
        SavingsGoalStore loadedGoals = new SavingsGoalStore(profileDirectory.resolve("metas.properties"));
        InvestmentStore loadedInvestments = new InvestmentStore(profileDirectory.resolve("investimentos.properties"));
        activeProfile = profile;
        activeDataDir = profileDirectory;
        transactionsFile = profileTransactions;
        transactions.clear(); transactions.addAll(loadedTransactions);
        budgetStore = loadedBudgets; categoryStore = loadedCategories; savingsGoalStore = loadedGoals; investmentStore = loadedInvestments;
    }

    private void activateProfile(ProfileStore.Profile profile) {
        ProfileStore.Profile previous = activeProfile;
        try {
            profileStore.setActive(profile.id());
            loadProfile(profile);
            selectedMonth = YearMonth.now(); currentPage = "dashboard";
            refreshProfileChooser(); refreshPage();
        } catch (Exception ex) {
            if (previous != null) {
                try { profileStore.setActive(previous.id()); } catch (IOException ignored) { }
                profileChooser.setSelectedItem(previous);
            }
            JOptionPane.showMessageDialog(this, "Não foi possível abrir esse perfil: " + ex.getMessage(), "Perfil indisponível", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createProfileDialog() {
        String name = JOptionPane.showInputDialog(this, "Nome para identificar os dados deste perfil:", "Criar perfil", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        try {
            ProfileStore.Profile profile = profileStore.create(name);
            activateProfile(profile);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Não foi possível criar o perfil", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void manageProfilesDialog() {
        JDialog dialog = new JDialog(this, "Gerenciar perfis", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 14)); root.setBackground(CANVAS);
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        JLabel title = new JLabel("Perfis neste computador"); title.setFont(new Font("Segoe UI", Font.BOLD, 18)); title.setForeground(INK);
        JLabel description = new JLabel("Escolha um perfil inativo para removê-lo. A exclusão dos dados não pode ser desfeita.");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 11)); description.setForeground(MUTED);
        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title); heading.add(Box.createVerticalStrut(5)); heading.add(description);

        JPanel card = new JPanel(new BorderLayout(0, 8)); card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        JLabel chooserLabel = new JLabel("Perfil"); chooserLabel.setFont(new Font("Segoe UI", Font.BOLD, 11)); chooserLabel.setForeground(MUTED);
        ThemeComboBox<ProfileStore.Profile> chooser = new ThemeComboBox<>(profileStore.profiles().toArray(ProfileStore.Profile[]::new));
        chooser.setSelectedItem(activeProfile); chooser.setMaximumRowCount(Math.min(profileStore.profiles().size(), 12));
        styleInput(chooser); chooser.setPreferredSize(new Dimension(420, 40));
        chooser.setRenderer((list, value, index, selected, focus) -> {
            JLabel item = new JLabel(value == null ? "" : value.name() + (value.id().equals(activeProfile.id()) ? "  ·  em uso" : ""));
            item.setOpaque(true); item.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
            item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            item.setForeground(selected ? INK : MUTED); item.setBackground(selected ? CARD_ALT : CARD);
            return item;
        });
        JPanel chooserField = new JPanel(new BorderLayout(0, 6)); chooserField.setOpaque(false);
        chooserField.add(chooserLabel, BorderLayout.NORTH); chooserField.add(chooser, BorderLayout.CENTER);
        JLabel status = new JLabel(" "); status.setFont(new Font("Segoe UI", Font.PLAIN, 11)); status.setForeground(MUTED);
        card.add(chooserField, BorderLayout.NORTH); card.add(status, BorderLayout.SOUTH);

        JButton delete = secondaryButton("Excluir perfil"); JButton close = secondaryButton("Fechar");
        Runnable updateDeleteState = () -> {
            ProfileStore.Profile selected = (ProfileStore.Profile) chooser.getSelectedItem();
            boolean personal = selected != null && "default".equals(selected.id());
            boolean active = selected != null && selected.id().equals(activeProfile.id());
            delete.setEnabled(selected != null && !personal && !active);
            if (personal) { status.setText("O perfil Pessoal mantém os dados legados e não pode ser excluído."); status.setForeground(MUTED); }
            else if (active) { status.setText("Troque para outro perfil antes de excluir o perfil que está em uso."); status.setForeground(MUTED); }
            else { status.setText("Movimentações, orçamentos, categorias, metas e investimentos deste perfil serão apagados."); status.setForeground(MUTED); }
        };
        chooser.addActionListener(event -> updateDeleteState.run());
        delete.addActionListener(event -> {
            ProfileStore.Profile selected = (ProfileStore.Profile) chooser.getSelectedItem();
            if (selected == null || !delete.isEnabled()) return;
            int choice = JOptionPane.showOptionDialog(dialog,
                    "Excluir o perfil ‘" + selected.name() + "’ e todos os dados guardados nele?\n\nEssa ação é permanente e não pode ser desfeita.",
                    "Confirmar exclusão", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new Object[]{"Cancelar", "Excluir perfil"}, "Cancelar");
            if (choice != 1) return;
            try {
                boolean cleaned = profileStore.delete(selected);
                refreshProfileChooser();
                chooser.setModel(new DefaultComboBoxModel<>(profileStore.profiles().toArray(ProfileStore.Profile[]::new)));
                ProfileStore.Profile nextSelection = profileStore.profiles().stream()
                        .filter(profile -> !"default".equals(profile.id()) && !profile.id().equals(activeProfile.id()))
                        .findFirst().orElse(activeProfile);
                chooser.setSelectedItem(nextSelection);
                updateDeleteState.run();
                if (cleaned) { status.setText("Perfil e dados excluídos."); status.setForeground(GREEN); }
                else { status.setText("Perfil removido da lista, mas alguns arquivos temporários ficaram na pasta local. Verifique a pasta de dados antes de liberar espaço."); status.setForeground(ACCENT); }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Não foi possível excluir o perfil: " + ex.getMessage(), "Exclusão não concluída", JOptionPane.ERROR_MESSAGE);
            }
        });
        close.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        actions.add(close); actions.add(delete);
        root.add(heading, BorderLayout.NORTH); root.add(card, BorderLayout.CENTER); root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root); dialog.setSize(660, 360); dialog.setMinimumSize(new Dimension(640, 340)); dialog.setResizable(false); dialog.setLocationRelativeTo(this);
        updateDeleteState.run(); dialog.setVisible(true);
    }

    private void refreshProfileChooser() {
        if (profileChooser == null) return;
        suppressProfileSelection = true;
        profileChooser.setModel(new DefaultComboBoxModel<>(profileStore.profiles().toArray(ProfileStore.Profile[]::new)));
        profileChooser.setSelectedItem(activeProfile);
        suppressProfileSelection = false;
    }

    private JPanel buildSidebar() {
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(248, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 16, 18, 16));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); brand.setOpaque(false); brand.setMaximumSize(new Dimension(205, 52));
        JLabel mark = new JLabel(new javax.swing.ImageIcon(AppIcon.render(43)));
        JPanel brandCopy = new JPanel(); brandCopy.setOpaque(false); brandCopy.setLayout(new BoxLayout(brandCopy, BoxLayout.Y_AXIS)); brandCopy.setBorder(BorderFactory.createEmptyBorder(5, 11, 0, 0));
        JLabel brandName = new JLabel("OrçaBem"); brandName.setFont(new Font("Segoe UI", Font.BOLD, 15)); brandName.setForeground(INK);
        JLabel brandTagline = new JLabel("SEU DINHEIRO, BEM PLANEJADO"); brandTagline.setFont(new Font("Segoe UI", Font.BOLD, 8)); brandTagline.setForeground(ACCENT);
        brandCopy.add(brandName); brandCopy.add(Box.createVerticalStrut(3)); brandCopy.add(brandTagline); brand.add(mark); brand.add(brandCopy);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT); sidebar.add(brand); sidebar.add(Box.createVerticalStrut(22));
        JLabel profileLabel = new JLabel("PERFIL ATIVO"); profileLabel.setFont(new Font("Segoe UI", Font.BOLD, 9)); profileLabel.setForeground(MUTED);
        profileLabel.setAlignmentX(Component.LEFT_ALIGNMENT); sidebar.add(profileLabel); sidebar.add(Box.createVerticalStrut(6));
        profileChooser = new ThemeComboBox<>(profileStore.profiles().toArray(ProfileStore.Profile[]::new));
        profileChooser.setSelectedItem(activeProfile); profileChooser.setPreferredSize(new Dimension(216, 38)); profileChooser.setMaximumSize(new Dimension(216, 38));
        profileChooser.setAlignmentX(Component.LEFT_ALIGNMENT); styleInput(profileChooser);
        profileChooser.addActionListener(e -> {
            Object selected = profileChooser.getSelectedItem();
            if (!suppressProfileSelection && selected instanceof ProfileStore.Profile profile && activeProfile != null && !profile.id().equals(activeProfile.id())) activateProfile(profile);
        });
        JButton newProfile = secondaryButton("Criar perfil"); newProfile.setToolTipText("Criar perfil independente");
        newProfile.setPreferredSize(new Dimension(216, 36)); newProfile.setMaximumSize(new Dimension(216, 36)); newProfile.setAlignmentX(Component.LEFT_ALIGNMENT);
        newProfile.addActionListener(e -> createProfileDialog());
        JButton manageProfiles = secondaryButton("Gerenciar perfis"); manageProfiles.setToolTipText("Gerenciar e excluir perfis existentes");
        manageProfiles.setPreferredSize(new Dimension(216, 36)); manageProfiles.setMaximumSize(new Dimension(216, 36)); manageProfiles.setAlignmentX(Component.LEFT_ALIGNMENT);
        manageProfiles.addActionListener(e -> manageProfilesDialog());
        sidebar.add(profileChooser); sidebar.add(Box.createVerticalStrut(7));
        sidebar.add(newProfile); sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(manageProfiles); sidebar.add(Box.createVerticalStrut(22));
        addNav("dashboard", "Visão geral");
        addNav("transactions", "Lançamentos");
        addNav("budgets", "Orçamentos");
        addNav("investments", "Investimentos");
        addNav("categories", "Categorias");
        addNav("insights", "Insights");
        addNav("privacy", "Dados e privacidade");
        sidebar.add(Box.createVerticalGlue());
        JLabel privateMark = new JLabel("•", SwingConstants.CENTER); privateMark.setForeground(GREEN); privateMark.setFont(new Font("Segoe UI", Font.BOLD, 26));
        privateMark.setToolTipText("Privado por design · guardado neste computador"); privateMark.setAlignmentX(Component.CENTER_ALIGNMENT); sidebar.add(privateMark);
        return sidebar;
    }

    private void addNav(String key, String accessibleName) {
        JButton button = new ThemeButton(accessibleName, SIDEBAR, GREEN_LIGHT, SIDEBAR);
        button.setIcon(new SidebarIcon(key)); button.setIconTextGap(12);
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
        JButton updateButton = secondaryButton(updateButtonLabel());
        updateButton.setToolTipText(isUpdateSimulation() ? "Simulação local: não consulta o GitHub nem usa um executável real." : "Consultar a versão mais recente publicada em Releases no GitHub.");
        updateButton.addActionListener(e -> checkForUpdates(updateButton));
        JButton quickAdd = primaryButton("+ Novo lançamento"); quickAdd.setToolTipText("Registrar uma receita ou despesa"); quickAdd.addActionListener(e -> addTransactionDialog());
        headerActions.add(monthPicker); headerActions.add(updateButton); headerActions.add(quickAdd);
        header.add(titles, BorderLayout.WEST); header.add(headerActions, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        pageHost.setOpaque(false); pageHost.setBorder(BorderFactory.createEmptyBorder(0, 30, 28, 30));
        root.add(pageHost, BorderLayout.CENTER);
        return root;
    }

    private void checkForUpdates(JButton button) {
        button.setEnabled(false); button.setText("Verificando…");
        new javax.swing.SwingWorker<java.util.Optional<UpdateService.Release>, Void>() {
            @Override protected java.util.Optional<UpdateService.Release> doInBackground() throws Exception {
                return UpdateService.fetchLatest();
            }

            @Override protected void done() {
                button.setEnabled(true); button.setText(updateButtonLabel());
                try {
                    java.util.Optional<UpdateService.Release> result = get();
                    if (result.isEmpty()) {
                        JOptionPane.showMessageDialog(OrcaBemApp.this,
                                "Ainda não há uma versão publicada na aba Releases do GitHub.\n\nQuando uma Release for criada, o OrçaBem poderá verificar se ela é mais recente que a versão " + APP_VERSION + ".",
                                "Nenhuma versão publicada", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    UpdateService.Release release = result.get();
                    int comparison;
                    try { comparison = UpdateService.compareVersions(release.tag(), APP_VERSION); }
                    catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(OrcaBemApp.this,
                                "A Release mais recente usa uma tag incompatível. Publique versões estáveis no formato v1.2.3.",
                                "Formato de versão inválido", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (comparison <= 0) {
                        String message = comparison == 0
                                ? "Você já está usando a versão mais recente (" + APP_VERSION + ")."
                                : "Esta instalação (" + APP_VERSION + ") é mais nova que a última Release pública (" + release.tag() + ").";
                        JOptionPane.showMessageDialog(OrcaBemApp.this, message, "OrçaBem atualizado", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if (release.assetUri() == null || release.assetName() == null) {
                        JOptionPane.showMessageDialog(OrcaBemApp.this,
                                "A versão " + release.tag() + " foi publicada, mas não contém o arquivo OrcaBem.exe.\n\nAnexe o executável aos arquivos da Release para habilitar o download.",
                                "Arquivo da atualização ausente", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (!release.hasVerifiedWindowsAsset()) {
                        JOptionPane.showMessageDialog(OrcaBemApp.this,
                                "O GitHub não forneceu uma conferência de integridade válida para OrcaBem.exe. O download foi bloqueado para proteger o aplicativo.",
                                "Não foi possível validar a atualização", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    showDownloadConfirmationDialog(release);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(OrcaBemApp.this, "A verificação foi interrompida.", "Verificação de atualizações", JOptionPane.WARNING_MESSAGE);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
                    JOptionPane.showMessageDialog(OrcaBemApp.this,
                            "Não foi possível consultar as Releases do GitHub.\n" + detail,
                            "Falha na verificação", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    static boolean isNewerVersionAvailable(String releaseTag) {
        try { return UpdateService.compareVersions(releaseTag, APP_VERSION) > 0; }
        catch (IllegalArgumentException ex) { return false; }
    }

    private void startAutomaticUpdateChecks() {
        if (isUpdateSimulation()) return;
        checkForUpdatesAutomatically();
        updateCheckTimer = new javax.swing.Timer(6 * 60 * 60 * 1000, event -> checkForUpdatesAutomatically());
        updateCheckTimer.setRepeats(true);
        updateCheckTimer.start();
    }

    private void checkForUpdatesAutomatically() {
        if (automaticUpdateCheckRunning || !isDisplayable()) return;
        automaticUpdateCheckRunning = true;
        new javax.swing.SwingWorker<java.util.Optional<UpdateService.Release>, Void>() {
            @Override protected java.util.Optional<UpdateService.Release> doInBackground() throws Exception {
                return UpdateService.fetchLatest();
            }

            @Override protected void done() {
                automaticUpdateCheckRunning = false;
                if (!isDisplayable()) return;
                try {
                    java.util.Optional<UpdateService.Release> result = get();
                    if (result.isEmpty()) return;
                    UpdateService.Release release = result.get();
                    if (!isNewerVersionAvailable(release.tag()) || !announcedUpdateVersions.add(release.tag())) return;
                    if (!release.hasVerifiedWindowsAsset()) {
                        String issue = release.assetUri() == null || release.assetName() == null
                                ? "A Release não contém o arquivo OrcaBem.exe."
                                : "Não foi possível validar a integridade do OrcaBem.exe; o download foi bloqueado por segurança.";
                        JOptionPane.showMessageDialog(OrcaBemApp.this,
                                "Uma nova versão (" + release.tag() + ") está disponível, mas ainda não pode ser instalada.\n" + issue,
                                "Atualização disponível", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    showDownloadConfirmationDialog(release);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException | IllegalArgumentException ignored) {
                    // Falhas de rede ou tags inválidas não bloqueiam o app; a verificação manual segue disponível.
                }
            }
        }.execute();
    }

    private static boolean isUpdateSimulation() {
        return Boolean.getBoolean("orcabem.update.simulation");
    }

    private static String updateButtonLabel() {
        return isUpdateSimulation() ? "Testar atualização simulada" : "Verificar atualização";
    }

    private static Path updateDownloadDirectory() {
        String configured = System.getProperty("orcabem.update.downloadDir");
        return configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "Downloads")
                : Path.of(configured);
    }

    private void showDownloadConfirmationDialog(UpdateService.Release release) {
        JDialog dialog = new JDialog(this, isUpdateSimulation() ? "Atualização simulada disponível" : "Atualização disponível", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 15)); root.setBackground(CANVAS);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));

        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel badge = new JLabel(isUpdateSimulation() ? "SIMULAÇÃO LOCAL · NÃO É UMA RELEASE REAL" : "NOVA VERSÃO NO GITHUB"); badge.setFont(new Font("Segoe UI", Font.BOLD, 10)); badge.setForeground(ACCENT);
        JLabel title = new JLabel("OrçaBem " + release.tag()); title.setFont(new Font("Segoe UI", Font.BOLD, 21)); title.setForeground(INK);
        JLabel comparison = new JLabel("Versão instalada " + APP_VERSION + "  →  versão disponível " + release.tag());
        comparison.setFont(new Font("Segoe UI", Font.PLAIN, 12)); comparison.setForeground(GREEN);
        heading.add(badge); heading.add(Box.createVerticalStrut(6)); heading.add(title); heading.add(Box.createVerticalStrut(5)); heading.add(comparison);
        root.add(heading, BorderLayout.NORTH);

        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 14, 16));
        JLabel notesTitle = new JLabel("Novidades desta versão"); notesTitle.setFont(new Font("Segoe UI", Font.BOLD, 13)); notesTitle.setForeground(INK);
        String notesText = release.notes().isBlank() ? "O autor não incluiu notas para esta versão." : release.notes().strip();
        if (notesText.length() > 3500) notesText = notesText.substring(0, 3500) + "\n…";
        JTextArea notes = new JTextArea(notesText); notes.setEditable(false); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        notes.setFont(new Font("Segoe UI", Font.PLAIN, 12)); notes.setForeground(INK); notes.setBackground(CARD_ALT);
        notes.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        JScrollPane notesScroll = new JScrollPane(notes); notesScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        notesScroll.getViewport().setBackground(CARD_ALT);
        JPanel noteArea = new JPanel(new BorderLayout(0, 8)); noteArea.setOpaque(false); noteArea.add(notesTitle, BorderLayout.NORTH); noteArea.add(notesScroll, BorderLayout.CENTER);

        String destination = updateDownloadDirectory().toAbsolutePath().toString();
        String initialStatus = isUpdateSimulation()
                ? "Teste isolado: o arquivo fictício será salvo em " + destination + ". Ele não é um executável real e não deve ser aberto."
                : "O arquivo será baixado para " + destination + " e conferido automaticamente. Depois, escolha Instalar e reiniciar para atualizar este app.";
        JTextArea status = new JTextArea(initialStatus);
        status.setEditable(false); status.setFocusable(false); status.setOpaque(false); status.setLineWrap(true); status.setWrapStyleWord(true);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 11)); status.setForeground(MUTED); status.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        javax.swing.JProgressBar progress = new javax.swing.JProgressBar(0, 100); progress.setStringPainted(true);
        progress.setString("Aguardando sua permissão"); progress.setVisible(false); progress.setForeground(ACCENT); progress.setBackground(CARD_ALT); progress.setBorderPainted(false);
        JPanel feedback = new JPanel(); feedback.setOpaque(false); feedback.setLayout(new BoxLayout(feedback, BoxLayout.Y_AXIS));
        feedback.add(status); feedback.add(Box.createVerticalStrut(8)); feedback.add(progress);
        card.add(noteArea, BorderLayout.CENTER); card.add(feedback, BorderLayout.SOUTH); root.add(card, BorderLayout.CENTER);

        JButton cancel = secondaryButton("Agora não"); JButton download = primaryButton("Baixar versão");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false); actions.add(cancel); actions.add(download); root.add(actions, BorderLayout.SOUTH);
        boolean[] downloading = {false}; boolean[] completed = {false};
        Path[] downloadedFile = {null}; Path[] installedExecutable = {UpdateInstaller.currentExecutable()};
        cancel.addActionListener(event -> { if (!downloading[0]) dialog.dispose(); });
        download.addActionListener(event -> {
            if (completed[0]) {
                if (isUpdateSimulation()) { dialog.dispose(); return; }
                if (installedExecutable[0] == null) {
                    try {
                        java.awt.Desktop.getDesktop().open(updateDownloadDirectory().toFile());
                        status.setText("Não consegui identificar o executável que está aberto. A nova versão continua salva em:\n" + downloadedFile[0] + "\n\nFeche este app e abra o arquivo baixado manualmente.");
                    } catch (Exception ex) {
                        status.setText("A nova versão está salva em:\n" + downloadedFile[0] + "\n\nFeche este app e abra o arquivo baixado manualmente.");
                    }
                    status.setForeground(GREEN); return;
                }
                try {
                    UpdateInstaller.startAfterExit(downloadedFile[0], installedExecutable[0], ProcessHandle.current().pid());
                    dialog.dispose();
                    OrcaBemApp.this.dispose();
                    System.exit(0);
                } catch (Exception ex) {
                    status.setText("Não consegui preparar a instalação automática: " + ex.getMessage() + "\n\nO arquivo continua salvo em:\n" + downloadedFile[0]);
                    status.setForeground(RED); download.setText("Tentar instalar novamente"); cancel.setText("Fechar");
                }
                return;
            }
            if (downloading[0]) return;
            downloading[0] = true; download.setEnabled(false); download.setText("Baixando…"); cancel.setEnabled(false);
            progress.setValue(0); progress.setString("Iniciando…"); progress.setVisible(true);
            status.setText(isUpdateSimulation()
                    ? "Baixando o arquivo fictício para teste. Ele não pode ser executado."
                    : "Baixando OrcaBem.exe. A verificação de integridade será feita antes de salvar o arquivo.");
            new javax.swing.SwingWorker<Path, Integer>() {
                @Override protected Path doInBackground() throws Exception {
                    return UpdateService.download(release, updateDownloadDirectory(), value -> publish(value));
                }

                @Override protected void process(List<Integer> chunks) {
                    if (chunks.isEmpty()) return;
                    int value = chunks.get(chunks.size() - 1); progress.setValue(value); progress.setString(value + "%");
                }

                @Override protected void done() {
                    downloading[0] = false;
                    try {
                        Path saved = get(); completed[0] = true; downloadedFile[0] = saved;
                        installedExecutable[0] = UpdateInstaller.currentExecutable();
                        progress.setValue(100); progress.setString(isUpdateSimulation() ? "Teste concluído" : "Pronto para instalar");
                        String nextStep;
                        if (isUpdateSimulation()) {
                            nextStep = "\n\nEste arquivo é fictício, não é um executável válido e NÃO deve ser aberto.";
                        } else if (installedExecutable[0] != null) {
                            nextStep = "\n\nClique em “Instalar e reiniciar” para substituir esta versão. O app será fechado e aberto novamente com a atualização.";
                        } else {
                            nextStep = "\n\nFeche o app e abra o executável baixado para concluir a atualização.";
                        }
                        status.setText("Atualização baixada e conferida. Arquivo salvo em:\n" + saved.toAbsolutePath() + nextStep); status.setForeground(GREEN);
                        if (isUpdateSimulation()) { download.setText("Teste concluído"); download.setEnabled(false); cancel.setText("Fechar"); }
                        else { download.setText(installedExecutable[0] == null ? "Abrir Downloads" : "Instalar e reiniciar"); download.setEnabled(true); cancel.setText(installedExecutable[0] == null ? "Fechar" : "Instalar depois"); }
                        cancel.setEnabled(true);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); downloadFailed("O download foi interrompido.");
                    } catch (java.util.concurrent.ExecutionException ex) {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
                        downloadFailed(detail);
                    }
                }

                private void downloadFailed(String message) {
                    status.setText("O download não foi concluído: " + message); status.setForeground(RED);
                    progress.setString("Não concluído"); download.setText("Tentar novamente"); download.setEnabled(true); cancel.setEnabled(true);
                }
            }.execute();
        });
        dialog.setContentPane(root); dialog.setSize(650, 500); dialog.setMinimumSize(new Dimension(600, 450));
        dialog.setResizable(false); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    private void refreshPage() {
        monthLabel.setText(monthName(selectedMonth));
        pageTitle.setText(switch (currentPage) { case "transactions" -> "Movimentações"; case "budgets" -> "Orçamentos"; case "investments" -> "Investimentos"; case "categories" -> "Categorias"; case "insights" -> "Insights"; case "privacy" -> "Dados e privacidade"; default -> "Seu dinheiro em foco."; });
        eyebrowLabel.setText("SEU RESUMO FINANCEIRO   /   " + monthName(selectedMonth).toUpperCase(PT));
        updateNavStyles();
        pageHost.removeAll();
        if (currentPage.equals("transactions")) pageHost.add(buildTransactionsPage());
        else if (currentPage.equals("budgets")) pageHost.add(buildBudgetsPage());
        else if (currentPage.equals("investments")) pageHost.add(buildInvestmentsPage());
        else if (currentPage.equals("categories")) pageHost.add(buildCategoriesPage());
        else if (currentPage.equals("insights")) pageHost.add(buildInsightsPage());
        else if (currentPage.equals("privacy")) pageHost.add(buildPrivacyPage());
        else pageHost.add(buildDashboardPage());
        pageHost.revalidate(); pageHost.repaint();
    }

    private void updateNavStyles() {
        navButtons.forEach((key, button) -> {
            boolean active = currentPage.equals(key);
            button.setBackground(active ? GREEN_LIGHT : SIDEBAR);
            button.setForeground(active ? INK : MUTED);
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
        stats.add(statCard("RECEITAS DISPONÍVEIS", money(income), "Entradas menos aportes financiados por receita", GREEN));
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
        JPanel fill = new JPanel(); fill.setBackground(ACCENT); int width = Math.max(4, (int)(290 * amount.doubleValue() / Math.max(1, totalSpent.doubleValue()))); fill.setPreferredSize(new Dimension(width, 6)); track.add(fill, BorderLayout.WEST);
        row.add(top, BorderLayout.NORTH); row.add(track, BorderLayout.SOUTH); return row;
    }

    private JPanel trendCard() {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 6)); card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(17, 18, 11, 14)));
        JPanel header = new JPanel(); header.setOpaque(false); header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(sectionHeading("Fluxo de caixa ao longo do tempo", chartPeriodDescription(cashFlowRange, selectedMonth)));
        JPanel controls = rangeControls(cashFlowRange, range -> { cashFlowRange = range; refreshPage(); });
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); controls.setAlignmentX(Component.LEFT_ALIGNMENT); header.add(controls);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 3)); legend.setOpaque(false);
        JLabel incomeLegend = new JLabel("● Receitas disponíveis"); incomeLegend.setForeground(ACCENT); incomeLegend.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JLabel expenseLegend = new JLabel("● Despesas"); expenseLegend.setForeground(PURPLE); expenseLegend.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        legend.add(incomeLegend); legend.add(expenseLegend); card.add(header, BorderLayout.NORTH);
        card.add(legend, BorderLayout.SOUTH);
        List<YearMonth> months = cashFlowMonths();
        TrendChart chart = new TrendChart(); chart.setThemeColors(ACCENT, PURPLE, MUTED, BORDER, CARD); chart.setCashFlowData(months, m -> total(m, Transaction.Type.INCOME), m -> total(m, Transaction.Type.EXPENSE));
        card.add(chart, BorderLayout.CENTER); return card;
    }

    private JPanel rangeControls(ChartRange selected, java.util.function.Consumer<ChartRange> onSelect) {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); controls.setOpaque(false);
        for (ChartRange range : ChartRange.values()) {
            JButton option = rangeButton(range.label, range == selected, () -> onSelect.accept(range));
            controls.add(option);
        }
        return controls;
    }

    private JButton rangeButton(String label, boolean selected, Runnable action) {
        JButton button = styleButton(label, selected ? ACCENT : CARD_ALT, selected ? ACCENT_TEXT : INK, selected ? ACCENT_HOVER : GREEN_LIGHT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 9)); button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        button.addActionListener(e -> action.run()); return button;
    }

    private String chartPeriodDescription(ChartRange range, YearMonth end) {
        return switch (range) {
            case CURRENT -> "Somente " + monthName(end);
            case THREE_MONTHS -> "Últimos 3 meses até " + monthName(end);
            case SIX_MONTHS -> "Últimos 6 meses até " + monthName(end);
            case ALL_TIME -> "Todo o histórico registrado";
        };
    }

    private List<YearMonth> cashFlowMonths() {
        List<YearMonth> recorded = transactions.stream().map(t -> YearMonth.from(t.getDate())).toList();
        return monthsForRange(cashFlowRange, selectedMonth, recorded);
    }

    private List<YearMonth> investmentMonths() {
        List<YearMonth> recorded = investmentStore.movements().stream().map(m -> YearMonth.from(m.date())).toList();
        return monthsForRange(investmentRange, YearMonth.now(), recorded);
    }

    static List<YearMonth> monthsForRange(ChartRange range, YearMonth end, List<YearMonth> recordedMonths) {
        if (range != ChartRange.ALL_TIME) return recentMonths(end, range.monthCount);
        if (recordedMonths.isEmpty()) return recentMonths(end, 6);
        YearMonth first = recordedMonths.stream().min(YearMonth::compareTo).orElse(end);
        YearMonth last = recordedMonths.stream().max(YearMonth::compareTo).orElse(end);
        if (end.isAfter(last)) last = end;
        return monthsBetween(first, last);
    }

    private static List<YearMonth> recentMonths(YearMonth end, int count) {
        List<YearMonth> months = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) months.add(end.minusMonths(i));
        return months;
    }

    private static List<YearMonth> monthsBetween(YearMonth first, YearMonth last) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth month = first;
        while (!month.isAfter(last) && months.size() < 2400) { months.add(month); month = month.plusMonths(1); }
        if (months.isEmpty()) return recentMonths(last, 6);
        return months;
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
        if (income.signum() > 0 && expense.compareTo(income) > 0) out.add("Suas despesas superam as receitas disponíveis após aportes em " + money(expense.subtract(income)) + " neste mês.");
        else if (income.signum() > 0 && expense.compareTo(income) <= 0) out.add("Depois das despesas, restaram " + percent(income.subtract(expense), income) + " das receitas disponíveis após aportes neste mês.");
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

    private JPanel buildInvestmentsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14)); page.setOpaque(false);
        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel intro = new JPanel(); intro.setOpaque(false); intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
        JLabel explanation = new JLabel("Registre aportes e resgates. O saldo considera apenas os valores que você informar, sem cotação de mercado.");
        explanation.setFont(new Font("Segoe UI", Font.PLAIN, 11)); explanation.setForeground(MUTED); explanation.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        JButton contribution = primaryButton("+ Registrar aporte"); contribution.addActionListener(e -> investmentMovementDialog(InvestmentStore.Type.APORTE));
        JButton withdrawal = secondaryButton("− Registrar resgate"); withdrawal.addActionListener(e -> investmentMovementDialog(InvestmentStore.Type.RESGATE));
        actions.add(contribution); actions.add(withdrawal); actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        intro.add(explanation); intro.add(Box.createVerticalStrut(8)); intro.add(actions);
        content.add(intro); content.add(Box.createVerticalStrut(13));

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0)); stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(800, 125)); stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        stats.add(statCard("SALDO INVESTIDO", money(investmentStore.balance()), "Aportes menos resgates registrados", GREEN));
        stats.add(statCard("APORTES ACUMULADOS", money(investmentStore.total(InvestmentStore.Type.APORTE)), "Acumulado aportado", ACCENT));
        stats.add(statCard("RESGATES ACUMULADOS", money(investmentStore.total(InvestmentStore.Type.RESGATE)), "Acumulado resgatado", PURPLE));
        content.add(stats); content.add(Box.createVerticalStrut(13));
        content.add(investmentChartCard()); content.add(Box.createVerticalStrut(13));
        content.add(investmentHistoryCard());
        JScrollPane scroll = new JScrollPane(content); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scroll, BorderLayout.CENTER); return page;
    }

    private JPanel investmentChartCard() {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 7));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(16, 18, 12, 18)));
        JPanel header = new JPanel(); header.setOpaque(false); header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(sectionHeading("Evolução do saldo investido", chartPeriodDescription(investmentRange, YearMonth.now())));
        JPanel controls = rangeControls(investmentRange, range -> { investmentRange = range; refreshPage(); });
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); controls.setAlignmentX(Component.LEFT_ALIGNMENT); header.add(controls);
        card.add(header, BorderLayout.NORTH);
        TrendChart chart = new TrendChart(); chart.setThemeColors(ACCENT, PURPLE, MUTED, BORDER, CARD);
        chart.setData(investmentMonths(), month -> investmentStore.balanceAt(month.atEndOfMonth()));
        card.add(chart, BorderLayout.CENTER);
        JLabel note = new JLabel("A visualização mostra o saldo calculado pelos aportes e resgates registrados; rentabilidade e preços de mercado não são estimados.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 10)); note.setForeground(MUTED); card.add(note, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(800, 330)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330)); return card;
    }

    private JPanel investmentHistoryCard() {
        JPanel card = cardPanel(); card.setLayout(new BorderLayout(0, 9));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(15, 18, 15, 18)));
        card.add(sectionHeading("Histórico de investimentos", "Movimentos deste perfil"), BorderLayout.NORTH);
        String[] columns = {"Data", "Movimento", "Origem", "Observação", "Valor", "Saldo após"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
        Map<String, BigDecimal> balancesAfter = investmentStore.balancesAfterEach();
        for (InvestmentStore.Movement movement : investmentStore.movements()) {
            String signed = (movement.type() == InvestmentStore.Type.APORTE ? "+ " : "− ") + money(movement.amount());
            model.addRow(new Object[]{movement.date().format(DATE_BR), movement.label(), movement.sourceLabel(), movement.note().isBlank() ? "—" : movement.note(), signed, money(balancesAfter.getOrDefault(movement.id(), BigDecimal.ZERO))});
        }
        JTable table = new JTable(model); table.setRowHeight(36); table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setForeground(INK); table.setBackground(CARD); table.setGridColor(BORDER); table.setShowVerticalLines(false); table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10)); table.getTableHeader().setForeground(INK); table.getTableHeader().setBackground(CARD_ALT); table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setDefaultRenderer((source, value, selected, focused, row, column) -> {
            JLabel cell = new JLabel("  " + String.valueOf(value)); cell.setOpaque(true); cell.setBackground(CARD_ALT); cell.setForeground(INK); cell.setFont(new Font("Segoe UI", Font.BOLD, 10));
            cell.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER)); return cell;
        });
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(BORDER)); scroll.setBackground(CARD); scroll.getViewport().setBackground(CARD);
        card.add(scroll, BorderLayout.CENTER);
        if (model.getRowCount() == 0) card.add(emptyMessage("Nenhum aporte ou resgate registrado neste perfil."), BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(800, 280)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340)); return card;
    }

    private void investmentMovementDialog(InvestmentStore.Type movementType) {
        boolean contribution = movementType == InvestmentStore.Type.APORTE;
        String actionName = contribution ? "Aporte" : "Resgate";
        JDialog dialog = new JDialog(this, "Registrar " + actionName.toLowerCase(PT), true); dialog.setSize(500, contribution ? 390 : 340); dialog.setLocationRelativeTo(this);
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12)); form.setBackground(CARD); form.setBorder(BorderFactory.createEmptyBorder(22, 24, 14, 24));
        JTextField date = new JTextField(LocalDate.now().format(DATE_BR)); styleInput(date); installDateInputFilter(date);
        JTextField amount = new JTextField(); styleInput(amount); installCurrencyFormatting(amount);
        JTextField note = new JTextField(); styleInput(note);
        javax.swing.JComboBox<String> source = contribution ? new ThemeComboBox<>(new String[]{"Receita registrada", "Aporte externo (não é receita)"}) : null;
        if (source != null) styleInput(source);
        addField(form, "Data (dd/MM/aaaa)", date); addField(form, "Valor (R$)", amount);
        if (contribution) addField(form, "Origem do aporte", source);
        addField(form, "Observação (opcional)", note);
        JLabel help = new JLabel(contribution ? "Será descontado das receitas registradas neste mês." : "O resgate não pode superar o saldo disponível.");
        if (contribution) source.addActionListener(e -> help.setText(source.getSelectedIndex() == 0
                ? "Será descontado das receitas registradas neste mês."
                : "Aporte externo: não altera o total de receitas."));
        help.setForeground(MUTED); help.setFont(new Font("Segoe UI", Font.PLAIN, 10)); form.add(new JLabel("")); form.add(help);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8)); buttons.setBackground(CARD);
        JButton cancel = secondaryButton("Cancelar"); cancel.addActionListener(e -> dialog.dispose());
        JButton save = primaryButton("Salvar " + actionName.toLowerCase(PT));
        save.addActionListener(e -> {
            try {
                LocalDate parsedDate = LocalDate.parse(date.getText().trim(), DATE_BR);
                BigDecimal parsedAmount = parseMoney(amount.getText());
                if (parsedAmount.signum() <= 0) throw new IllegalArgumentException("Digite um valor maior que zero.");
                InvestmentStore.Source selectedSource = contribution && source.getSelectedIndex() == 0
                        ? InvestmentStore.Source.RECEITA : InvestmentStore.Source.EXTERNO;
                if (selectedSource == InvestmentStore.Source.RECEITA) {
                    YearMonth month = YearMonth.from(parsedDate);
                    BigDecimal availableIncome = recordedIncome(month).subtract(investmentStore.incomeFundedContributions(month));
                    if (parsedAmount.compareTo(availableIncome) > 0)
                        throw new IllegalArgumentException("O aporte excede as receitas registradas ainda disponíveis neste mês (" + money(availableIncome.max(BigDecimal.ZERO)) + "). Escolha aporte externo se o valor veio de outra fonte.");
                }
                investmentStore.add(parsedDate, movementType, parsedAmount, note.getText(), selectedSource);
                dialog.dispose(); refreshPage();
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? "Confira a data e o valor." : ex.getMessage();
                JOptionPane.showMessageDialog(dialog, message, "Não foi possível registrar", JOptionPane.WARNING_MESSAGE);
            }
        });
        buttons.add(cancel); buttons.add(save); dialog.setLayout(new BorderLayout()); dialog.add(form, BorderLayout.CENTER); dialog.add(buttons, BorderLayout.SOUTH); dialog.setVisible(true);
        SwingUtilities.invokeLater(amount::requestFocusInWindow);
    }

    private JPanel buildPrivacyPage() {
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false);
        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel privacyCard = cardPanel(); privacyCard.setLayout(new BoxLayout(privacyCard, BoxLayout.Y_AXIS));
        privacyCard.setBorder(BorderFactory.createCompoundBorder(privacyCard.getBorder(), BorderFactory.createEmptyBorder(22, 24, 22, 24))); privacyCard.setAlignmentX(Component.LEFT_ALIGNMENT); privacyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        JLabel badge = new JLabel("●  ARMAZENAMENTO LOCAL"); badge.setFont(new Font("Segoe UI", Font.BOLD, 10)); badge.setForeground(GREEN);
        JLabel heading = new JLabel("Seus dados ficam com você."); heading.setFont(new Font("Segoe UI", Font.BOLD, 19)); heading.setForeground(INK);
        JLabel explanation = new JLabel("<html><div style='width:760px'>Os dados do perfil ativo ficam neste computador, em uma pasta separada. Não é necessária uma conta e os dados não são enviados a um servidor.</div></html>"); explanation.setFont(new Font("Segoe UI", Font.PLAIN, 12)); explanation.setForeground(MUTED);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT); heading.setAlignmentX(Component.LEFT_ALIGNMENT); explanation.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea location = new JTextArea(activeDataDir.toString()); location.setRows(2); location.setColumns(72); location.setEditable(false); location.setLineWrap(true); location.setWrapStyleWord(true); location.setBackground(CARD_ALT); location.setForeground(INK); location.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11)); location.setPreferredSize(new Dimension(700, 52)); location.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52)); location.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0)); actions.setOpaque(false); actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton export = primaryButton("Exportar lançamentos CSV"); export.addActionListener(e -> exportCsv());
        JButton importButton = secondaryButton("Importar CSV"); importButton.addActionListener(e -> importCsv());
        JButton open = secondaryButton("Abrir pasta local"); open.addActionListener(e -> openDataFolder());
        actions.add(export); actions.add(importButton); actions.add(open);
        privacyCard.add(badge); privacyCard.add(Box.createVerticalStrut(13)); privacyCard.add(heading); privacyCard.add(Box.createVerticalStrut(7)); privacyCard.add(explanation); privacyCard.add(Box.createVerticalStrut(17)); privacyCard.add(location); privacyCard.add(Box.createVerticalStrut(17)); privacyCard.add(actions);
        content.add(privacyCard);

        JPanel themeCard = cardPanel(); themeCard.setLayout(new BoxLayout(themeCard, BoxLayout.Y_AXIS));
        themeCard.setBorder(BorderFactory.createCompoundBorder(themeCard.getBorder(), BorderFactory.createEmptyBorder(18, 22, 18, 22)));
        JPanel themeCopy = new JPanel(); themeCopy.setOpaque(false); themeCopy.setLayout(new BoxLayout(themeCopy, BoxLayout.Y_AXIS));
        JLabel themeTitle = new JLabel("Cor do tema"); themeTitle.setFont(new Font("Segoe UI", Font.BOLD, 15)); themeTitle.setForeground(INK);
        JLabel themeDescription = new JLabel("<html><div style='width:440px'>Escolha uma paleta pronta ou abra o quadro para selecionar qualquer cor. A preferência fica salva neste computador.</div></html>"); themeDescription.setFont(new Font("Segoe UI", Font.PLAIN, 11)); themeDescription.setForeground(MUTED);
        themeCopy.add(themeTitle); themeCopy.add(Box.createVerticalStrut(5)); themeCopy.add(themeDescription);
        javax.swing.JComboBox<AppTheme> themeChooser = new ThemeComboBox<>(AppTheme.values());
        themeChooser.setSelectedItem(selectedTheme); themeChooser.setPreferredSize(new Dimension(180, 38)); styleInput(themeChooser);
        themeChooser.addActionListener(e -> {
            Object value = themeChooser.getSelectedItem();
            if (!(value instanceof AppTheme theme)) return;
            if (theme == AppTheme.PERSONALIZADA) {
                Color chosen = chooseCustomThemeColor();
                if (chosen == null) { themeChooser.setSelectedItem(selectedTheme); return; }
                changeTheme(theme, chosen);
            } else changeTheme(theme, null);
        });
        JButton chooseColor = secondaryButton("Escolher cor…");
        chooseColor.addActionListener(e -> {
            Color chosen = chooseCustomThemeColor();
            if (chosen != null) changeTheme(AppTheme.PERSONALIZADA, chosen);
        });
        JPanel colorSwatch = new JPanel(); colorSwatch.setPreferredSize(new Dimension(30, 30)); colorSwatch.setBackground(ACCENT);
        colorSwatch.setBorder(BorderFactory.createLineBorder(BORDER, 1, true)); colorSwatch.setToolTipText("Amostra da cor principal atual");
        JPanel themeControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); themeControls.setOpaque(false);
        themeControls.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeControls.add(themeChooser); themeControls.add(chooseColor); themeControls.add(colorSwatch);
        themeCard.add(themeCopy); themeCard.add(Box.createVerticalStrut(12)); themeCard.add(themeControls);
        themeCard.setAlignmentX(Component.LEFT_ALIGNMENT); themeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        content.add(Box.createVerticalStrut(12)); content.add(themeCard);
        JScrollPane scroll = new JScrollPane(content); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        page.add(scroll, BorderLayout.CENTER); return page;
    }

    private Color chooseCustomThemeColor() {
        Color initial = selectedTheme == AppTheme.PERSONALIZADA ? ACCENT : paletteFor(selectedTheme).accent();
        return javax.swing.JColorChooser.showDialog(this, "Escolha a cor principal do OrçaBem", initial);
    }

    private void changeTheme(AppTheme theme) { changeTheme(theme, null); }

    private void changeTheme(AppTheme theme, Color customColor) {
        if (theme == null || (theme == selectedTheme && customColor == null)) return;
        if (theme == AppTheme.PERSONALIZADA && customColor == null) return;
        if (customColor != null) {
            customColor = new Color(customColor.getRed(), customColor.getGreen(), customColor.getBlue());
            THEME_PREFERENCES.putInt("customAccentRgb", customColor.getRGB());
        }
        selectedTheme = theme;
        applyPalette(paletteFor(theme));
        THEME_PREFERENCES.put("theme", theme.name());
        try { THEME_PREFERENCES.flush(); }
        catch (BackingStoreException ex) {
            JOptionPane.showMessageDialog(this, "O tema foi alterado, mas não foi possível salvar a preferência: " + ex.getMessage(), "Preferência do tema", JOptionPane.WARNING_MESSAGE);
        }
        sidebar.removeAll(); navButtons.clear(); pageHost.removeAll(); getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout()); getContentPane().setBackground(CANVAS);
        add(buildSidebar(), BorderLayout.WEST); add(buildMain(), BorderLayout.CENTER);
        refreshPage(); getContentPane().revalidate(); getContentPane().repaint();
    }

    private void openDataFolder() {
        try {
            Files.createDirectories(activeDataDir);
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(activeDataDir.toFile());
            else JOptionPane.showMessageDialog(this, "A pasta local deste perfil fica em:\n" + activeDataDir);
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
        JButton edit = secondaryButton("Editar lançamento"); edit.setEnabled(false);
        edit.setToolTipText("Selecione um lançamento na tabela para editá-lo");
        JButton delete = secondaryButton("Remover lançamento"); delete.setEnabled(false);
        delete.setToolTipText("Selecione um lançamento na tabela para removê-lo");
        actions.add(edit); actions.add(delete);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) { boolean selected = table.getSelectedRow() >= 0; edit.setEnabled(selected); delete.setEnabled(selected); } });
        edit.addActionListener(e -> { int row = table.getSelectedRow(); if (row >= 0 && row < filtered.size()) addTransactionDialog(filtered.get(row)); });
        delete.addActionListener(e -> { int row = table.getSelectedRow(); if (row < 0 || row >= filtered.size()) return;
            Transaction chosen = filtered.get(row);
            String description = chosen.getDescription().equals("Sem descrição") ? chosen.getCategory() : chosen.getDescription();
            String details = description + " · " + chosen.getDate().format(DATE_BR) + " · " + money(chosen.getAmount());
            int confirm = JOptionPane.showConfirmDialog(this, "Remover este lançamento?\n\n" + details + "\n\nEssa ação não pode ser desfeita.",
                    "Remover lançamento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
            if (confirm == JOptionPane.YES_OPTION) {
                int originalIndex = transactions.indexOf(chosen);
                if (originalIndex >= 0) {
                    transactions.remove(originalIndex);
                    if (!persistTransactions()) transactions.add(originalIndex, chosen);
                    refreshPage();
                }
            }
        });
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
        styleInput(date); installDateInputFilter(date); styleInput(type); styleInput(category);
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
        installAllowedInputFilter(field, true);
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

    private static void installDateInputFilter(JTextField field) { installAllowedInputFilter(field, false); }

    private static void installAllowedInputFilter(JTextField field, boolean currency) {
        if (!(field.getDocument() instanceof javax.swing.text.AbstractDocument document)) return;
        document.setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override public void insertString(FilterBypass bypass, int offset, String text, javax.swing.text.AttributeSet attributes) throws javax.swing.text.BadLocationException {
                replace(bypass, offset, 0, text, attributes);
            }

            @Override public void replace(FilterBypass bypass, int offset, int length, String text, javax.swing.text.AttributeSet attributes) throws javax.swing.text.BadLocationException {
                String insertion = text == null ? "" : text;
                String current = bypass.getDocument().getText(0, bypass.getDocument().getLength());
                String candidate = current.substring(0, offset) + insertion + current.substring(offset + length);
                if (isAllowedNumericText(candidate, currency)) bypass.replace(offset, length, insertion, attributes);
            }
        });
    }

    private static boolean isAllowedNumericText(String value, boolean currency) {
        int commas = 0, periods = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') continue;
            if (currency && c == ',') commas++;
            else if (currency && c == '.') periods++;
            else if (!currency && c == '/') continue;
            else return false;
        }
        if (!currency) return true;
        return commas <= 1 && (commas == 1 || periods <= 1);
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

    private boolean persistTransactions() {
        try { CsvStore.save(transactionsFile, transactions); return true; }
        catch (IOException ex) { JOptionPane.showMessageDialog(this, "Não foi possível salvar os dados locais: " + ex.getMessage(), "Erro ao salvar", JOptionPane.ERROR_MESSAGE); return false; }
    }

    private BigDecimal total(YearMonth month, Transaction.Type type) {
        BigDecimal recorded = transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(month) && t.getType() == type).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return type == Transaction.Type.INCOME ? recorded.subtract(investmentStore.incomeFundedContributions(month)) : recorded;
    }

    private BigDecimal recordedIncome(YearMonth month) {
        return transactions.stream().filter(t -> YearMonth.from(t.getDate()).equals(month) && t.getType() == Transaction.Type.INCOME)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private JButton primaryButton(String text) { return styleButton(text, ACCENT, ACCENT_TEXT, ACCENT_HOVER); }
    private JButton secondaryButton(String text) { return styleButton(text, CARD_ALT, INK, GREEN_LIGHT); }
    private JButton smallButton(String text) { JButton b = styleButton(text, CARD_ALT, INK, GREEN_LIGHT); b.setPreferredSize(new Dimension(37, 37)); b.setFont(new Font("Segoe UI", Font.BOLD, 18)); return b; }
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
                    JButton arrow = new ThemeButton("▾", CARD_ALT, GREEN_LIGHT, BORDER);
                    arrow.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12)); arrow.setPreferredSize(new Dimension(34, 28));
                    arrow.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3)); return arrow;
                }
            });
            combo.setOpaque(false); combo.setBackground(CARD_ALT); combo.setForeground(INK); combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focused) {
                    JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                    cell.setOpaque(true); cell.setFont(new Font("Segoe UI", Font.PLAIN, 12)); cell.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
                    cell.setBackground(selected ? GREEN_LIGHT : CARD_ALT); cell.setForeground(INK);
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
        BigDecimal amount = new BigDecimal(cleaned);
        if (amount.signum() < 0) throw new NumberFormatException("Valores negativos não são permitidos.");
        return amount;
    }

    private static final class SidebarIcon implements javax.swing.Icon {
        private final String type;
        SidebarIcon(String type) { this.type = type; }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override public void paintIcon(Component component, java.awt.Graphics raw, int x, int y) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) raw.create();
            g.translate(x, y);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(component.isEnabled() ? component.getForeground() : MUTED);
            g.setStroke(new java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            switch (type) {
                case "dashboard" -> {
                    java.awt.geom.Path2D house = new java.awt.geom.Path2D.Double();
                    house.moveTo(2, 8); house.lineTo(9, 2.5); house.lineTo(16, 8); g.draw(house);
                    g.drawRoundRect(4, 7, 10, 9, 2, 2); g.drawLine(8, 16, 8, 11); g.drawLine(10, 16, 10, 11); g.drawLine(8, 11, 10, 11);
                }
                case "transactions" -> {
                    g.drawLine(2, 5, 15, 5); g.drawLine(12, 2, 15, 5); g.drawLine(12, 8, 15, 5);
                    g.drawLine(16, 13, 3, 13); g.drawLine(6, 10, 3, 13); g.drawLine(6, 16, 3, 13);
                }
                case "budgets" -> {
                    g.drawRoundRect(3, 2, 12, 14, 2, 2); g.drawLine(6, 6, 12, 6); g.drawLine(6, 9, 12, 9); g.drawLine(6, 12, 10, 12);
                }
                case "investments" -> {
                    g.drawLine(2, 16, 16, 16); g.drawRoundRect(3, 10, 3, 6, 1, 1); g.drawRoundRect(8, 7, 3, 9, 1, 1); g.drawRoundRect(13, 3, 3, 13, 1, 1);
                }
                case "categories" -> {
                    g.drawRoundRect(2, 3, 5, 5, 2, 2); g.drawRoundRect(11, 3, 5, 5, 2, 2);
                    g.drawRoundRect(2, 11, 5, 5, 2, 2); g.drawRoundRect(11, 11, 5, 5, 2, 2);
                }
                case "insights" -> {
                    g.drawOval(5, 2, 8, 9); g.drawLine(7, 11, 7, 13); g.drawLine(11, 11, 11, 13); g.drawLine(7, 14, 11, 14); g.drawLine(8, 16, 10, 16);
                    g.drawLine(9, 0, 9, 1); g.drawLine(3, 5, 4, 6); g.drawLine(14, 6, 15, 5);
                }
                case "privacy" -> {
                    java.awt.geom.Path2D shield = new java.awt.geom.Path2D.Double();
                    shield.moveTo(9, 2); shield.lineTo(15, 4.5); shield.lineTo(14.5, 10); shield.lineTo(12, 13.5); shield.lineTo(9, 16); shield.lineTo(6, 13.5); shield.lineTo(3.5, 10); shield.lineTo(3, 4.5); shield.closePath(); g.draw(shield);
                    g.drawLine(6, 9, 8, 11); g.drawLine(8, 11, 12, 7);
                }
                default -> { g.drawOval(3, 3, 12, 12); g.drawLine(6, 9, 12, 9); }
            }
            g.dispose();
        }
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
            return isEnabled() ? super.getForeground() : MUTED;
        }

        @Override protected void paintComponent(java.awt.Graphics raw) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) raw.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = !isEnabled() ? CARD_ALT.darker() : getModel().isPressed() ? getBackground().darker() : getModel().isRollover() ? hoverColor : getBackground();
            g.setColor(fill); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.setColor(outlineColor); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.dispose();
            super.paintComponent(raw);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> {
            try {
                OrcaBemApp app = new OrcaBemApp();
                app.setVisible(true);
                SwingUtilities.invokeLater(app::startAutomaticUpdateChecks);
            }
            catch (IOException ex) { JOptionPane.showMessageDialog(null, "Não foi possível carregar os dados locais: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE); }
        });
    }
}
