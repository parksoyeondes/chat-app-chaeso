// FriendsPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class FriendsPanel extends JPanel implements TabView {

    private final String myName;
    private final ProfileData myProfile;

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> friendList = new JList<>(model);

    // 상단 내 프로필 UI를 갱신하기 위해 참조
    private JLabel profileImageLabel;
    private JLabel lblMyName;

    // 친구들의 "표시 이름" 저장용 (실제 서버 이름 → 내가 붙인 표시 이름)
    private final Map<String, String> friendNicknameMap = new HashMap<>();

    // 친구별 프로필 데이터 (상대방 프로필)
    private final Map<String, ProfileData> friendProfileMap = new HashMap<>();

    // 친구 프로필 기본 아이콘 (모든 친구들 같은 이미지)
    private ImageIcon defaultFriendIcon;

    // 서버 통신용 (나중에 프로필 실시간 동기화에 쓸 수 있음)
    private ClientNet clientNet;

    public FriendsPanel(String myName) {
        this.myName = myName;
        // 내 프로필 기본값
        this.myProfile = new ProfileData(
                myName,
                "One line Introduction",
                "/icons/tomato_face.png",
                "/icons/profile_bg_default.png"
        );

        // 친구들 공통 아이콘 (조금 작게)
        defaultFriendIcon = loadProfileIconSimple("/icons/tomato_face.png", 40, 32);

        // 전체 패널 기본 설정
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // ===== 상단 영역 (Friends 제목, 내 프로필, 구분선) =====
        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        // 제목 Friends
        JLabel lblTitle = new JLabel("Friends");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(lblTitle);
        topArea.add(Box.createVerticalStrut(10));

        // 내 프로필 (이미지 + 닉네임)
        JPanel myProfilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        myProfilePanel.setBackground(Color.WHITE);
        myProfilePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        profileImageLabel = new JLabel();
        ImageIcon icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45);
        if (icon != null) {
            profileImageLabel.setIcon(icon);
        } else {
            profileImageLabel.setText("🙂");
            profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 26));
        }
        myProfilePanel.add(profileImageLabel);

        lblMyName = new JLabel(myProfile.getName());
        lblMyName.setFont(new Font("Dialog", Font.PLAIN, 15));
        myProfilePanel.add(lblMyName);

        // 내 프로필 클릭 → 내 프로필 창(ProfileWindow) 띄우기
        myProfilePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        myProfilePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Window w = SwingUtilities.getWindowAncestor(FriendsPanel.this);
                Frame owner = (w instanceof Frame) ? (Frame) w : null;

                ProfileWindow dialog = new ProfileWindow(
                        owner,
                        myProfile,
                        new Runnable() {
                            @Override
                            public void run() {
                                refreshMyProfileView();
                                // 여기서 내 프로필 변경 서버로 보내는 로직 추가 가능
                                // if (clientNet != null) clientNet.SendMessage("/profileUpdate ...");
                            }
                        }
                );
                dialog.setVisible(true);
            }
        });

        topArea.add(myProfilePanel);
        topArea.add(Box.createVerticalStrut(8));

        // 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(210, 210, 210));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(separator);

        add(topArea, BorderLayout.NORTH);

        // ===== 친구 목록 =====
        friendList.setFixedCellHeight(40);
        friendList.setBackground(Color.WHITE);

        // 각 줄 렌더러 (아이콘 + 표시 이름)
        friendList.setCellRenderer(new FriendCellRenderer());

        // ★ 친구 항목 더블클릭 → 친구 프로필 창 띄우기
        friendList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 더블클릭
                    int index = friendList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String realName = model.getElementAt(index); // 서버에 있는 실제 이름
                        openFriendProfile(realName);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(friendList);
        scroll.setBorder(new EmptyBorder(5, 15, 15, 15));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    // ===== ChatHomeFrame 에서 ClientNet 주입 =====
    public void setClientNet(ClientNet clientNet) {
        this.clientNet = clientNet;
    }

    // ===== 내 프로필 UI 갱신 =====
    private void refreshMyProfileView() {
        lblMyName.setText(myProfile.getName());
        ImageIcon icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45);
        if (icon != null) {
            profileImageLabel.setIcon(icon);
            profileImageLabel.setText("");
        }
    }

    // ===== 친구 프로필 창 띄우기 =====
    private void openFriendProfile(String realName) {
        if (realName == null || realName.trim().isEmpty()) return;

        // 친구 프로필 데이터 (없으면 기본값 생성)
        ProfileData friendProfile = friendProfileMap.get(realName);
        if (friendProfile == null) {
            friendProfile = new ProfileData(
                    realName,
                    "One line Introduction",
                    "/icons/tomato_face.png",
                    "/icons/profile_bg_default.png"
            );
            friendProfileMap.put(realName, friendProfile);
        }

        // 현재 표시 이름 (닉네임) 가져오기
        String displayName = getDisplayName(realName);

        Window w = SwingUtilities.getWindowAncestor(this);
        Frame owner = (w instanceof Frame) ? (Frame) w : null;

        // FriendProfileWindow 열기
        FriendProfileWindow dialog = new FriendProfileWindow(
                owner,
                friendProfile,
                realName,
                displayName,
                newDisplayName -> {
                    // Save 눌렀을 때 → 이 친구의 표시 이름 저장
                    setFriendNickname(realName, newDisplayName);
                }
        );
        dialog.setVisible(true);
    }

    // ===== 표시 이름 저장/삭제 =====
    public void setFriendNickname(String realName, String nick) {
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (nick == null || nick.trim().isEmpty()) {
            friendNicknameMap.remove(key);   // 빈 문자열이면 별칭 제거 → 실제 이름으로 표시
        } else {
            friendNicknameMap.put(key, nick.trim());
        }
        friendList.repaint();
    }

    // ===== 실제 이름 → 화면에 보여줄 이름(표시 이름) =====
    public String getDisplayName(String realName) {
        if (realName == null) return "";
        String trimmed = realName.trim();
        if (trimmed.isEmpty()) return "";
        String nick = friendNicknameMap.get(trimmed);
        if (nick != null && !nick.trim().isEmpty()) {
            return nick.trim();
        }
        return trimmed;
    }

    // ─────────────────────────────────────────────────────────────
    // 경로가 리소스(/로 시작)면 getResource, 아니면 파일 경로로 처리
    // ─────────────────────────────────────────────────────────────
    private ImageIcon loadProfileIconSimple(String path, int width, int height) {
        if (path == null || path.isEmpty()) return null;
        Image raw = null;

        try {
            if (path.startsWith("/")) {  // classpath 리소스
                URL url = FriendsPanel.class.getResource(path);
                if (url != null) {
                    raw = new ImageIcon(url).getImage();
                }
            } else {                      // 일반 파일 경로
                File f = new File(path);
                if (f.exists()) {
                    raw = new ImageIcon(path).getImage();
                }
            }
            if (raw == null) return null;

            Image scaled = raw.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    // ===== TabView 구현 =====
    @Override public String getTitle() { return "Friends"; }
    @Override public void refresh() { /* 나중에 서버에서 목록 다시 받는 훅 */ }
    @Override public JComponent getComponent() { return this; }

    // ===== 서버에서 받은 유저 목록 전체 설정 =====
    public void setUserList(String[] names) {
        model.clear();
        if (names == null) return;

        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) continue;

            String trimmed = names[i].trim();
            if (trimmed.isEmpty()) continue;

            if (!model.contains(trimmed)) {
                model.addElement(trimmed);
            }

            // 친구 프로필 데이터도 기본값으로 하나씩 준비
            friendProfileMap.computeIfAbsent(
                    trimmed,
                    n -> new ProfileData(
                            n,
                            "One line Introduction",
                            "/icons/tomato_face.png",
                            "/icons/profile_bg_default.png"
                    )
            );
        }
    }

    // 친구 목록 전체 클리어
    public void clearFriends() {
        model.clear();
        friendNicknameMap.clear();
        friendProfileMap.clear();
    }

    // 새로 들어온 유저 추가
    public void addUser(String name) {
        if (name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;

        if (!model.contains(trimmed)) {
            model.addElement(trimmed);
        }

        friendProfileMap.computeIfAbsent(
                trimmed,
                n -> new ProfileData(
                        n,
                        "One line Introduction",
                        "/icons/tomato_face.png",
                        "/icons/profile_bg_default.png"
                )
        );
    }

    // 유저 목록 반환 (실제 서버 이름)
    public String[] getFriendsList() {
        int size = model.getSize();
        String[] usersForChat = new String[size];
        for (int i = 0; i < model.size(); i++) {
            usersForChat[i] = model.getElementAt(i);
        }
        return usersForChat;
    }

    // ─────────────────────────────────────────────────────────────
    // JList 렌더러 (프로필 이미지 + 표시 이름)
    // ─────────────────────────────────────────────────────────────
    private class FriendCellRenderer extends JPanel implements ListCellRenderer<String> {

        private JLabel iconLabel = new JLabel();
        private JLabel nameLabel = new JLabel();

        public FriendCellRenderer() {
            setLayout(new BorderLayout());
            setOpaque(true);

            iconLabel.setPreferredSize(new Dimension(40, 40));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

            nameLabel.setFont(new Font("Dialog", Font.PLAIN, 14));

            add(iconLabel, BorderLayout.WEST);
            add(nameLabel, BorderLayout.CENTER);

            setBorder(new EmptyBorder(5, 5, 5, 5));
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list,
                String value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            // value = 실제 서버 이름
            String realName = value;
            String displayName = getDisplayName(realName);

            // 프로필 아이콘
            if (defaultFriendIcon != null) {
                iconLabel.setIcon(defaultFriendIcon);
                iconLabel.setText("");
            } else {
                iconLabel.setIcon(null);
                iconLabel.setText("🙂");
            }

            nameLabel.setText(displayName);

            if (isSelected) {
                setBackground(new Color(230, 230, 230));
            } else {
                setBackground(Color.WHITE);
            }

            return this;
        }
    }
}
