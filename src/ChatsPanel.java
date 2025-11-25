// ChatsPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


public class ChatsPanel extends JPanel implements TabView {

    private DefaultListModel<String> model = new DefaultListModel<>();
    private JList<String> chatList = new JList<>(model);
    private FriendsPanel friendsPanel; // 유저 리스트 패널에서 프렌즈리스트 가져올거임

    public ChatsPanel() {

        // 전체 패널 설정
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);   // 전체 흰색


        // 1) 상단 타이틀  =======================

        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        // ====== 상단 타이틀 + 우측 + 버튼 ======
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false); // 배경 흰색이랑 섞이게

        JLabel lblTitle = new JLabel("Chats");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));

        JButton btnNewChat = new JButton("➕");
        btnNewChat.setFocusPainted(false);
        btnNewChat.setMargin(new Insets(2, 8, 2, 8));

        // ➕ 버튼 누를 때 기능
        btnNewChat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // 1) 현재 접속자 목록 (일단 예시, 나중에 실제로 가져오면 됨)
                String[] onlineUsers = { "손채림", "박소연", "아무개" };
                // TODO: 나중에는 FriendsPanel이나 서버에서 실제 접속자 리스트 받아와서 쓰기

                JFrame friendsFrame = new JFrame();
                friendsFrame.setSize(200, 250);
                friendsFrame.setLayout(new BorderLayout());

                JLabel chatTitle = new JLabel("대화 상대 추가");
                chatTitle.setFont(new Font("Dialog", Font.BOLD, 15));
                //음 이거 올려야 하는데

                // 유저 선택 패널
                JPanel centerPanel = new JPanel();
                centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
                centerPanel.setBackground(Color.WHITE);
                centerPanel.add(chatTitle);

                List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

                for (int i = 0; i < onlineUsers.length; i++) {
                    String name = onlineUsers[i];
                    JCheckBox cb = new JCheckBox(name);
                    cb.setBackground(Color.WHITE);
                    checkBoxes.add(cb);
                    centerPanel.add(cb);
                }

                JScrollPane scroll = new JScrollPane(centerPanel);
                scroll.getViewport().setBackground(Color.WHITE);
                scroll.setBorder(new EmptyBorder(10, 10, 10, 10));

                friendsFrame.add(scroll, BorderLayout.CENTER);

                // 4) 아래 확인 / 취소 버튼
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton btnCancel = new JButton("취소");
                JButton btnOk = new JButton("확인");

                bottomPanel.add(btnCancel);
                bottomPanel.add(btnOk);

                friendsFrame.add(bottomPanel, BorderLayout.SOUTH);

                // 5) 버튼 이벤트

                // 취소: 그냥 창 닫기
                btnCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        friendsFrame.dispose();
                    }
                });

                // 확인: 체크된 유저들 모아서 방 만들기 + 대화창 띄우기
                btnOk.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        java.util.List<String> selected = new java.util.ArrayList<String>();

                        for (int i = 0; i < checkBoxes.size(); i++) {
                            JCheckBox cb = checkBoxes.get(i);
                            if (cb.isSelected()) {
                                selected.add(cb.getText());
                            }
                        }

                        if (selected.isEmpty()) {
                            JOptionPane.showMessageDialog(friendsFrame, "한 명 이상 선택해 주세요.");
                            return;
                        }

                        // 방 생성 + 채팅창 열기
                        //createChatWindow(selected);

                        friendsFrame.dispose();
                    }
                });

                friendsFrame.setVisible(true);

            }
        });


        titleRow.add(lblTitle, BorderLayout.WEST);
        titleRow.add(btnNewChat, BorderLayout.EAST);

        topArea.add(titleRow);
        topArea.add(Box.createVerticalStrut(10));
        add(topArea, BorderLayout.NORTH);

        // 2) 채팅방 리스트 + 주변 흰색 처리  =======================

        chatList.setFixedCellHeight(44);
        chatList.setFont(new Font("Dialog", Font.PLAIN, 14));
        chatList.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(chatList);

        // 스크롤
        scroll.setBorder(null);
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);

        // 회색 안 보이게, 흰색 패널로 한 번 감싸고 여백 주기
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(Color.WHITE);                     // ★ 여기도 흰색
        centerWrapper.setBorder(new EmptyBorder(5, 15, 15, 15));      // 바깥 여백
        centerWrapper.add(scroll, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);

    }


    @Override
    public String getTitle() {
        return "Chats";
    }

    @Override
    public void refresh() {
        // 나중에 서버에서 채팅방 목록 갱신
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    public void addRoom(String name) {
        if (!model.contains(name)) {
            model.addElement(name);
        }
    }
}
