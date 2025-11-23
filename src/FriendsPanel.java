// FriendsPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;

public class FriendsPanel extends JPanel implements TabView {

    private final String myName;
    private final ProfileData myProfile;

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> friendList = new JList<>(model);

    // 상단 내 프로필 UI를 갱신하기 위해 참조를 잡아둠
    private JLabel profileImageLabel;
    private JLabel lblMyName;

    public FriendsPanel(String myName) {
        this.myName = myName;
        // 내 프로필 기본값 설정 (필요하면 기본 이미지 경로 수정)
        this.myProfile = new ProfileData(
                myName,
                "One line Introduction",
                "/icons/tomato_face.png",
                "/icons/profile_bg_default.png"
        );

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

        // 내 프로필 클릭하면 프로필 창 띄우기
        myProfilePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        myProfilePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 부모 Frame 찾기
                Window w = SwingUtilities.getWindowAncestor(FriendsPanel.this);
                Frame owner = (w instanceof Frame) ? (Frame) w : null;

                ProfileWindow dialog = new ProfileWindow(owner, myProfile, new Runnable() {
                    @Override
                    public void run() {
                        refreshMyProfileView();
                        // TODO: 여기서 서버로 "/profile_update ..." 같은 메시지 보내도 됨
                        // ex) clientNet.SendMessage("/profile_update " + ... );
                    }
                });
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
        friendList.setFont(new Font("Dialog", Font.PLAIN, 14));
        friendList.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(friendList);
        scroll.setBorder(new EmptyBorder(5, 15, 15, 15));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    // 내 프로필이 수정되었을 때 상단 UI 갱신
    private void refreshMyProfileView() {
        lblMyName.setText(myProfile.getName());
        ImageIcon icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45);
        if (icon != null) {
            profileImageLabel.setIcon(icon);
            profileImageLabel.setText("");
        }
        // 상태메시지를 FriendsPanel에서 보여주고 싶으면 Label 하나 더 만들어서 여기서 setText 하면 됨
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
    @Override public void refresh() { /* 서버에서 목록 다시 받는 훅 */ }
    @Override public JComponent getComponent() { return this; }

    // ===== 목록 조작 메서드 =====
    public void setUserList(String [] names) {
        System.out.println("[FriendsPanel] setUserList 호출됨, 길이 = " + names.length);
        model.clear();              // 기존 목록 싹 지우고
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                continue;
            }
            String trimmed = names[i].trim();

            if (trimmed.isEmpty())
                continue;   // 공백/빈 문자열이면 무시
            model.addElement(trimmed);
        }
    }

    public void clearFriends() {
        model.clear();
    }

    //새로 들어온 유저 집어넣기
    public void addUser(String name) {
        if (name == null)
            return;
        String trimmed = name.trim(); // 혹시 모를 공백 제거
        if (trimmed.isEmpty())
            return;

        // 이미 있는 이름이면 중복 추가 안 하기 -> 근데 새로 로그인해서 들어오면 어차피 이 모델에 없으니까 넣어지게 됨
        if (!model.contains(trimmed)) {
            model.addElement(trimmed);
        }
    }


}
