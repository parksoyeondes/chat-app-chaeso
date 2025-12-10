import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.awt.Color;


public class ChatRoom extends JFrame{
    private String roomId;
    private ClientNet clientNet;
    //이모지
    private Map<String, ImageIcon> emojiMap = new HashMap<>();
    //입력창 + 버튼
    private JTextField txtInput;
    private JButton btnSend;
    private JDialog emojiDialog;
    // 말풍선들이 쌓이는 패널
    private JPanel messagePanel;
    private JScrollPane scrollPane;

    public ChatRoom(String roomId, ClientNet clientNet){
        // 채팅방 생성 체크박스에서 체크된것들 가져옴
        this.roomId = roomId;
        this.clientNet = clientNet;
        // 이모티콘 32x32 사이즈
        int EMOJI_SIZE = 60;
        //이모티콘 용 이모지 등록
        emojiMap.put(":emoj1:", loadEmoji("/icons/emoj1.png", EMOJI_SIZE));
        emojiMap.put(":emoj2:", loadEmoji("/icons/emoj2.png", EMOJI_SIZE));
        emojiMap.put(":emoj3:", loadEmoji("/icons/emoj3.png", EMOJI_SIZE));
        emojiMap.put(":emoj4:", loadEmoji("/icons/emoj4.png", EMOJI_SIZE));
        emojiMap.put(":emoj5:", loadEmoji("/icons/emoj5.png", EMOJI_SIZE));
        emojiMap.put(":emoj6:", loadEmoji("/icons/emoj6.png", EMOJI_SIZE));
        emojiMap.put(":emoj7:", loadEmoji("/icons/emoj7.png", EMOJI_SIZE));

        // 이제 GUI 올리기 -------------------
        setTitle("채팅방 - " + roomId);
        setSize(400, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // ---- 채팅 출력용 패널 ----
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        //--------아래 패널 -> 입력창 + 전송 + 이모지 ----------
        JPanel bottom = new JPanel(new BorderLayout());
        txtInput = new JTextField();
        btnSend = new JButton("전송");
        JButton btnEmoji = new JButton("😊");

        bottom.add(btnEmoji, BorderLayout.WEST);
        bottom.add(txtInput, BorderLayout.CENTER);
        bottom.add(btnSend, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        txtInput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        btnEmoji.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showEmojiPicker();   // 이제 선택창 띄우기
            }
        });
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // 창 닫으면 그냥 숨기기만 하기 -> 데이터 보존 위해
        setVisible(true);
    }

    //-----------------------말풍선 채팅을 위한 클래스 ---------------------
    class MessageBubble extends JPanel {
        private String text;
        private boolean isMine; // true: 내 말(오른쪽 파랑), false: 상대 말(왼쪽 회색)

        public MessageBubble(String text, boolean isMine) {
            this.text = text;
            this.isMine = isMine;
            setOpaque(false); // 배경 직접 그릴 거라 투명
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 위아래 간격
        }

        //========= 텍스트에 맞춰서 말풍선 크기 제작하기 =======
        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            int padding = 12;
            int tailSize = 8; // 말풍선 옆에 삼각형 꼬리

            int bubbleWidth = textWidth + padding * 2 + tailSize;
            int bubbleHeight = textHeight + padding * 2;

            return new Dimension(bubbleWidth + 10, bubbleHeight + 10);
        }

        //========= 실제 말풍선 그리기 ===========
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
                // 내 말 : 오른쪽 파란 말풍선
                x = getWidth() - bubbleWidth - tailSize - 5;

                g2.setColor(new Color(0, 132, 255)); // 파랑
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
                // 상대 말 : 왼쪽 회색 말풍선
                x = tailSize + 5;

                g2.setColor(new Color(230, 230, 230)); // 연회색
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

    //----------------메시지 보내기 함수들 --------------------------------
    private void sendMessage() {
        String msg = txtInput.getText().trim();
        //내가 친 걸 담아서
        if (msg.isEmpty()) {
            return;
        }
        // 각 멤버별 채팅방을 위한 메세지 보내기
        clientNet.SendMessage("/roomMsg " + roomId + " " + msg);
        txtInput.setText("");
    }

    //서버에서 메시지 받았을 떄 호출되는 함수
    //텍스튼는 말풍선과 함께 출력됨
    public void appendMessage(String senderName, String body) {
        boolean isMine = senderName != null && senderName.equals(clientNet.getUsername());

        // 이모지 코드일 때
        ImageIcon emoji = emojiMap.get(body);
        if (emoji != null) {
            appendEmoji(isMine, emoji); // 아이콘만 찍기
            return;
        }

        // 일반 텍스트면 말풍선 만들기
        String displayMsg;
        if (isMine) {
            displayMsg = body;
        } else {
            displayMsg = "[" + senderName + "] " + body;
        }

        // 말풍선 하나 생성
        MessageBubble bubble = new MessageBubble(displayMsg, isMine);

        // 왼쪽/오른쪽 정렬을 위해 한 줄 래퍼 패널 사용
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        if (isMine) {
            line.add(bubble, BorderLayout.EAST);
        } else {
            line.add(bubble, BorderLayout.WEST);
        }

        // 메시지들 서로간의 높이 간격 조절하기..
        Dimension pref = line.getPreferredSize();
        // 가로는 마음껏 늘어나도 되고, 세로(높이)만 고정
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

    private void sendEmoticon(String code) {
        // 그냥 텍스트 메시지처럼 서버에 보냄
        clientNet.SendMessage("/roomMsg " + roomId + " " + code);
    }

    // 이모티콘은 말풍선 없이 아이콘만 놓기
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

        // 메시지들 서로간의 높이 간격 조절하기..
        Dimension pref = line.getPreferredSize();
        // 가로는 마음껏 늘어나도 되고, 세로(높이)만 고정
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            }
        });
    }


    //----------------이모지 선택창 띄우기----------------
    private void showEmojiPicker() {
        if (emojiDialog == null) {
            emojiDialog = new JDialog(this, "이모티콘", false); // 부모: 이 채팅방, 모달 아님
            emojiDialog.setLayout(new GridLayout(2, 4, 5, 5)); // 2행 4열 예시

            // 이모지 코드들 배열로 한 번에 처리
            String[] codes = {":emoj1:", ":emoj2:", ":emoj3:", ":emoj4:",
                    ":emoj5:", ":emoj6:", ":emoj7:"};

            for (String code : codes) {
                ImageIcon icon = emojiMap.get(code);

                JButton btn;
                if (icon != null) {
                    btn = new JButton(icon);   // ← 아이콘만 달린 버튼 = 미리보기
                } else {
                    btn = new JButton(code);   // 아이콘 못 찾았을 때만 텍스트
                }

                btn.setMargin(new Insets(2, 2, 2, 2));
                btn.addActionListener(e -> {
                    sendEmoticon(code);        // 클릭하면 곧장 전송
                });

                emojiDialog.add(btn);
            }
            emojiDialog.pack();
        }
        // 채팅방 근처에 위치시키기 (대충 아래쪽)
        Point p = this.getLocationOnScreen();
        emojiDialog.setLocation(p.x + 50, p.y + this.getHeight() - emojiDialog.getHeight() - 50);
        emojiDialog.setVisible(true);
    }

    //-------------이모티콘 이미지 축소시키기-------------------
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


}
