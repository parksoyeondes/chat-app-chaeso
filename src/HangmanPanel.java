import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class HangmanPanel extends JPanel {
    // ===== 네트워크 모드용 리스너 =====
    public interface HangmanNetListener {
        void onLetterChosen(char ch);   // 내가 글자 선택함
        void onGameEnd();               // 게임 끝내기(나가기)
        void onRestartRequested();      // 재시작하기
    }
    private HangmanNetListener netListener;
    private boolean networkMode = false;

    // ===== 게임 상태 =====
    private String[] WORDS  = { "parksoyeon", "sonchaerim", "seoyujin", "shinyoungseo" };
    private String[] THEMES = { "My Friend Name" };

    private String answer;          // 정답 단어
    private char[] current;         // 맞춘 글자 상태
    private boolean[] used = new boolean[26];
    private int mistakes = 0;
    private int maxMistakes = 6;
    private int score = 0;

    // ===== UI 컴포넌트 =====
    private JLabel lblScore;
    private JLabel lblTheme;
    private JLabel lblWord;
    private JLabel lblUnder;
    private HangmanDrawingPanel drawingPanel;

    private JPanel keyboardPanel;
    private JButton btnEnd;

    // 키보드 버튼 배열 (키보드 입력 연동용)
    private JButton[] letterButtons = new JButton[26];

    // ----------------------------- 생성자 쪼개기 -------------------------------
    public HangmanPanel() {
        this(null, false);   // 기본은 로컬 모드
    }
    public HangmanPanel(HangmanNetListener listener, boolean networkMode) {
        this.netListener = listener;
        this.networkMode = networkMode;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // =========================================
        // 1) 상단 HANGMAN 타이틀
        // =========================================
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

        // 테두리 추가
        titleButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        titleButton.setBorderPainted(true);

        // 비활성화 회색 방지 (enabled 유지 + 클릭 무효)
        titleButton.setEnabled(true);
        titleButton.setFocusable(false);
        titleButton.setRolloverEnabled(false);
        titleButton.addActionListener(e -> { /* 아무 동작 없음 */ });

        topBar.add(titleButton);
        topBar.add(Box.createVerticalStrut(20));

        // ★ 여기 구분선 추가
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(200, 200, 200));
        topBar.add(sep);

        // =========================================
        // 2) Theme / Score 라인 (좌표 직접 지정)
        // =========================================
        JPanel themeScorePanel = new JPanel(null);   // ★ 절대 좌표 레이아웃
        themeScorePanel.setBackground(Color.WHITE);
        themeScorePanel.setBorder(new EmptyBorder(10, 20, 5, 20));
        themeScorePanel.setPreferredSize(new Dimension(0, 50)); // 높이 확보용

        JLabel lblThemeTitle = new JLabel("Theme");
        lblThemeTitle.setFont(new Font("Dialog", Font.BOLD, 18));
        lblThemeTitle.setForeground(new Color(150, 150, 150));
        // ★ Theme 텍스트 위치 직접 조정 (x, y, width, height)
        lblThemeTitle.setBounds(180, 20, 100, 30);

        lblScore = new JLabel("Score: 00");
        lblScore.setFont(new Font("Dialog", Font.BOLD, 17));
        lblScore.setForeground(Color.RED);
        // ★ Score 위치도 직접 조정
        lblScore.setBounds(330, 10, 100, 30);

        themeScorePanel.add(lblThemeTitle);
        themeScorePanel.add(lblScore);

        topBar.add(themeScorePanel);

        // =========================================
        // 3) 실제 Theme 텍스트 (My Friend Name)
        // =========================================
        lblTheme = new JLabel("“ OO (Random) ”");
        lblTheme.setFont(new Font("Dialog", Font.BOLD, 18));
        lblTheme.setForeground(Color.BLACK);
        lblTheme.setHorizontalAlignment(SwingConstants.CENTER);
        lblTheme.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTheme.setBorder(new EmptyBorder(5, 0, 15, 0));

        topBar.add(lblTheme);

        add(topBar, BorderLayout.NORTH);

        // =========================================
        // 4) 단어 + 밑줄 + 행맨 그림
        // =========================================
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

        drawingPanel = new HangmanDrawingPanel();   // ★ 이미지 머리 쓰는 패널
        drawingPanel.setPreferredSize(new Dimension(260, 120));
        drawingPanel.setMaximumSize(new Dimension(260, 120));
        drawingPanel.setBackground(Color.WHITE);

        centerPanel.add(lblWord);
        centerPanel.add(lblUnder);
        centerPanel.add(drawingPanel);

        add(centerPanel, BorderLayout.CENTER);

        // =========================================
        // 5) 가상 키보드 + End 버튼
        // =========================================
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(new Color(220, 220, 220));

        keyboardPanel = new JPanel(new GridBagLayout());
        keyboardPanel.setBackground(new Color(220, 220, 220));
        keyboardPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        createKeyboard();     // 키보드 버튼 생성
        setupKeyBindings();   // 실제 키보드 입력(A~Z) 연동

        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        endPanel.setBackground(new Color(220, 220, 220));

        btnEnd = new JButton("End");
        btnEnd.setBackground(new Color(190, 70, 60));
        btnEnd.setForeground(Color.BLACK);
        btnEnd.setFocusPainted(false);
        btnEnd.setPreferredSize(new Dimension(70, 32));
        // End 버튼 테두리
        btnEnd.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnEnd.setBorderPainted(true);

        btnEnd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (networkMode && netListener != null) {
                    netListener.onGameEnd();   // 서버로 /hangEnd 보내게 할 예정
                }
                onEndGame(); // 내 창 닫기
            }
        });


        endPanel.add(btnEnd);

        bottom.add(keyboardPanel);
        bottom.add(endPanel);

        add(bottom, BorderLayout.SOUTH);

        if (!networkMode) {
            // 로컬 모드일 때만 랜덤 시작
            startNewGameRandom();
        }
    }


    // =========================================
    // 키보드 버튼 생성 (QWERTY + 가운데 정렬: 0,1,2 offset)
    // =========================================
    private void createKeyboard() {
        keyboardPanel.removeAll();
        letterButtons = new JButton[26];

        String[] rows = {
                "QWERTYUIOP",
                "ASDFGHJKL",
                "ZXCVBNM"
        };

        // 각 줄의 시작 column (들여쓰기)
        int[] startOffset = {0, 1, 2};

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int offset = startOffset[r];

            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);           // Q, W, E ...

                JButton btn = new JButton(String.valueOf(ch));
                btn.setPreferredSize(new Dimension(40, 40));
                btn.setMinimumSize(new Dimension(36, 36));
                btn.setFont(new Font("Dialog", Font.BOLD, 14));
                btn.setMargin(new Insets(0, 0, 0, 0));
                btn.setFocusPainted(false);

                // 버튼 색
                btn.setBackground(new Color(240, 240, 240));
                btn.setForeground(Color.BLACK);

                btn.addActionListener(new LetterButtonListener(ch, btn));

                int idx = ch - 'A';
                if (idx >= 0 && idx < 26) {
                    letterButtons[idx] = btn;
                }

                gbc.gridx = c + offset;  // 가운데 정렬용 offset
                gbc.gridy = r;

                keyboardPanel.add(btn, gbc);
            }
        }

        keyboardPanel.revalidate();
        keyboardPanel.repaint();
    }

    // =========================================
    // 실제 키보드 입력 (A~Z) 연동
    // =========================================
    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        for (char c = 'A'; c <= 'Z'; c++) {
            String key = "KEY_" + c;

            im.put(KeyStroke.getKeyStroke(c),                        key);
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
            // 네트워크 모드: 직접 처리 X, 서버로 보내라고 알림
            netListener.onLetterChosen(Character.toLowerCase(ch));
        } else {
            // 로컬 모드: 그대로 처리
            onLetterSelected(Character.toLowerCase(ch), btn);
        }
    }


    // =========================================
    // 새 게임 (랜덤 시작)
    // =========================================
    public void startNewGameRandom() {
        Random rand = new Random();
        int w = rand.nextInt(WORDS.length);
        int t = rand.nextInt(THEMES.length);
        startNewGameFromIndex(w, t);
    }

    // 나중에 네트워크 동기화용으로도 쓸 수 있는 메서드
    public void startNewGameFromIndex(int wordIdx, int themeIdx) {
        answer = WORDS[wordIdx].toLowerCase();
        current = new char[answer.length()];

        for (int i = 0; i < current.length; i++) current[i] = ' ';
        for (int i = 0; i < used.length; i++)    used[i]    = false;

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
            if (current[i] == ' ') {
                w.append("  ");
            } else {
                w.append(current[i]).append(' ');
            }
            u.append("_ ");
        }

        lblWord.setText(w.toString());
        lblUnder.setText(u.toString());
    }

    // =========================================
    // 글자 선택 처리 (버튼/키보드 공통)
    // =========================================
    private void onLetterSelected(char ch, JButton btn) {
        // 대문자든 소문자든 무조건 소문자로 통일
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
            if (mistakes >= maxMistakes) {
                onLose();
            }
        } else {
            updateWordLabel();
            if (isAllRevealed()) {
                onWin();
            }
        }
    }

    private boolean isAllRevealed() {
        for (char c : current) {
            if (c == ' ') return false;
        }
        return true;
    }

    // =========================================
    // WIN / LOSE / END
    // =========================================
    private void onWin() {
        score++;
        lblScore.setText(String.format("Score: %02d", score));
        showEndDialog(true);
    }
    private void onLose() {
        showEndDialog(false);
    }

    // 게임 종료 공통 다이얼로그
// win == true  → 성공
// win == false → 실패
    private void showEndDialog(boolean win) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "게임 종료", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String titleText  = win ? "성공!" : "실패!";
        String detailText = "정답: " + answer;

        JLabel msg = new JLabel(
                "<html><center>" + titleText + "<br/>" + detailText + "</center></html>",
                SwingConstants.CENTER
        );
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.setFont(new Font("Dialog", Font.PLAIN, 14));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton restartBtn = new JButton("재시작하기");
        restartBtn.setBackground(new Color(60, 179, 113));
        restartBtn.setForeground(Color.BLACK);
        restartBtn.setFocusPainted(false);

        JButton exitBtn = new JButton("나가기");
        exitBtn.setBackground(new Color(190, 70, 60));
        exitBtn.setForeground(Color.BLACK);
        exitBtn.setFocusPainted(false);

        // 로컬 / 네트워크 모드에 따라 동작 나누기
        if (networkMode && netListener != null) {
            // ★ 멀티 플레이 모드

            // 재시작: 서버에 "이 방 다시 시작" 요청
            restartBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    netListener.onRestartRequested();  // ChatRoom이 /hangStart roomId 보냄
                }
            });

            // 나가기: 서버에 /hangEnd 보내고, 내 창 닫기
            exitBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    netListener.onGameEnd();  // /hangEnd roomId
                    onEndGame();              // 내 창 닫기
                }
            });

        } else {
            // ★ 싱글(로컬) 모드
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
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }



//    private void onLose() {
//        // ★ 커스텀 JDialog로 버튼 색 지정
//        Window parent = SwingUtilities.getWindowAncestor(this);
//        JDialog dialog = new JDialog(parent, "LOSE", Dialog.ModalityType.APPLICATION_MODAL);
//        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//
//        JPanel panel = new JPanel();
//        panel.setBackground(Color.WHITE);
//        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//
//        JLabel msg = new JLabel("<html>LOSE!<br/>정답: " + answer + "</html>", SwingConstants.CENTER);
//        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
//        msg.setFont(new Font("Dialog", Font.PLAIN, 14));
//
//        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
//        btnPanel.setBackground(Color.WHITE);
//
//        JButton restartBtn = new JButton("Restart");
//        restartBtn.setBackground(new Color(60, 179, 113));
//        restartBtn.setForeground(Color.BLACK);
//        restartBtn.setFocusPainted(false);
//
//        JButton endBtn = new JButton("End");
//        endBtn.setBackground(new Color(190, 70, 60));
//        endBtn.setForeground(Color.BLACK);
//        endBtn.setFocusPainted(false);
//
//        // 리스타트 눌렀을 때
//        restartBtn.addActionListener(e -> {
//            dialog.dispose();
//            startNewGameRandom();
//        });
//
//        // 엔드 눌렀을 때
//        endBtn.addActionListener(e -> {
//            dialog.dispose();
//            onEndGame();
//        });
//
//        btnPanel.add(restartBtn);
//        btnPanel.add(endBtn);
//
//        panel.add(msg);
//        panel.add(Box.createVerticalStrut(10));
//        panel.add(btnPanel);
//
//        dialog.setContentPane(panel);
//        dialog.pack();
//        dialog.setLocationRelativeTo(this);
//        dialog.setResizable(false);
//        dialog.setVisible(true);
//    }

    private void onEndGame() {
        // 나중에 여기서 채팅방에 "행맨게임을 종료했습니다." 보내도 됨
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) w.dispose();
    }

    // =========================================
    // 행맨 그림 그리는 패널 (머리는 이미지)
    // =========================================
    private static class HangmanDrawingPanel extends JPanel {
        private int mistakes = 0;
        private int max = 6;

        // ★ 머리 이미지
        private Image headImage;

        public HangmanDrawingPanel() {
            // 프로젝트 기준 경로: src/icons/tomato_face.png
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

            int offset = 20;  // ★ 전체를 아래로 20px 내리기
            int dx=30;

            // 바닥 + 기둥 + 가로대 + 줄
            g2.drawLine(30, h - 20 + offset, w - 30, h - 20 + offset); // 바닥
            g2.drawLine(60+dx, h - 20 + offset, 60+dx, -10 + offset);        // 기둥
            g2.drawLine(60+dx, -10 + offset, w / 2+dx, -10 + offset);        // 상단 가로
            g2.drawLine(w / 2+dx, -10 + offset, w / 2+dx, 0 + offset);       // 매달린 줄

            int cx=w/2+dx;
            
            // ===== 사람 (선 먼저) =====
            // 몸통
            if (mistakes >= 2)
                g2.drawLine(cx, 24 + offset, cx, 45 + offset);

            // 팔
            if (mistakes >= 3)
                g2.drawLine(cx, 30 + offset, cx-20, 40 + offset);   // 왼팔

            if (mistakes >= 4)
                g2.drawLine(cx, 30 + offset, cx+20, 40 + offset);   // 오른팔

            // 다리
            if (mistakes >= 5)
                g2.drawLine(cx, 45 + offset, cx-15, 60 + offset);   // 왼다리

            if (mistakes >= 6)
                g2.drawLine(cx, 45 + offset, cx+ 15, 60 + offset);   // 오른다리

            // ★ 머리: tomato_face.png (선 위에 마지막으로 그려서 겹쳐도 예쁨)
            if (mistakes >= 1 && headImage != null) {
                int headW = 50;
                int headH = 30;
                int headX = cx - headW / 2;
                int headY = 0 + offset;
                g2.drawImage(headImage, headX, headY, headW, headH, this);
            }
        }
    }
    // 네트워크로부터 받은 글자 적용 (버튼 상태도 같이 처리)
    public void applyGuessFromNetwork(char ch) {
        ch = Character.toLowerCase(ch);
        int idx = ch - 'a';
        if (idx < 0 || idx >= 26) return;

        JButton btn = letterButtons[idx];
        if (btn == null || !btn.isEnabled()) return;

        onLetterSelected(ch, btn);
    }


    // =========================================
    // 키보드 버튼 리스너
    // =========================================
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
                onLetterSelected(ch, btn);
            }
        }
    }

}

