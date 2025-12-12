// ChatRoom.java
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ChatRoom extends JFrame {
    // 이 채팅방의 고유 ID (예: "손채림,박소연")
    private String roomId;

    // 서버와 연결되어 있는 네트워크 객체
    private ClientNet clientNet;

    // 이모지 코드(:emoj1: 등) → 이모지 아이콘 매핑
    private Map<String, ImageIcon> emojiMap = new HashMap<>();

    // 하단 입력창 + 전송 버튼
    private JTextField txtInput;
    private JButton btnSend;
    private JDialog emojiDialog;

    // 채팅 말풍선들이 쌓이는 패널
    private JPanel messagePanel;
    private JScrollPane scrollPane;

    // 행맨 게임용
    private JDialog hangmanDialog;
    private HangmanPanel hangmanPanel;

    // 첨부 버튼
    private JButton btnAttach;

    public ChatRoom(String roomId, ClientNet clientNet) {
        this.roomId = roomId;
        this.clientNet = clientNet;

        int EMOJI_SIZE = 60;

        emojiMap.put(":emoj1:", loadEmoji("/icons/emoj1.png", EMOJI_SIZE));
        emojiMap.put(":emoj2:", loadEmoji("/icons/emoj2.png", EMOJI_SIZE));
        emojiMap.put(":emoj3:", loadEmoji("/icons/emoj3.png", EMOJI_SIZE));
        emojiMap.put(":emoj4:", loadEmoji("/icons/emoj4.png", EMOJI_SIZE));
        emojiMap.put(":emoj5:", loadEmoji("/icons/emoj5.png", EMOJI_SIZE));
        emojiMap.put(":emoj6:", loadEmoji("/icons/emoj6.png", EMOJI_SIZE));
        emojiMap.put(":emoj7:", loadEmoji("/icons/emoj7.png", EMOJI_SIZE));

        // ------------------- 기본 창 세팅 -------------------
        String titleName = buildRoomTitle(roomId);
        setTitle("Chat - " + titleName);
        setSize(300, 400);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // ------------------- 채팅 내용 표시 영역 -------------------
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // ------------------- 하단 입력 영역 -------------------
        JPanel bottom = new JPanel(new BorderLayout());
        txtInput = new JTextField();

        // 전송 버튼
        btnSend = new JButton("send");
        btnSend.setPreferredSize(new Dimension(50, 28));
        btnSend.setBackground(new Color(190, 70, 60));
        btnSend.setForeground(Color.WHITE);
        btnSend.setOpaque(true);
        btnSend.setFocusPainted(false);
        btnSend.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        btnSend.setBorderPainted(true);

        // 이모지 버튼
        JButton btnEmoji = new JButton("😊");
        btnEmoji.setPreferredSize(new Dimension(28, 28));
        btnEmoji.setBackground(new Color(240, 240, 240));
        btnEmoji.setOpaque(true);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        btnEmoji.setBorderPainted(true);

        // 게임 버튼
        JButton btnGame = new JButton("Game");
        btnGame.setPreferredSize(new Dimension(50, 28));
        btnGame.setBackground(new Color(200, 200, 200));
        btnGame.setOpaque(true);
        btnGame.setFocusPainted(false);
        btnGame.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        btnGame.setBorderPainted(true);

        // 첨부 버튼 (+)
        btnAttach = new JButton("+");
        btnAttach.setPreferredSize(new Dimension(28, 28));
        btnAttach.setBackground(new Color(240, 240, 240));
        btnAttach.setOpaque(true);
        btnAttach.setFocusPainted(false);
        btnAttach.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        btnAttach.setBorderPainted(true);

        // 왼쪽에 첨부 + 이모티콘 + 게임 버튼
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(btnAttach);
        leftPanel.add(btnEmoji);
        leftPanel.add(btnGame);

        bottom.add(leftPanel, BorderLayout.WEST);
        bottom.add(txtInput, BorderLayout.CENTER);
        bottom.add(btnSend, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        // ===== 리스너들 =====
        btnSend.addActionListener(e -> sendMessage());

        btnGame.addActionListener(e ->
                clientNet.SendMessage("/hangStart " + roomId)
        );

        txtInput.addActionListener(e -> sendMessage());

        btnEmoji.addActionListener(e -> showEmojiPicker());

        // 첨부 버튼: 파일 선택 후 clientNet.sendImage 호출
        btnAttach.addActionListener(e -> openImageFileChooser());

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setVisible(true);
    }

    // 방 제목을 닉네임 기준으로 만들어주기
    private String buildRoomTitle(String roomId) {
        if (clientNet == null || roomId == null) return roomId;
        String me = clientNet.getUsername();
        String[] members = roomId.split(",");
        List<String> others = new ArrayList<>();

        for (String raw : members) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            if (me != null && trimmed.equals(me)) continue; // 나 자신은 빼고
            String disp = clientNet.getDisplayName(trimmed);
            others.add(disp);
        }

        if (others.isEmpty()) {
            // 혹시 혼자 있는 방이면 자기 이름이라도 보여주기
            return clientNet.getDisplayName(me);
        }
        if (others.size() == 1) {
            return others.get(0);
        }
        return String.join(", ", others);
    }

    // ----------------------- 말풍선 패널 클래스 -----------------------
    class MessageBubble extends JPanel {
        private String text;
        private boolean isMine;

        public MessageBubble(String text, boolean isMine) {
            this.text = text;
            this.isMine = isMine;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            int padding = 12;
            int tailSize = 8;

            int bubbleWidth = textWidth + padding * 2 + tailSize;
            int bubbleHeight = textHeight + padding * 2;

            return new Dimension(bubbleWidth + 10, bubbleHeight + 10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            int padding = 12;
            int tailSize = 8;
            int arc = 18;

            int bubbleWidth = textWidth + padding * 2;
            int bubbleHeight = textHeight + padding * 2;

            int y = 5;
            int x;

            if (isMine) {
                x = getWidth() - bubbleWidth - tailSize - 5;

                g2.setColor(new Color(46, 139, 87));
                g2.fillRoundRect(x, y, bubbleWidth, bubbleHeight, arc, arc);

                int baseY = y + bubbleHeight - arc / 2;
                Polygon tail = new Polygon(
                        new int[]{x + bubbleWidth, x + bubbleWidth + tailSize, x + bubbleWidth},
                        new int[]{baseY, baseY + tailSize / 2, baseY + tailSize},
                        3
                );
                g2.fill(tail);

                g2.setColor(Color.WHITE);
                int textX = x + padding;
                int textY = y + padding + fm.getAscent();
                g2.drawString(text, textX, textY);
            } else {
                x = tailSize + 5;

                g2.setColor(new Color(230, 230, 230));
                g2.fillRoundRect(x, y, bubbleWidth, bubbleHeight, arc, arc);

                int baseY = y + bubbleHeight - arc / 2;
                Polygon tail = new Polygon(
                        new int[]{x, x - tailSize, x},
                        new int[]{baseY, baseY + tailSize / 2, baseY + tailSize},
                        3
                );
                g2.fill(tail);

                g2.setColor(Color.BLACK);
                int textX = x + padding;
                int textY = y + padding + fm.getAscent();
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }
    }

    // ----------------------------- 메시지 전송 -----------------------------

    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;

        clientNet.SendMessage("/roomMsg " + roomId + " " + msg);
        txtInput.setText("");
    }

    // 서버에서 텍스트 메시지 수신 시 호출
    public void appendMessage(String senderName, String body) {
        boolean isMine = senderName != null && senderName.equals(clientNet.getUsername());

        ImageIcon emoji = emojiMap.get(body);
        if (emoji != null) {
            appendEmoji(isMine, emoji);
            return;
        }

        String displayMsg;
        if (isMine) {
            displayMsg = body;
        } else {
            String showName = senderName;
            if (senderName != null && clientNet != null) {
                showName = clientNet.getDisplayName(senderName);
            }
            displayMsg = "[" + showName + "] " + body;
        }

        MessageBubble bubble = new MessageBubble(displayMsg, isMine);

        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        if (isMine) {
            line.add(bubble, BorderLayout.EAST);
        } else {
            line.add(bubble, BorderLayout.WEST);
        }

        Dimension pref = line.getPreferredSize();
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void sendEmoticon(String code) {
        clientNet.SendMessage("/roomMsg " + roomId + " " + code);
    }

    private void appendEmoji(boolean isMine, ImageIcon icon) {
        JLabel label = new JLabel(icon);

        JPanel bubblePanel = new JPanel();
        bubblePanel.setOpaque(false);
        bubblePanel.add(label);

        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        if (isMine) {
            line.add(bubblePanel, BorderLayout.EAST);
        } else {
            line.add(bubblePanel, BorderLayout.WEST);
        }

        Dimension pref = line.getPreferredSize();
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // ==================== 첨부 이미지 ====================

    private void openImageFileChooser() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files (png, jpg, jpeg, gif, bmp)",
                "png", "jpg", "jpeg", "gif", "bmp"
        );
        chooser.setFileFilter(filter);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            // 화면에는 서버에서 브로드캐스트 받은 뒤 appendImage로 그림
            clientNet.sendImage(roomId, selectedFile);
        }
    }

    // 서버에서 받은 이미지 그리기
    public void appendImage(boolean isMine, ImageIcon icon) {
        // ★ 여기서 채팅방 폭에 맞게 스케일링
        int viewportWidth = scrollPane.getViewport().getWidth();
        if (viewportWidth <= 0) {
            // 레이아웃이 아직 안 잡힌 타이밍일 수 있으니 기본값
            viewportWidth = 220;
        }
        int maxWidth  = viewportWidth - 60;  // 좌우 여백 조금 빼기
        int maxHeight = 250;                 // 최대 높이 제한 (원하는대로 조절 가능)

        ImageIcon scaledIcon = scaleImageToFit(icon, maxWidth, maxHeight);

        JLabel label = new JLabel(scaledIcon);

        JPanel bubblePanel = new JPanel();
        bubblePanel.setOpaque(false);
        bubblePanel.add(label);

        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        if (isMine) {
            line.add(bubblePanel, BorderLayout.EAST);
        } else {
            line.add(bubblePanel, BorderLayout.WEST);
        }

        Dimension pref = line.getPreferredSize();
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // 실제 스케일링 로직
    private ImageIcon scaleImageToFit(ImageIcon src, int maxWidth, int maxHeight) {
        int w = src.getIconWidth();
        int h = src.getIconHeight();

        if (w <= 0 || h <= 0) return src;

        // 이미 충분히 작으면 그대로 사용
        if (w <= maxWidth && h <= maxHeight) {
            return src;
        }

        double scaleW = (double) maxWidth / w;
        double scaleH = (double) maxHeight / h;
        double scale  = Math.min(scaleW, scaleH);

        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        Image scaled = src.getImage().getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // -------------------------- 이모지 선택창 --------------------------
    private void showEmojiPicker() {
        if (emojiDialog == null) {
            emojiDialog = new JDialog(this, "Emoji", false);
            emojiDialog.setLayout(new GridLayout(2, 4, 5, 5));

            String[] codes = {
                    ":emoj1:", ":emoj2:", ":emoj3:", ":emoj4:",
                    ":emoj5:", ":emoj6:", ":emoj7:"
            };

            for (String code : codes) {
                ImageIcon icon = emojiMap.get(code);

                JButton btn;
                if (icon != null) {
                    btn = new JButton(icon);
                } else {
                    btn = new JButton(code);
                }

                btn.setMargin(new Insets(2, 2, 2, 2));
                btn.addActionListener(e -> sendEmoticon(code));

                emojiDialog.add(btn);
            }
            emojiDialog.pack();
        }

        Point p = this.getLocationOnScreen();
        emojiDialog.setLocation(
                p.x + 50,
                p.y + this.getHeight() - emojiDialog.getHeight() - 50
        );
        emojiDialog.setVisible(true);
    }

    private ImageIcon loadEmoji(String path, int size) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            System.out.println("이모티콘 리소스 못 찾음: " + path);
            return null;
        }
        ImageIcon icon = new ImageIcon(url);
        Image img = icon.getImage();
        Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    //----------------------------  행맨 게임 -----------------------------------

    public void openHangman(int wordIdx, int themeIdx) {
        if (hangmanDialog == null) {
            hangmanPanel = new HangmanPanel(
                    new HangmanPanel.HangmanNetListener() {
                        @Override
                        public void onLetterChosen(char ch) {
                            clientNet.SendMessage("/hangGuess " + roomId + " " + ch);
                        }

                        @Override
                        public void onGameEnd() {
                            clientNet.SendMessage("/hangEnd " + roomId);
                        }

                        @Override
                        public void onRestartRequested() {
                            clientNet.SendMessage("/hangStart " + roomId);
                        }
                    },
                    true // ★ 네트워크 모드
            );

            hangmanDialog = new JDialog(this, "Hangman - " + roomId, false);
            hangmanDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            hangmanDialog.getContentPane().add(hangmanPanel);
            hangmanDialog.pack();
            hangmanDialog.setLocationRelativeTo(this);
        }

        // ★ 여기서 딱 한 번만, 매번 서버가 준 인덱스로 새 게임 시작
        hangmanPanel.startNewGameFromIndex(wordIdx, themeIdx);

        hangmanDialog.setVisible(true);
        hangmanDialog.toFront();
    }


    public void applyHangmanGuess(char ch) {
        if (hangmanPanel != null) {
            hangmanPanel.applyGuessFromNetwork(ch);
        }
    }

    public void closeHangman() {
        if (hangmanDialog != null) {
            hangmanDialog.setVisible(false);
        }
    }
}
