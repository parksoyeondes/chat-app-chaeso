// HangmanPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class HangmanPanel extends JPanel {

    // =========================================================
    // 네트워크 모드용 리스너
    // =========================================================
    public interface HangmanNetListener {
        void onLetterChosen(char ch); // 글자 눌렸을 때
        void onGameEnd(); // 게임 끝낼 때
        void onRestartRequested(); // 재시작 요청 할 때
    }

    private HangmanNetListener netListener;
    private boolean networkMode = false;

    // =========================================================
    // 테마 + 테마별 단어
    // =========================================================
    private String[] THEMES = { "My Friend Name", "Country", "Animal" };

    private String[][] WORDS_BY_THEME = {
            { "parksoyeon", "sonchaerim" },
            { "korea", "japan", "france" },
            { "dog", "cat", "elephant" }
    };

    // =========================================================
    // 게임 상태
    // =========================================================
    private String answer;
    private char[] current;
    private boolean[] used = new boolean[26];
    private int mistakes = 0;
    private int maxMistakes = 6;
    private int score = 0;

    // =========================================================
    // UI 컴포넌트
    // =========================================================
    private JLabel lblScore;
    private JLabel lblTheme;
    private JLabel lblWord;
    private JLabel lblUnder;
    private HangmanDrawingPanel drawingPanel;

    private JPanel keyboardPanel;
    private JButton btnEnd;

    private JButton[] letterButtons = new JButton[26];

    // =========================================================
    // 생성자
    // =========================================================
    public HangmanPanel() {
        this(null, false);
    }

    public HangmanPanel(HangmanNetListener listener, boolean networkMode) {
        this.netListener = listener;
        this.networkMode = networkMode;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // =========================================================
        // 상단 HANGMAN 타이틀 + 테마 스코어
        // =========================================================
        JPanel topBar = new JPanel();
        topBar.setLayout(new BoxLayout(topBar, BoxLayout.Y_AXIS));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(20, 10, 0, 10));

        JButton titleButton = new JButton("HANGMAN");
        titleButton.setBackground(new Color(190, 190, 190));
        titleButton.setForeground(Color.BLACK);
        titleButton.setFont(new Font("Dialog", Font.BOLD, 18));
        titleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleButton.setMaximumSize(new Dimension(220, 40));
        titleButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        titleButton.setBorderPainted(true);

        titleButton.setEnabled(true);
        titleButton.setFocusable(false);
        titleButton.setRolloverEnabled(false);
        titleButton.addActionListener(e -> {});

        topBar.add(titleButton);
        topBar.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(200, 200, 200));
        topBar.add(sep);

        JPanel themeScorePanel = new JPanel(null);
        themeScorePanel.setBackground(Color.WHITE);
        themeScorePanel.setBorder(new EmptyBorder(10, 20, 5, 20));
        themeScorePanel.setPreferredSize(new Dimension(0, 50));

        JLabel lblThemeTitle = new JLabel("Theme");
        lblThemeTitle.setFont(new Font("Dialog", Font.BOLD, 18));
        lblThemeTitle.setForeground(new Color(150, 150, 150));
        lblThemeTitle.setBounds(180, 20, 100, 30);

        lblScore = new JLabel("Score: 00");
        lblScore.setFont(new Font("Dialog", Font.BOLD, 17));
        lblScore.setForeground(Color.RED);
        lblScore.setBounds(330, 10, 100, 30);

        themeScorePanel.add(lblThemeTitle);
        themeScorePanel.add(lblScore);
        topBar.add(themeScorePanel);

        lblTheme = new JLabel("“ OO (Random) ”");
        lblTheme.setFont(new Font("Dialog", Font.BOLD, 18));
        lblTheme.setForeground(Color.BLACK);
        lblTheme.setHorizontalAlignment(SwingConstants.CENTER);
        lblTheme.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTheme.setBorder(new EmptyBorder(5, 0, 15, 0));

        topBar.add(lblTheme);
        add(topBar, BorderLayout.NORTH);

        // =========================================================
        // 중앙 단어 + 밑줄 + 그림
        // =========================================================
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblWord = new JLabel("", SwingConstants.CENTER);
        lblWord.setFont(new Font("Dialog", Font.BOLD, 24));
        lblWord.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblUnder = new JLabel("", SwingConstants.CENTER);
        lblUnder.setFont(new Font("Dialog", Font.PLAIN, 22));
        lblUnder.setForeground(Color.GRAY);
        lblUnder.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblUnder.setBorder(new EmptyBorder(5, 0, 20, 0));

        drawingPanel = new HangmanDrawingPanel();
        drawingPanel.setPreferredSize(new Dimension(260, 120));
        drawingPanel.setMaximumSize(new Dimension(260, 120));
        drawingPanel.setBackground(Color.WHITE);

        centerPanel.add(lblWord);
        centerPanel.add(lblUnder);
        centerPanel.add(drawingPanel);

        add(centerPanel, BorderLayout.CENTER);

        // =========================================================
        // 하단 키보드 + End 버튼
        // =========================================================
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(new Color(220, 220, 220));

        keyboardPanel = new JPanel(new GridBagLayout());
        keyboardPanel.setBackground(new Color(220, 220, 220));
        keyboardPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        createKeyboard();
        setupKeyBindings();

        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        endPanel.setBackground(new Color(220, 220, 220));

        btnEnd = new JButton("End");
        btnEnd.setBackground(new Color(190, 70, 60));
        btnEnd.setForeground(Color.BLACK);
        btnEnd.setFocusPainted(false);
        btnEnd.setPreferredSize(new Dimension(70, 32));
        btnEnd.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnEnd.setBorderPainted(true);

        btnEnd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (networkMode && netListener != null) {
                    netListener.onGameEnd();
                }
                onEndGame();
            }
        });

        endPanel.add(btnEnd);

        bottom.add(keyboardPanel);
        bottom.add(endPanel);

        add(bottom, BorderLayout.SOUTH);

        if (!networkMode) {
            startNewGameRandom();
        }
    }

    // =========================================================
    // 키보드 생성
    // =========================================================
    private void createKeyboard() {
        keyboardPanel.removeAll();
        letterButtons = new JButton[26];

        String[] rows = { "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM" };
        int[] startOffset = { 0, 1, 2 };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int offset = startOffset[r];

            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);

                JButton btn = new JButton(String.valueOf(ch));
                btn.setPreferredSize(new Dimension(40, 40));
                btn.setMinimumSize(new Dimension(36, 36));
                btn.setFont(new Font("Dialog", Font.BOLD, 14));
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setFocusPainted(false);

                btn.setBackground(new Color(240, 240, 240));
                btn.setForeground(Color.BLACK);

                btn.addActionListener(new LetterButtonListener(ch, btn));

                int idx = ch - 'A';
                if (idx >= 0 && idx < 26) {
                    letterButtons[idx] = btn;
                }

                gbc.gridx = c + offset;
                gbc.gridy = r;
                keyboardPanel.add(btn, gbc);
            }
        }

        keyboardPanel.revalidate();
        keyboardPanel.repaint();
    }

    // =========================================================
    // 실제 키보드 입력 바인딩
    // =========================================================
    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        for (char c = 'A'; c <= 'Z'; c++) {
            String key = "KEY_" + c;

            im.put(KeyStroke.getKeyStroke(c), key);
            im.put(KeyStroke.getKeyStroke(Character.toLowerCase(c)), key);

            char finalC = c;
            am.put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleKeyPress(finalC);
                }
            });
        }
    }

    private void handleKeyPress(char ch) {
        ch = Character.toUpperCase(ch);
        int idx = ch - 'A';
        if (idx < 0 || idx >= 26) return;

        JButton btn = letterButtons[idx];
        if (btn == null || !btn.isEnabled()) return;

        if (networkMode && netListener != null) {
            netListener.onLetterChosen(Character.toLowerCase(ch));
        } else {
            onLetterSelected(Character.toLowerCase(ch), btn);
        }
    }

    // =========================================================
    // 새 게임 시작
    // =========================================================
    public void startNewGameRandom() {
        Random rand = new Random();
        int themeIdx = rand.nextInt(THEMES.length);
        int wordIdx = rand.nextInt(WORDS_BY_THEME[themeIdx].length);
        startNewGameFromIndex(wordIdx, themeIdx);
    }

    public void startNewGameFromIndex(int wordIdx, int themeIdx) {
        answer = WORDS_BY_THEME[themeIdx][wordIdx].toLowerCase();
        current = new char[answer.length()];

        for (int i = 0; i < current.length; i++) current[i] = ' ';
        for (int i = 0; i < used.length; i++) used[i] = false;

        mistakes = 0;

        lblTheme.setText("“ " + THEMES[themeIdx] + " ”");

        createKeyboard();
        updateWordLabel();
        drawingPanel.setMistakes(mistakes, maxMistakes);
    }

    private void updateWordLabel() {
        StringBuilder w = new StringBuilder();
        StringBuilder u = new StringBuilder();

        for (int i = 0; i < answer.length(); i++) {
            if (current[i] == ' ') w.append("  ");
            else w.append(current[i]).append(' ');
            u.append("_ ");
        }

        lblWord.setText(w.toString());
        lblUnder.setText(u.toString());
    }

    // =========================================================
    // 글자 선택 처리
    // =========================================================
    private void onLetterSelected(char ch, JButton btn) {
        ch = Character.toLowerCase(ch);

        int idx = ch - 'a';
        if (idx < 0 || idx >= 26) return;
        if (used[idx]) return;

        used[idx] = true;
        if (btn != null) btn.setEnabled(false);

        boolean hit = false;
        for (int i = 0; i < answer.length(); i++) {
            if (answer.charAt(i) == ch) {
                current[i] = ch;
                hit = true;
            }
        }

        if (!hit) {
            mistakes++;
            drawingPanel.setMistakes(mistakes, maxMistakes);
            if (mistakes >= maxMistakes) onLose();
        } else {
            updateWordLabel();
            if (isAllRevealed()) onWin();
        }
    }

    private boolean isAllRevealed() {
        for (char c : current) {
            if (c == ' ') return false;
        }
        return true;
    }

    // =========================================================
    // 승리 패배 처리
    // =========================================================
    private void onWin() {
        score++;
        lblScore.setText(String.format("Score: %02d", score));
        showEndDialog(true);
    }

    private void onLose() {
        showEndDialog(false);
    }

    // =========================================================
    // Game Over 다이얼로그
    // 1번 코드 형태로 버튼 사이즈 모양 위치 맞춤
    // =========================================================
    private void showEndDialog(boolean win) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Game Over", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String titleText = win ? "Success!" : "Failed!";
        String detailText = "Answer: " + answer;

        JLabel msg = new JLabel(
                "<html><center>" + titleText + "<br/>" + detailText + "</center></html>",
                SwingConstants.CENTER
        );
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.setFont(new Font("Dialog", Font.PLAIN, 14));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton restartBtn = new JButton("Restart");
        restartBtn.setBackground(new Color(60, 179, 113));
        restartBtn.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        restartBtn.setForeground(Color.BLACK);
        restartBtn.setFocusPainted(false);
        restartBtn.setPreferredSize(new Dimension(80, 36));

        JButton exitBtn = new JButton("End");
        exitBtn.setBackground(new Color(190, 70, 60));
        exitBtn.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        exitBtn.setForeground(Color.BLACK);
        exitBtn.setFocusPainted(false);
        exitBtn.setPreferredSize(new Dimension(80, 36));

        if (networkMode && netListener != null) {
            restartBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    netListener.onRestartRequested();
                }
            });

            exitBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    netListener.onGameEnd();
                    onEndGame();
                }
            });

        } else {
            restartBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    startNewGameRandom();
                }
            });

            exitBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    onEndGame();
                }
            });
        }

        btnPanel.add(restartBtn);
        btnPanel.add(exitBtn);

        panel.add(msg);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnPanel);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(270, 180)); 
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private void onEndGame() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) w.dispose();
    }

    // =========================================================
    // 행맨 그림 패널
    // =========================================================
    private static class HangmanDrawingPanel extends JPanel {
        private int mistakes = 0;
        private int max = 6;

        private Image headImage;

        public HangmanDrawingPanel() {
            headImage = new ImageIcon("src/icons/tomato_face.png").getImage();
        }

        public void setMistakes(int mistakes, int max) {
            this.mistakes = mistakes;
            this.max = max;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);

            int w = getWidth();
            int h = getHeight();

            int offset = 20;
            int dx = 30;

            g2.drawLine(30, h - 20 + offset, w - 30, h - 20 + offset);
            g2.drawLine(60 + dx, h - 20 + offset, 60 + dx, -10 + offset);
            g2.drawLine(60 + dx, -10 + offset, w / 2 + dx, -10 + offset);
            g2.drawLine(w / 2 + dx, -10 + offset, w / 2 + dx, 0 + offset);

            int cx = w / 2 + dx;

            if (mistakes >= 2) g2.drawLine(cx, 24 + offset, cx, 45 + offset);
            if (mistakes >= 3) g2.drawLine(cx, 30 + offset, cx - 20, 40 + offset);
            if (mistakes >= 4) g2.drawLine(cx, 30 + offset, cx + 20, 40 + offset);
            if (mistakes >= 5) g2.drawLine(cx, 45 + offset, cx - 15, 60 + offset);
            if (mistakes >= 6) g2.drawLine(cx, 45 + offset, cx + 15, 60 + offset);

            if (mistakes >= 1 && headImage != null) {
                int headW = 50;
                int headH = 30;
                int headX = cx - headW / 2;
                int headY = 0 + offset;
                g2.drawImage(headImage, headX, headY, headW, headH, this);
            }
        }
    }

    // =========================================================
    // 네트워크에서 받은 추측 적용
    // =========================================================
    public void applyGuessFromNetwork(char ch) {
        ch = Character.toLowerCase(ch);
        int idx = ch - 'a';
        if (idx < 0 || idx >= 26) return;

        JButton btn = letterButtons[idx];
        if (btn == null || !btn.isEnabled()) return;

        onLetterSelected(ch, btn);
    }

    // =========================================================
    // 키보드 버튼 리스너
    // =========================================================
    private class LetterButtonListener implements ActionListener {
        private char ch;
        private JButton btn;

        public LetterButtonListener(char ch, JButton btn) {
            this.ch = ch;
            this.btn = btn;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (networkMode && netListener != null) {
                netListener.onLetterChosen(Character.toLowerCase(ch));
            } else {
                onLetterSelected(Character.toLowerCase(ch), btn);
            }
        }
    }
}
