import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// 하나의 채팅방 창을 담당하는 클래스.
//- roomId(예: "손채림,박소연") 기준으로 생성

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

    // ---------------------- 생성자 : 채팅방 창 만들기 ----------------------
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
        btnSend = new JButton("Send");
        btnSend.setPreferredSize(new Dimension(50, 28));
        btnSend.setBackground(new Color(190, 70, 60));
        btnSend.setForeground(Color.WHITE);
        btnSend.setOpaque(true);
        btnSend.setFocusPainted(false);
        btnSend.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnSend.setBorderPainted(true);

        // 이모지 버튼
        JButton btnEmoji = new JButton("😊");
        btnEmoji.setPreferredSize(new Dimension(28, 28));
        btnEmoji.setBackground(new Color(240, 240, 240));
        btnEmoji.setOpaque(true);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnEmoji.setBorderPainted(true);

        // 게임 버튼
        JButton btnGame = new JButton("Game");
        btnGame.setPreferredSize(new Dimension(50, 28));
        btnGame.setBackground(new Color(200, 200, 200));
        btnGame.setOpaque(true);
        btnGame.setFocusPainted(false);
        btnGame.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnGame.setBorderPainted(true);

        // 첨부 버튼 (+)
        btnAttach = new JButton("+");
        btnAttach.setPreferredSize(new Dimension(28, 28));
        btnAttach.setBackground(new Color(240, 240, 240));
        btnAttach.setOpaque(true);
        btnAttach.setFocusPainted(false);
        btnAttach.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
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

        // ======== 리스너들 =========
        // 메시지 전송 버튼
        btnSend.addActionListener(e -> sendMessage());
        txtInput.addActionListener(e -> sendMessage());

        //게임 시작 버튼
        btnGame.addActionListener(e ->
                clientNet.SendMessage("/hangStart " + roomId)
        );

        // 이모티콘 전송 버튼
        btnEmoji.addActionListener(e -> showEmojiPicker());

        // 첨부 버튼: 파일 선택 후 clientNet.sendImage 호출
        btnAttach.addActionListener(e -> openImageFileChooser());

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setVisible(true);
    }

    // ---------------------- 방 제목 만들기 ----------------------
    // 방 제목을 roomId로부터 만들어서 "나 자신을 제외한 다른 멤버들의 닉네임들"로 구성
    //  - 나 포함 1:1 방이면 상대방 한 명만
    //  - 그룹이면 여러 명을 콤마로 연결
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

    // -------------------------- 말풍선 패널 클래스 ----------------------------
    // 텍스트 메시지를 말풍선 모양으로 그려주는 컴포넌트
    class MessageBubble extends JPanel {
        private String text; // 실제 표시할 텍스트
        private boolean isMine; // 내가 보낸 메시지인지 여부(오른쪽/녹색)

        public MessageBubble(String text, boolean isMine) {
            this.text = text;
            this.isMine = isMine;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        // 레이아웃이 적당한 크기를 잡을 수 있도록 말풍선 크기 계산 메소드 -----------
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

        // 실제 말풍선(둥근 사각형 + 꼬리) 그리기 GUI -----------------
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
                // -------- 내가 보낸 메시지: 오른쪽 정렬 + 초록색 말풍선 ---------
                x = getWidth() - bubbleWidth - tailSize - 5;

                g2.setColor(new Color(46, 139, 87));
                g2.fillRoundRect(x, y, bubbleWidth, bubbleHeight, arc, arc);

                int baseY = y + bubbleHeight - arc / 2;
                // 오른쪽 꼬리(삼각형)
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
                //----------- 상대가 보낸 메시지: 왼쪽 정렬 + 회색 말풍선 ----------
                x = tailSize + 5;

                g2.setColor(new Color(230, 230, 230));
                g2.fillRoundRect(x, y, bubbleWidth, bubbleHeight, arc, arc);

                int baseY = y + bubbleHeight - arc / 2;
                // 왼쪽 꼬리(삼각형)
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

    // ================================  메시지 전송( 텍스트,이모지) ==============================

    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;

        // 서버에 "/roomMsg roomId 실제메시지" 형태로 전송
        clientNet.SendMessage("/roomMsg " + roomId + " " + msg);
        txtInput.setText("");
    }

    // ---------------- 서버에서 텍스트 메시지 수신 시 호출 ----------------

    public void appendMessage(String senderName, String body) {
        // senderName이 내 아이디와 같으면 내가 보낸 메시지
        boolean isMine = senderName != null && senderName.equals(clientNet.getUsername());

        // 만약 서버로부터 받은 body가 이모티콘 코드(:emoj1: 등)이면 이모티콘으로 처리 --------
        ImageIcon emoji = emojiMap.get(body);
        if (emoji != null) {
            appendEmoji(isMine, emoji);
            return;
        }//-----------------------------

        // 실제 말풍선에 보여줄 문자열 만들기
        String displayMsg;
        if (isMine) {
            // 내가 보낸 메시지는 이름 없이 본문만
            displayMsg = body;
        } else {
            String showName = senderName;
            if (senderName != null && clientNet != null) {
                showName = clientNet.getDisplayName(senderName);
            }
            displayMsg = "[" + showName + "] " + body;
        }

        // 말풍선 컴포넌트 생성
        MessageBubble bubble = new MessageBubble(displayMsg, isMine);
        // 한 줄에 말풍선 하나 올려놓을 패널 (오른쪽/왼쪽 정렬용)
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        if (isMine) {
            line.add(bubble, BorderLayout.EAST);
        } else {
            line.add(bubble, BorderLayout.WEST);
        }

        // 가로 폭은 최대, 세로는 내용만큼
        Dimension pref = line.getPreferredSize();
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        // 실제 채팅 내용이 쌓이는 messagePanel에 추가 → UI 갱신
        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();

        // 항상 스크롤을 제일 아래로 내리기
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // -----------  이모티콘 보내기 : 텍스트 코드(:emoj1:)를 프로토콜 메시지로 보내는 메소드 ----------------
    private void sendEmoticon(String code) {
        clientNet.SendMessage("/roomMsg " + roomId + " " + code);
    }

    // ---------------- 이모티콘(이미지) 말풍선 추가 ----------------
    private void appendEmoji(boolean isMine, ImageIcon icon) {
        //실제 채팅방에 이모티콘 ( = 이미지 ) 를 올리기 위함
        JLabel label = new JLabel(icon);

        //이미지 담을 패널
        JPanel bubblePanel = new JPanel();
        bubblePanel.setOpaque(false);
        bubblePanel.add(label);

        // 한 줄 단위로 이모티콘 올리기 위함.
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        if (isMine) {
            line.add(bubblePanel, BorderLayout.EAST); // 한 줄에 왼쪽 / 오른쪽 나누어 올림
        } else {
            line.add(bubblePanel, BorderLayout.WEST);
        }

        //그 한 줄의 높이와 폭 설정
        Dimension pref = line.getPreferredSize();
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        //실제 채팅 내용이 쌓이는 messagePanel에 한 줄 추가함
        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();

        //스크롤은 항상 아래로
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // -------------------------- 이모지 선택하는 창 생성됨 -----------------------------

    private void showEmojiPicker() {
        // 이 안에서 각 이모지마다 버튼을 만들고 버튼 클릭시 전송됨
        if (emojiDialog == null) {
            emojiDialog = new JDialog(this, "Emoji", false);

            // 바깥 여백 + 배경색을 주기 위한 외곽 패널
            JPanel outer = new JPanel(new BorderLayout());
            outer.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12)); // [추가] 바깥 여백
            outer.setBackground(new Color(210, 210, 210));                    // [추가] 회색 배경

            // 이모티콘 버튼을 배치할 그리드 패널
            JPanel grid = new JPanel(new GridLayout(2, 4, 5, 5));
            grid.setOpaque(false); // 바깥 회색 배경이 보이게

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

                // 버튼 UI 스타일 정리
                btn.setBackground(Color.WHITE);
                btn.setOpaque(true);
                btn.setFocusPainted(false);

                // 해당 이모지 버튼을 클릭하면 해당 코드(:emoj1:)를 메시지로 전송함
                btn.addActionListener(e -> sendEmoticon(code));

                grid.add(btn);
            }

            // 외곽 패널에 그리드 패널 부착
            outer.add(grid, BorderLayout.CENTER);

            // 다이얼로그 콘텐츠를 outer로 교체
            emojiDialog.setContentPane(outer);
            emojiDialog.pack();
        }

        // 채팅방 창 근처에 이모티콘 창 위치시키기
        Point p = this.getLocationOnScreen();
        emojiDialog.setLocation(
                p.x + 50,
                p.y + this.getHeight() - emojiDialog.getHeight() - 50
        );
        emojiDialog.setVisible(true);
    }


    // -----------  리소스에서 이모티콘 아이콘을 읽고 지정된 크기로 스케일링하는 메소드 -------------
    private ImageIcon loadEmoji(String path, int size) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            System.out.println("Can't find Emoji resource: " + path);
            return null;
        }
        ImageIcon icon = new ImageIcon(url);
        Image img = icon.getImage();
        Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // ==================================== 첨부 파일 이미지 ====================================

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
        // 여기서 채팅방 폭에 맞게 스케일링
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


    // =================================== 행맨 게임 ====================================

    // 서버에서 "/hangStart roomId wordIdx themeIdx" 수신 시 호출됨

    public void openHangman(int wordIdx, int themeIdx) {
        if (hangmanDialog == null) {
            //  네트워크로 이벤트 보내는 리스너 만들어서 주입
            hangmanPanel = new HangmanPanel(
                    new HangmanPanel.HangmanNetListener() {
                        @Override
                        public void onLetterChosen(char ch) {
                            // 사용자가 행맨에서 글자를 고르면 서버에 /hangGuess 전송
                            clientNet.SendMessage("/hangGuess " + roomId + " " + ch);
                        }

                        @Override
                        public void onGameEnd() {
                            // 게임이 끝나면 서버에 /hangEnd 전송 → 방 전체 종료
                            clientNet.SendMessage("/hangEnd " + roomId);
                        }

                        @Override
                        public void onRestartRequested() {
                            // 재시작 요청 시 서버에 /hangStart 전송 → 서버가 다시 랜덤 단어 뽑음
                            clientNet.SendMessage("/hangStart " + roomId);
                        }
                    },
                    true // 네트워크 모드 true
            );

            //행맨 게임 창을 만들고 보여줌 UI 띄우는거
            hangmanDialog = new JDialog(this, "Hangman - " + roomId, false);
            hangmanDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            hangmanDialog.getContentPane().add(hangmanPanel);
            hangmanDialog.setSize(460, 630);
            hangmanDialog.setResizable(false);
            hangmanDialog.setLocationRelativeTo(this);
        }

        // 서버에서 내려준 인덱스(wordIdx, themeIdx)로 새 게임 시작
        hangmanPanel.startNewGameFromIndex(wordIdx, themeIdx);
        hangmanDialog.setVisible(true);
        hangmanDialog.toFront();
    }

    // "/hangGuess roomId ch" 수신 시: 내 로컬 행맨 패널에 적용
    public void applyHangmanGuess(char ch) {
        if (hangmanPanel != null) {
            hangmanPanel.applyGuessFromNetwork(ch);
        }
    }

    // "/hangEnd roomId" 수신 시: 행맨 창 닫기
    public void closeHangman() {
        if (hangmanDialog != null) {
            hangmanDialog.setVisible(false);
        }
    }
}