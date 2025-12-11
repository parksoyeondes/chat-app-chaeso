import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.awt.Color;

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


    public ChatRoom(String roomId, ClientNet clientNet) {
        // 서버에서 받은 roomId (초대된 멤버 이름들을 조합한 문자열)
        this.roomId = roomId;
        this.clientNet = clientNet;

        // 이모티콘 아이콘 크기
        int EMOJI_SIZE = 60;

        // 이모지 코드에 대응되는 이미지 등록~
        emojiMap.put(":emoj1:", loadEmoji("/icons/emoj1.png", EMOJI_SIZE));
        emojiMap.put(":emoj2:", loadEmoji("/icons/emoj2.png", EMOJI_SIZE));
        emojiMap.put(":emoj3:", loadEmoji("/icons/emoj3.png", EMOJI_SIZE));
        emojiMap.put(":emoj4:", loadEmoji("/icons/emoj4.png", EMOJI_SIZE));
        emojiMap.put(":emoj5:", loadEmoji("/icons/emoj5.png", EMOJI_SIZE));
        emojiMap.put(":emoj6:", loadEmoji("/icons/emoj6.png", EMOJI_SIZE));
        emojiMap.put(":emoj7:", loadEmoji("/icons/emoj7.png", EMOJI_SIZE));

        // ------------------- 기본 창 세팅 -------------------
        setTitle("채팅방 - " + roomId);
        setSize(400, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // ------------------- 채팅 내용 표시 영역 -------------------
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS)); // 위에서 아래로 말풍선 쌓기
        messagePanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // ------------------- 하단 입력 영역 -------------------
        JPanel bottom = new JPanel(new BorderLayout());
        txtInput = new JTextField();
        btnSend = new JButton("전송");
        JButton btnEmoji = new JButton("😊"); // 이모티콘 선택 버튼
        JButton btnGame = new JButton("게임"); // 게임 시작 버튼

        // 왼쪽에 이모지 + 게임 버튼 두 개 배치
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(btnEmoji);
        leftPanel.add(btnGame);

        bottom.add(leftPanel, BorderLayout.WEST);
        bottom.add(txtInput, BorderLayout.CENTER);
        bottom.add(btnSend, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // 전송 버튼 클릭 → 메시지 보내기
        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // 게임 시작 버튼 액션
        btnGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 이 채팅방(roomId)에 게임 시작 요청
                clientNet.SendMessage("/hangStart " + roomId);
            }
        });

        // 엔터 치면 전송
        txtInput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // 이모지 버튼 클릭 → 이모지 선택창 띄우기
        btnEmoji.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showEmojiPicker();
            }
        });

        // X 눌러도 실제로는 종료가 아니라 숨기기만 함 (대화 내용 유지)
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setVisible(true);
    }

    // ----------------------- 말풍선 패널 클래스 -----------------------

     //한 줄짜리 말풍선(내 말 / 상대 말)을 그려주는 패널
    class MessageBubble extends JPanel {
        private String text;   // 말풍선 안에 들어갈 문자열
        private boolean isMine; // true: 내 메시지(오른쪽 파랑), false: 상대 메시지(왼쪽 회색)

        public MessageBubble(String text, boolean isMine) {
            this.text = text;
            this.isMine = isMine;
            setOpaque(false);
            // 위아래 여백 조금 주기
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        // ========= 텍스트 길이에 맞춰 말풍선 크기 계산 =========
        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            int padding = 12;
            int tailSize = 8; // 말풍선의 삼각형 꼬리 길이

            int bubbleWidth = textWidth + padding * 2 + tailSize;
            int bubbleHeight = textHeight + padding * 2;

            // 약간의 여백 포함
            return new Dimension(bubbleWidth + 10, bubbleHeight + 10);
        }

        // ========= 실제 말풍선과 꼬리, 텍스트 그리기 =========
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
            int arc = 18; // 말풍선 모서리 둥글기

            int bubbleWidth = textWidth + padding * 2;
            int bubbleHeight = textHeight + padding * 2;

            int y = 5; // 위쪽 위치
            int x;     // 왼쪽 위치 (내 말 / 상대 말에 따라 다름)

            if (isMine) {
                // =============== 내 말: 오른쪽 파란 말풍선 ===============
                x = getWidth() - bubbleWidth - tailSize - 5;

                // 말풍선 본체
                g2.setColor(new Color(0, 132, 255)); // 파란색
                g2.fillRoundRect(x, y, bubbleWidth, bubbleHeight, arc, arc);

                // 말풍선 꼬리 (오른쪽)
                int baseY = y + bubbleHeight - arc / 2;
                Polygon tail = new Polygon(
                        new int[]{x + bubbleWidth, x + bubbleWidth + tailSize, x + bubbleWidth},
                        new int[]{baseY, baseY + tailSize / 2, baseY + tailSize},
                        3
                );
                g2.fill(tail);

                // 텍스트(흰색)
                g2.setColor(Color.WHITE);
                int textX = x + padding;
                int textY = y + padding + fm.getAscent();
                g2.drawString(text, textX, textY);
            } else {
                // =============== 상대 말: 왼쪽 회색 말풍선 ===============
                x = tailSize + 5; // 왼쪽 여백 + 꼬리 공간

                // 말풍선 본체
                g2.setColor(new Color(230, 230, 230)); // 연회색
                g2.fillRoundRect(x, y, bubbleWidth, bubbleHeight, arc, arc);

                // 말풍선 꼬리 (왼쪽)
                int baseY = y + bubbleHeight - arc / 2;
                Polygon tail = new Polygon(
                        new int[]{x, x - tailSize, x},
                        new int[]{baseY, baseY + tailSize / 2, baseY + tailSize},
                        3
                );
                g2.fill(tail);

                // 텍스트(검정색)
                g2.setColor(Color.BLACK);
                int textX = x + padding;
                int textY = y + padding + fm.getAscent();
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }
    }

    // ---------------- 메시지 전송 관련 메서드 ----------------

    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) {
            return; // 빈 문자열은 전송 X
        }

        // 방 단위 메시지 프로토콜:
        //   /roomMsg {roomId} {메시지내용}
        clientNet.SendMessage("/roomMsg " + roomId + " " + msg);
        txtInput.setText("");
    }


     //===============   서버에서 메시지를 받았을 때 호출  ================
     //senderName, body를 기반으로 말풍선 / 이모지 출력

    public void appendMessage(String senderName, String body) {
        // senderName이 내 이름과 같으면 "내 메시지"로 처리
        boolean isMine = senderName != null && senderName.equals(clientNet.getUsername());

        // 이모지 코드(:emoj1: 등)인 경우 → 아이콘만 출력
        ImageIcon emoji = emojiMap.get(body);
        if (emoji != null) {
            appendEmoji(isMine, emoji);
            return;
        }

        // 일반 텍스트 메시지
        String displayMsg;
        if (isMine) {
            // 내 말은 이름 없이 내용만
            displayMsg = body;
        } else {
            // 상대 말은 [이름] + 내용
            displayMsg = "[" + senderName + "] " + body;
        }

        // 말풍선 패널 생성
        MessageBubble bubble = new MessageBubble(displayMsg, isMine);

        // 한 줄에 말풍선을 왼쪽/오른쪽에 붙이기 위한 래퍼 패널
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        if (isMine) {
            line.add(bubble, BorderLayout.EAST);   // 내 말 → 오른쪽 정렬
        } else {
            line.add(bubble, BorderLayout.WEST);   // 상대 말 → 왼쪽 정렬
        }

        // 세로 간격 고정을 위해 최대 가로 = 무한, 세로 = 자신의 높이
        Dimension pref = line.getPreferredSize();
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        // messagePanel에 추가
        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();

        // 항상 스크롤을 맨 아래로 내리기
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            }
        });
    }

    //이모지 코드(:emoj1: 등)를 서버로 전송
    //서버/다른 클라이언트에서는 body가 코드로 들어옴
    private void sendEmoticon(String code) {
        clientNet.SendMessage("/roomMsg " + roomId + " " + code);
    }

   //================  이모티콘 아이콘을 말풍선 없이 그대로 배치  ============
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

        // 스크롤 맨 아래로
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            }
        });
    }

    // ------------------- 이모지 선택창 (JDialog) ---------------------

    private void showEmojiPicker() {
        if (emojiDialog == null) {
            // 부모: 이 ChatRoom, 모달(false): 다른 작업도 가능
            emojiDialog = new JDialog(this, "이모티콘", false);
            emojiDialog.setLayout(new GridLayout(2, 4, 5, 5)); // 2행 4열 배치 예시

            String[] codes = {
                    ":emoj1:", ":emoj2:", ":emoj3:", ":emoj4:",
                    ":emoj5:", ":emoj6:", ":emoj7:"
            };

            for (String code : codes) {
                ImageIcon icon = emojiMap.get(code);

                JButton btn;
                if (icon != null) {
                    // 아이콘 미리 보기 버튼
                    btn = new JButton(icon);
                } else {
                    // 아이콘 못 찾았을 때 코드 텍스트로 표시
                    btn = new JButton(code);
                }

                btn.setMargin(new Insets(2, 2, 2, 2));
                btn.addActionListener(e -> {
                    // 버튼 클릭 시 바로 이모지 전송
                    sendEmoticon(code);
                });

                emojiDialog.add(btn);
            }
            emojiDialog.pack();
        }

        // 채팅방 위치 기준으로 대충 아래쪽에 띄우기
        Point p = this.getLocationOnScreen();
        emojiDialog.setLocation(
                p.x + 50,
                p.y + this.getHeight() - emojiDialog.getHeight() - 50
        );
        emojiDialog.setVisible(true);
    }

    // ---------------- 이모티콘 이미지 로딩 & 리사이즈 ----------------

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

    //                            ====================== 행맨 게임 ========================

    // ------------------------서버에서 /hangStart roomId wordIdx themeIdx 를 받았을 때 호출됨 -----------------------
    public void openHangman(int wordIdx, int themeIdx) {
        // 다이얼로그가 아직 없으면 한 번만 생성
        if (hangmanDialog == null) {
            hangmanPanel = new HangmanPanel(
                    new HangmanPanel.HangmanNetListener() {
                        @Override
                        public void onLetterChosen(char ch) {
                            // 내가 글자 선택 → 서버로 /hangGuess
                            clientNet.SendMessage("/hangGuess " + roomId + " " + ch);
                        }

                        @Override
                        public void onGameEnd() {
                            // 게임 나가기 → 서버로 /hangEnd
                            clientNet.SendMessage("/hangEnd " + roomId);
                        }

                        @Override
                        public void onRestartRequested() {
                            // 재시작하기 → 서버로 /hangStart
                            clientNet.SendMessage("/hangStart " + roomId);
                        }
                    },
                    true
            );


            hangmanDialog = new JDialog(this, "Hangman - " + roomId, false);
            hangmanDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            hangmanDialog.getContentPane().add(hangmanPanel);
            hangmanDialog.pack();
            hangmanDialog.setLocationRelativeTo(this);
        }

        // 서버가 준 인덱스로 같은 단어/테마로 시작
        hangmanPanel.startNewGameFromIndex(wordIdx, themeIdx);
        hangmanDialog.setVisible(true);
        hangmanDialog.toFront();
    }

    // -----------------------  서버에서 /hangGuess roomId c 받은 뒤 호출됨 --------------------------
    public void applyHangmanGuess(char ch) {
        if (hangmanPanel != null) {
            hangmanPanel.applyGuessFromNetwork(ch);
        }
    }

    // ------------------------  서버에서 /hangEnd roomId 받은 뒤 호출됨  ------------------
    public void closeHangman() {
        if (hangmanDialog != null) {
            hangmanDialog.setVisible(false);
        }
    }


}
