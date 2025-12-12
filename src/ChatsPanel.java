// ChatsPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

// "Chats" 탭에 해당하는 패널
public class ChatsPanel extends JPanel implements TabView {

    // 채팅방 목록을 관리하는 리스트 모델 (문자열 roomId들을 저장)
    private DefaultListModel<String> model = new DefaultListModel<>();
    // 실제로 화면에 보이는 채팅방 리스트
    private JList<String> chatList = new JList<>(model);

    // 친구 목록/프로필 정보를 가지고 있는 FriendsPanel (주입받음)
    private FriendsPanel friendsPanel;
    // 서버와 통신하는 네트워크 객체 (주입받음)
    private ClientNet clientNet;

    // -------------------- 생성자 : 기본 UI 구성 --------------------
    public ChatsPanel() {

        // 전체 패널 설정
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);   // 전체 흰색

        // ------------- 상단 타이틀 영역 ------------
        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        // ------------ "Chats" 라벨 + 우측 새 채팅 버튼 +  -----------
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Chats");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));

        // 새 채팅방 만들기 버튼(+) 설정
        JButton btnNewChat = new JButton("➕");
        btnNewChat.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnNewChat.setFocusPainted(false);
        btnNewChat.setMargin(new Insets(2, 8, 2, 8));
        btnNewChat.setBackground(new Color(230, 230, 230));
        btnNewChat.setOpaque(true);
        
        btnNewChat.setPreferredSize(new Dimension(35, 28));
        btnNewChat.setMinimumSize(new Dimension(35, 28));
        btnNewChat.setMaximumSize(new Dimension(35, 28));

        // ================== [새 채팅방 만들기] 버튼 이벤트 ==================
        btnNewChat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                if (friendsPanel == null) {
                    JOptionPane.showMessageDialog(ChatsPanel.this,
                            "Failed to load friend list.");
                    return;
                }

                // 현재 접속자 목록(실제 서버 아이디들)을 FriendsPanel에서 가져옴
                String[] chatUsers = friendsPanel.getFriendsList();

                // 대화상대 선택용 작은 프레임 띄우기
                JFrame friendsFrame = new JFrame("New Chat");
                friendsFrame.setSize(250, 300);
                friendsFrame.setLayout(new BorderLayout());
                friendsFrame.setLocationRelativeTo(ChatsPanel.this);

                JLabel chatTitle = new JLabel("Choose Friends");
                chatTitle.setFont(new Font("Dialog", Font.BOLD, 15));
                chatTitle.setBorder(new EmptyBorder(5, 5, 4, 10));

                // 가운데 영역: 체크박스로 유저 목록 뿌리기 =========================
                JPanel centerPanel = new JPanel();
                centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
                centerPanel.setBackground(Color.WHITE);
                centerPanel.setBorder(new EmptyBorder(5, 5, 10, 10));

                // 체크박스들을 배열로 관리해서 나중에 선택된 유저를 모을 때 사용
                List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

                // FriendsPanel에서 받은 유저 목록(chatUsers)을 바탕으로 체크박스 생성
                for (int i = 0; i < chatUsers.length; i++) {
                    String realName = chatUsers[i];
                    if (realName == null) continue;
                    
                    String trimmed = realName.trim();
                    if (trimmed.isEmpty()) continue;

                    // 화면에 보여줄 이름(닉네임) 얻기
                    String displayName = friendsPanel.getDisplayName(trimmed);

                    JCheckBox box = new JCheckBox(displayName);
                    box.setBackground(Color.WHITE);
                    // 실제 서버에서 사용하는 이름(실제 아이디)을 clientProperty로 따로 저장
                    box.putClientProperty("realName", trimmed);
                    
                    box.setFocusPainted(false);
                    box.setBorderPainted(false);
                    box.setOpaque(false);
                    box.setFocusable(false);

                    checkBoxes.add(box);
                    centerPanel.add(box);
                }

                // 유저 체크박스들이 많은 경우를 대비해서 스크롤 추가
                JScrollPane scroll = new JScrollPane(centerPanel);
                scroll.getViewport().setBackground(Color.WHITE);
                scroll.setBorder(new EmptyBorder(10, 10, 10, 10));
                
                friendsFrame.add(chatTitle, BorderLayout.NORTH);
                friendsFrame.add(scroll, BorderLayout.CENTER);

                // 아래쪽: "취소" / "확인" 버튼 영역 =========================
                Color panelBg = new Color(230, 230, 230);
                
                JPanel bottomPanel = new JPanel(new BorderLayout());
                bottomPanel.setBackground(panelBg);
                
                bottomPanel.setBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 210, 210))
                );
                
                JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
                leftPanel.setBackground(panelBg);

                JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
                rightPanel.setBackground(panelBg);
                
                // Cancel 버튼
                JButton btnCancel = new JButton("Cacel");
                btnCancel.setBackground(new Color(200, 200, 200));
                btnCancel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                btnCancel.setFocusPainted(false);
                btnCancel.setOpaque(true);
                btnCancel.setPreferredSize(new Dimension(80, 32));
                
                // Ok 버튼
                JButton btnOk = new JButton("OK");
                btnOk.setBackground(new Color(60, 179, 113));
                btnOk.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                btnOk.setFocusPainted(false);
                btnOk.setOpaque(true);
                btnOk.setPreferredSize(new Dimension(80, 32));

                leftPanel.add(btnCancel);
                rightPanel.add(btnOk);
                
                bottomPanel.add(leftPanel, BorderLayout.WEST);
                bottomPanel.add(rightPanel, BorderLayout.EAST);
                
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

                        // 선택된 유저들의 "실제 이름"을 담을 리스트
                        List<String> selectedUsers = new ArrayList<>();

                        // 모든 체크박스를 돌면서 선택된 것만 골라냄
                        for (int i = 0; i < checkBoxes.size(); i++) {
                            JCheckBox cb = checkBoxes.get(i);
                            if (cb.isSelected()) {
                                String real = (String) cb.getClientProperty("realName");
                                if (real != null && !real.trim().isEmpty()) {
                                    selectedUsers.add(real.trim());
                                }
                            }
                        }

                        // 한 명도 선택 안 했으면 경고
                        if (selectedUsers.isEmpty()) {
                            JOptionPane.showMessageDialog(friendsFrame, "Select at least one user.");
                            return;
                        }

                        // 본인(me)도 방 멤버에 자동 포함되도록 처리
                        if (clientNet != null) {
                            String me = clientNet.getUsername();
                            if (me != null && !me.trim().isEmpty() &&
                                    !selectedUsers.contains(me)) {
                                selectedUsers.add(me);
                            }
                        }

                        // 셀렉된 리스트로 방 제목 만들기 = roomId 문자열 생성 "손채림,박소연"
                        String roomId = String.join(",", selectedUsers);

                        // Chats 탭 리스트에 방 추가하기 ( GUI )
                        addRoom(roomId);

                        //서버에게도 "이 멤버들로 방 열어줘" 요청
                        if (clientNet != null) {
                            clientNet.SendMessage("/openRoom " + roomId);
                        }

                        // 선택 창 닫기
                        friendsFrame.dispose();
                    }
                });
                friendsFrame.setVisible(true);
            }
        });

        // 타이틀 행에 "Chats" 라벨과 + 버튼 배치
        titleRow.add(lblTitle, BorderLayout.WEST);
        titleRow.add(btnNewChat, BorderLayout.EAST);

        // 상단 영역에 타이틀 행 추가 + 아래 여백
        topArea.add(titleRow);
        topArea.add(Box.createVerticalStrut(20));
        add(topArea, BorderLayout.NORTH);
        
        // 타이틀 아래 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(210, 210, 210));

        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.add(separator, BorderLayout.CENTER);

        topArea.add(sepWrapper);

        // 상단 영역을 NORTH에 붙이기
        add(topArea, BorderLayout.NORTH);


        // ----------------------- 채팅방 리스트 영역 ------------------------------

        // 각 채팅방 항목의 높이/폰트/배경 등 기본 설정
        chatList.setFixedCellHeight(44);
        chatList.setFont(new Font("Dialog", Font.PLAIN, 14));
        chatList.setBackground(Color.WHITE);

        //채팅방 리스트도 닉네임으로 보여주기 위한 렌더러
        chatList.setCellRenderer(new ChatRoomCellRenderer());

        // 채팅방 리스트 더블 클릭 시 해당 방 열기
        chatList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String roomId = chatList.getSelectedValue();
                    if (roomId != null && clientNet != null) {
                        clientNet.openRoom(roomId); // 실제 방 창 띄우기(이미 열려 있으면 앞으로 가져오기)
                    }
                }
            }
        });

        // 채팅방 리스트에 스크롤 적용
        JScrollPane scrollChat = new JScrollPane(chatList);
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

    // =========================
    // [TabView] 탭 제목 반환
    // =========================
    @Override
    public String getTitle() {
        return "Chats";
    }

    // =========================
    // [TabView] 새로고침 훅
    // - 추후 서버에서 채팅방 목록 갱신할 때 사용
    // =========================
    @Override
    public void refresh() {
        // 나중에 서버에서 채팅방 목록 갱신
    }
    
    // 이 탭의 실제 컴포넌트를 반환
    @Override
    public JComponent getComponent() {
        return this;
    }

    // 채팅방 이름(roomId)을 리스트에 추가하는 메소드
    public void addRoom(String name) {
        if (!model.contains(name)) {
            model.addElement(name);
        }
    }

    // 네트워크 객체 ClientNet 주입 (ChatHomeFrame에서 호출)
    public void setClientNet(ClientNet clientNet) {
        this.clientNet = clientNet;
    }

    // FriendsPanel 주입 (ChatHomeFrame에서 호출)
    // - 유저 목록, 닉네임 정보 등을 가져오는데 사용
    public void setFriendsList(FriendsPanel friendsPanel) {
        this.friendsPanel = friendsPanel;
    }

    // ==================== 채팅방 리스트 렌더러 ====================
    // JList에 들어있는 값은 "손채림,박소연" 같은 roomId 문자열이다.
    // 이 렌더러에서는 각 멤버의 "표시용 이름"을 friendsPanel에서 받아와서
    // "채림, 소연" 이런 식으로 UI에 보여준다.
    private class ChatRoomCellRenderer extends JLabel implements ListCellRenderer<String> {

        public ChatRoomCellRenderer() {
            setOpaque(true); // 배경색이 보이도록 설정
            setFont(new Font("Dialog", Font.PLAIN, 14));
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list,
                String value,        // 이 셀에 해당하는 roomId 문자열
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            String roomId = value;
            String displayText = roomId; // 기본은 raw roomId

            // FriendsPanel이 있고, roomId가 null이 아니라면
            if (friendsPanel != null && roomId != null) {
                String[] members = roomId.split(",");
                java.util.List<String> names = new java.util.ArrayList<>();

                // roomId에 들어있는 각 멤버 이름을 돌며 표시용 이름으로 변환
                for (String m : members) {
                    if (m == null) continue;
                    String trimmed = m.trim();
                    if (trimmed.isEmpty()) continue;

                    // friendsPanel에서 displayName(닉네임)을 가져옴
                    String disp = friendsPanel.getDisplayName(trimmed);
                    names.add(disp);
                }

                // 한 명 이상 있으면 "A, B, C" 형태의 문자열로 합침
                if (!names.isEmpty()) {
                    displayText = String.join(", ", names);
                }
            }

            // 최종적으로 셀에 표시할 텍스트 세팅
            setText(displayText);

            // 선택된 셀 배경색 / 선택 안 된 셀 배경색 구분
            if (isSelected) {
                setBackground(new Color(230, 230, 230));
            } else {
                setBackground(Color.WHITE);
            }

            return this; // 이 JLabel 자체를 셀 컴포넌트로 사용
        }
    }
}