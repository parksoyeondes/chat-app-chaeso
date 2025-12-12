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

    private JLabel profileImageLabel;
    private JLabel lblMyName;

    private final Map<String, String> friendNicknameMap = new HashMap<>();
    private final Map<String, ProfileData> friendProfileMap = new HashMap<>();

    private ImageIcon defaultFriendIcon;

    private ClientNet clientNet;

    public FriendsPanel(String myName) {
        this.myName = myName;
        this.myProfile = new ProfileData(
                myName,
                "One line Introduction",
                "/icons/tomato_face.png",
                "/icons/profile_bg_default.png"
        );

        defaultFriendIcon = loadProfileIconSimple("/icons/tomato_face.png", 40, 32);

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        JLabel lblTitle = new JLabel("Friends");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(lblTitle);
        topArea.add(Box.createVerticalStrut(10));

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

                                // 저장됐으면 서버로 내 프로필 업데이트 전송
                                if (clientNet != null) {
                                    clientNet.sendProfileUpdate(myProfile);
                                }
                            }
                        }
                );
                dialog.setVisible(true);
            }
        });

        topArea.add(myProfilePanel);
        topArea.add(Box.createVerticalStrut(8));

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(210, 210, 210));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(separator);

        add(topArea, BorderLayout.NORTH);

        friendList.setFixedCellHeight(40);
        friendList.setBackground(Color.WHITE);
        friendList.setCellRenderer(new FriendCellRenderer());

        friendList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = friendList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String realName = model.getElementAt(index);
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

    public void setClientNet(ClientNet clientNet) {
        this.clientNet = clientNet;
    }

    private void refreshMyProfileView() {
        lblMyName.setText(myProfile.getName());
        ImageIcon icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45);
        if (icon != null) {
            profileImageLabel.setIcon(icon);
            profileImageLabel.setText("");
        }
        repaint();
    }

    private void openFriendProfile(String realName) {
        if (realName == null || realName.trim().isEmpty()) return;

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

        String displayName = getDisplayName(realName);

        Window w = SwingUtilities.getWindowAncestor(this);
        Frame owner = (w instanceof Frame) ? (Frame) w : null;

        FriendProfileWindow dialog = new FriendProfileWindow(
                owner,
                friendProfile,
                realName,
                displayName,
                newDisplayName -> setFriendNickname(realName, newDisplayName)
        );
        dialog.setVisible(true);
    }

    public void setFriendNickname(String realName, String nick) {
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (nick == null || nick.trim().isEmpty()) {
            friendNicknameMap.remove(key);
        } else {
            friendNicknameMap.put(key, nick.trim());
        }
        friendList.repaint();
    }

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

    // ===================== 핵심: 실시간 프로필 반영 =====================
    public void updateFriendProfile(String realName, String displayName, String status) {
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        // 내 프로필 업데이트를 서버에서 다시 받는 경우
        if (key.equals(myName)) {
            myProfile.setName(displayName);
            myProfile.setStatusMessage(status);
            refreshMyProfileView();
            return;
        }

        ProfileData p = friendProfileMap.get(key);
        if (p == null) {
            p = new ProfileData(key);
            friendProfileMap.put(key, p);
        }

        p.setName(displayName);
        p.setStatusMessage(status);

        // 친구 리스트에는 “표시 이름”이 보여야 하니까 닉네임 맵도 갱신
        // (원래 별칭 기능을 계속 쓰고 싶으면, 여기서 setFriendNickname 호출을 빼면 됨)
        setFriendNickname(key, displayName);

        friendList.repaint();
    }

    private ImageIcon loadProfileIconSimple(String path, int width, int height) {
        if (path == null || path.isEmpty()) return null;
        Image raw = null;

        try {
            if (path.startsWith("/")) {
                URL url = FriendsPanel.class.getResource(path);
                if (url != null) {
                    raw = new ImageIcon(url).getImage();
                }
            } else {
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

    @Override public String getTitle() { return "Friends"; }
    @Override public void refresh() { }
    @Override public JComponent getComponent() { return this; }

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

    public void clearFriends() {
        model.clear();
        friendNicknameMap.clear();
        friendProfileMap.clear();
    }

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

    public String[] getFriendsList() {
        int size = model.getSize();
        String[] usersForChat = new String[size];
        for (int i = 0; i < model.size(); i++) {
            usersForChat[i] = model.getElementAt(i);
        }
        return usersForChat;
    }

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
            String realName = value;
            String displayName = getDisplayName(realName);

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
