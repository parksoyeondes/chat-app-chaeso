// FriendsPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FriendsPanel extends JPanel implements TabView {

    private final String myName;

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> friendList = new JList<>(model);

    public FriendsPanel(String myName) {
        this.myName = myName;

        // 패널 기본
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // ===== 상단 영역 (제목, 내 프로필, 구분선) =====
        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        // 제목
        JLabel lblTitle = new JLabel("Friends");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(lblTitle);
        topArea.add(Box.createVerticalStrut(10));

        // 내 프로필 (이미지 + 닉네임)
        JPanel myProfilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        myProfilePanel.setBackground(Color.WHITE);
        myProfilePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel profileImageLabel;
        ImageIcon icon = loadProfileIconSimple("/icons/tomato_face.png", 55, 45); // ★ 단순 축소 사용
        if (icon != null) {
            profileImageLabel = new JLabel(icon);
        } else {
            profileImageLabel = new JLabel("🙂");
            profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 26));
        }
        myProfilePanel.add(profileImageLabel);

        JLabel lblMyName = new JLabel(myName);
        lblMyName.setFont(new Font("Dialog", Font.PLAIN, 15));
        myProfilePanel.add(lblMyName);

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
        friendList.setFont(new Font("Dialog", Font.PLAIN, 14));
        friendList.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(friendList);
        scroll.setBorder(new EmptyBorder(5, 15, 15, 15));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    // 이미지를 단순히 width×height로 축소해서 ImageIcon으로 반환
    // ─────────────────────────────────────────────────────────────
    private ImageIcon loadProfileIconSimple(String resourcePath, int width, int height) {
        java.net.URL url = FriendsPanel.class.getResource(resourcePath);
        if (url == null) {
            System.err.println("[IMG] 리소스를 못 찾음: " + resourcePath);
            return null;
        }
        Image raw = new ImageIcon(url).getImage();
        Image scaled = raw.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // ===== TabView 구현 =====
    @Override public String getTitle() { return "Friends"; }
    @Override public void refresh() { /* 서버에서 목록 다시 받는 훅 */ }
    @Override public JComponent getComponent() { return this; }

    // ===== 목록 조작 메서드 =====
    public void setUsers(java.util.List<String> users) {
        model.clear();
        for (String u : users) {
            if (!u.equals(myName)) model.addElement(u);
        }
    }
    public void addFriend(String name) {
        if (name == null) return;
        name = name.trim();
        if (!name.equals(myName) && !model.contains(name)) model.addElement(name);
    }
    public void removeFriend(String name) {
        if (name == null) return;
        model.removeElement(name.trim());
    }
    public void clearFriends() { model.clear(); }
}
