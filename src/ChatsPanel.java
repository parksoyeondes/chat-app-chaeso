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
    private ClientNet clientNet;

    public ChatsPanel() {

        // 전체 패널 설정
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);   // 전체 흰색

        // 상단 타이틀  =======================
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

        // 채팅방 생성 + 버튼 누를 때
        btnNewChat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // 현재 접속자 목록을 프렌즈패널에서 가져오기 빌리기?
                String[] chatUsers = friendsPanel.getFriendsList(); // <- 메서드 이름 맞게

                JFrame friendsFrame = new JFrame("대화 상대 추가");
                friendsFrame.setSize(250, 300);
                friendsFrame.setLayout(new BorderLayout());
                friendsFrame.setLocationRelativeTo(ChatsPanel.this);

                JLabel chatTitle = new JLabel("대화 상대 추가");
                chatTitle.setFont(new Font("Dialog", Font.BOLD, 15));

                // 유저 선택 패널 =============================== 채팅방 개설하기 위한 전 단계
                JPanel centerPanel = new JPanel();
                centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
                centerPanel.setBackground(Color.WHITE);
                centerPanel.add(chatTitle);
                centerPanel.add(Box.createVerticalStrut(8));

                List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();
                // 체크박스 형태로 유저 이름 모아두기

                for (int i = 0; i < chatUsers.length; i++) { // 가져온 명단으로 체크박스 만들기
                    String name = chatUsers[i];
                    if (name == null) continue;
                    String trimmed = name.trim();
                    if (trimmed.isEmpty()) continue;

                    JCheckBox box = new JCheckBox(trimmed);
                    box.setBackground(Color.WHITE);

                    checkBoxes.add(box);   // 데이터 모아두기
                    centerPanel.add(box);  // 이제 저 리스트를 GUI에 붙이기
                }

                JScrollPane scroll = new JScrollPane(centerPanel);
                scroll.getViewport().setBackground(Color.WHITE);
                scroll.setBorder(new EmptyBorder(10, 10, 10, 10));

                friendsFrame.add(scroll, BorderLayout.CENTER);

                // 아래 확인 / 취소 버튼 -----------------------
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton btnCancel = new JButton("취소");
                JButton btnOk = new JButton("확인");

                bottomPanel.add(btnCancel);
                bottomPanel.add(btnOk);
                friendsFrame.add(bottomPanel, BorderLayout.SOUTH);

                // 취소: 그냥 창 닫기
                btnCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        friendsFrame.dispose();
                    }
                });

                // 확인: 체크된 유저들 모아서 방 만들기
                btnOk.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        List<String> selectedUsers = new ArrayList<>(); //선택된 유저 이름 담기
                        for (int i = 0; i < checkBoxes.size(); i++) {
                            JCheckBox cb = checkBoxes.get(i);
                            if (cb.isSelected()) {               //  여기서 선택 여부 확인
                                selectedUsers.add(cb.getText());
                            }
                        }
                        if (selectedUsers.isEmpty()) {
                            //선택 안 할시 경고 문구 띄우기
                            JOptionPane.showMessageDialog(friendsFrame, "한 명 이상 선택해 주세요.");
                            return;
                        }
                        if (clientNet != null) {
                            String me = clientNet.getUsername();
                            if (!selectedUsers.contains(me)) {
                                selectedUsers.add(me);
                            }
                        }
                        // 일단 셀렉된 리스트 가지고 하나의 문자열 -> 방 제목 만들기 "손채림,박소연"
                        String roomId = String.join(",", selectedUsers);
                        // Chats 탭 리스트에 방 추가하기 ( GUI )
                        addRoom(roomId);
                        // 셀렉된 명단 리스트를 서버에게 보내기 ( 클라send 이용해서 )
                        clientNet.SendMessage("/openRoom " + roomId);
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
        chatList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {  // 더블클릭하기
                    String roomId = chatList.getSelectedValue();
                    if (roomId != null && clientNet != null) {
                        clientNet.openRoom(roomId);
                    }
                }
            }
        });


        JScrollPane scrollChat = new JScrollPane(chatList);

        // 스크롤
        scrollChat.setBorder(null);
        scrollChat.setBackground(Color.WHITE);
        scrollChat.getViewport().setBackground(Color.WHITE);

        // 회색 안 보이게, 흰색 패널로 한 번 감싸고 여백 주기
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(Color.WHITE);
        centerWrapper.setBorder(new EmptyBorder(5, 15, 15, 15));
        centerWrapper.add(scrollChat, BorderLayout.CENTER);

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

    public void setClientNet(ClientNet clientNet) {
        this.clientNet = clientNet;
    }

    // FriendsPanel을 주입하는 메소드
    public void setFriendsList(FriendsPanel friendsPanel) {
        this.friendsPanel = friendsPanel;
    }
}
