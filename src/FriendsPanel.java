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
    private final ProfileData myProfile; // 수정함

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> friendList = new JList<>(model);

    private JLabel profileImageLabel;
    private JLabel lblMyName;

    private final Map<String, String> friendNicknameMap = new HashMap<>(); // 수정함
    private final Map<String, ProfileData> friendProfileMap = new HashMap<>(); // 수정함

    private ImageIcon defaultFriendIcon;

    private ClientNet clientNet; // 수정함

    public FriendsPanel(String myName) {
        this.myName = myName;
        this.myProfile = new ProfileData( // 수정함
                myName,
                "One line Introduction",
                "/icons/tomato_face.png",
                "/icons/profile_bg_default.png"
        );

        defaultFriendIcon = loadProfileIconSimple("/icons/tomato_face.png", 40, 32); // 수정함

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
        ImageIcon icon = null;
        if (myProfile.getProfileImageIcon() != null) { // 수정함
            icon = ProfileData.scaleIcon(myProfile.getProfileImageIcon(), 55, 45); // 수정함
        } else {
            icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45); // 수정함
        }

        if (icon != null) profileImageLabel.setIcon(icon);
        else {
            profileImageLabel.setText("🙂");
            profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 26));
        }
        myProfilePanel.add(profileImageLabel);

        lblMyName = new JLabel(myProfile.getName()); // 수정함
        lblMyName.setFont(new Font("Dialog", Font.PLAIN, 15));
        myProfilePanel.add(lblMyName);

        myProfilePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        myProfilePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Window w = SwingUtilities.getWindowAncestor(FriendsPanel.this);
                Frame owner = (w instanceof Frame) ? (Frame) w : null;

                ProfileWindow dialog = new ProfileWindow( // 수정함
                        owner,
                        myProfile,
                        new Runnable() {
                            @Override
                            public void run() {
                                refreshMyProfileView(); // 수정함

                                if (clientNet != null) { // 수정함
                                    clientNet.sendProfileUpdate(myProfile); // 수정함

                                    // 프로필 사진 파일이면 바이트 전송 // 수정함
                                    File pFile = tryFile(myProfile.getProfileImagePath()); // 수정함
                                    if (pFile != null) clientNet.sendMyProfileImage(pFile); // 수정함

                                    // 배경 사진 파일이면 바이트 전송 // 수정함
                                    File bFile = tryFile(myProfile.getBackgroundImagePath()); // 수정함
                                    if (bFile != null) clientNet.sendMyBackgroundImage(bFile); // 수정함
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
        friendList.setCellRenderer(new FriendCellRenderer()); // 수정함

        friendList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = friendList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String realName = model.getElementAt(index);
                        openFriendProfile(realName); // 수정함
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

    public void setClientNet(ClientNet clientNet) { // 수정함
        this.clientNet = clientNet; // 수정함
    }

    private File tryFile(String path) { // 수정함
        if (path == null) return null;
        String t = path.trim();
        if (t.isEmpty()) return null;
        if (t.startsWith("/")) return null; // 리소스면 전송 안 함 // 수정함
        File f = new File(t);
        if (!f.exists()) return null;
        return f;
    }

    private void refreshMyProfileView() { // 수정함
        lblMyName.setText(myProfile.getName()); // 수정함

        ImageIcon icon = null;
        if (myProfile.getProfileImageIcon() != null) { // 수정함
            icon = ProfileData.scaleIcon(myProfile.getProfileImageIcon(), 55, 45); // 수정함
        } else {
            icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45); // 수정함
        }

        if (icon != null) {
            profileImageLabel.setIcon(icon);
            profileImageLabel.setText("");
        }
        repaint();
    }

    private void openFriendProfile(String realName) { // 수정함
        if (realName == null || realName.trim().isEmpty()) return;

        ProfileData friendProfile = friendProfileMap.get(realName); // 수정함
        if (friendProfile == null) {
            friendProfile = new ProfileData(realName); // 수정함
            friendProfileMap.put(realName, friendProfile); // 수정함
        }

        String displayName = getDisplayName(realName); // 수정함

        Window w = SwingUtilities.getWindowAncestor(this);
        Frame owner = (w instanceof Frame) ? (Frame) w : null;

        FriendProfileWindow dialog = new FriendProfileWindow( // 수정함
                owner,
                friendProfile,
                realName,
                displayName,
                newDisplayName -> setFriendNickname(realName, newDisplayName) // 수정함
        );
        dialog.setVisible(true);
    }

    public void setFriendNickname(String realName, String nick) { // 수정함
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (nick == null || nick.trim().isEmpty()) friendNicknameMap.remove(key); // 수정함
        else friendNicknameMap.put(key, nick.trim()); // 수정함

        friendList.repaint(); // 수정함
    }

    public String getDisplayName(String realName) { // 수정함
        if (realName == null) return "";
        String trimmed = realName.trim();
        if (trimmed.isEmpty()) return "";
        String nick = friendNicknameMap.get(trimmed); // 수정함
        if (nick != null && !nick.trim().isEmpty()) return nick.trim(); // 수정함
        return trimmed;
    }

    // ===================== 텍스트 실시간 반영 =====================
    public void updateFriendProfile(String realName, String displayName, String status) { // 수정함
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (key.equals(myName)) { // 수정함
            myProfile.setName(displayName); // 수정함
            myProfile.setStatusMessage(status); // 수정함
            refreshMyProfileView(); // 수정함
            return;
        }

        ProfileData p = friendProfileMap.get(key); // 수정함
        if (p == null) {
            p = new ProfileData(key); // 수정함
            friendProfileMap.put(key, p); // 수정함
        }

        p.setName(displayName); // 수정함
        p.setStatusMessage(status); // 수정함

        setFriendNickname(key, displayName); // 수정함
        friendList.repaint(); // 수정함
    }

    // ===================== 프로필 사진 실시간 반영 =====================
    public void updateFriendProfileImage(String realName, ImageIcon icon) { // 수정함
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (key.equals(myName)) { // 수정함
            myProfile.setProfileImageIcon(icon); // 수정함
            refreshMyProfileView(); // 수정함
            return;
        }

        ProfileData p = friendProfileMap.get(key); // 수정함
        if (p == null) {
            p = new ProfileData(key); // 수정함
            friendProfileMap.put(key, p); // 수정함
        }
        p.setProfileImageIcon(icon); // 수정함

        friendList.repaint(); // 수정함
    }

    // ===================== 배경 사진 실시간 반영 =====================
    public void updateFriendBackgroundImage(String realName, ImageIcon icon) { // 수정함
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (key.equals(myName)) { // 수정함
            myProfile.setBackgroundImageIcon(icon); // 수정함
            repaint(); // 수정함
            return;
        }

        ProfileData p = friendProfileMap.get(key); // 수정함
        if (p == null) {
            p = new ProfileData(key); // 수정함
            friendProfileMap.put(key, p); // 수정함
        }
        p.setBackgroundImageIcon(icon); // 수정함

        repaint(); // 수정함
    }

    private ImageIcon loadProfileIconSimple(String path, int width, int height) {
        if (path == null || path.isEmpty()) return null;
        Image raw = null;

        try {
            if (path.startsWith("/")) {
                URL url = FriendsPanel.class.getResource(path);
                if (url != null) raw = new ImageIcon(url).getImage();
            } else {
                File f = new File(path);
                if (f.exists()) raw = new ImageIcon(path).getImage();
            }
            if (raw == null) return null;

            Image scaled = raw.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    @Override public JComponent getComponent() { return this; }

    public void setUserList(String[] names) {
        model.clear();
        if (names == null) return;

        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) continue;

            String trimmed = names[i].trim();
            if (trimmed.isEmpty()) continue;

            if (!model.contains(trimmed)) model.addElement(trimmed);

            friendProfileMap.computeIfAbsent(trimmed, n -> new ProfileData(n)); // 수정함
        }
    }

    public void addUser(String name) {
        if (name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;

        if (!model.contains(trimmed)) model.addElement(trimmed);
        friendProfileMap.computeIfAbsent(trimmed, n -> new ProfileData(n)); // 수정함
    }

    public String[] getFriendsList() {
        int size = model.getSize();
        String[] usersForChat = new String[size];
        for (int i = 0; i < model.size(); i++) usersForChat[i] = model.getElementAt(i);
        return usersForChat;
    }

    private class FriendCellRenderer extends JPanel implements ListCellRenderer<String> { // 수정함

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
            String displayName = getDisplayName(realName); // 수정함

            ProfileData p = friendProfileMap.get(realName); // 수정함

            ImageIcon show = null;
            if (p != null && p.getProfileImageIcon() != null) { // 수정함
                show = ProfileData.scaleIcon(p.getProfileImageIcon(), 40, 32); // 수정함
            } else {
                show = defaultFriendIcon; // 수정함
            }

            if (show != null) {
                iconLabel.setIcon(show);
                iconLabel.setText("");
            } else {
                iconLabel.setIcon(null);
                iconLabel.setText("🙂");
            }

            nameLabel.setText(displayName); // 수정함

            if (isSelected) setBackground(new Color(230, 230, 230));
            else setBackground(Color.WHITE);

            return this;
        }
    }
}
