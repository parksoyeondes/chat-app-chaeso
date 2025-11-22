// FriendsPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;

public class FriendsPanel extends JPanel implements TabView {

    private String myName;

    private DefaultListModel<String> model = new DefaultListModel<>();
    private JList<String> friendList = new JList<>(model);

    public FriendsPanel(String myName) {
        this.myName = myName;

        // 전체 패널 기본 세팅
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);              // ★ 전체 흰색

        // =======================
        // 0) 상단 영역 (타이틀 + 내 프로필 + 구분선)
        // =======================
        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        // 1) "친구" 타이틀
        JLabel lblTitle = new JLabel("Freinds");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20)); // 공통 폰트
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(lblTitle);

        topArea.add(Box.createVerticalStrut(10));

        // 2) 내 프로필 (이미지 + 이름)
        JPanel myProfilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        myProfilePanel.setBackground(Color.WHITE);
        myProfilePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- 프로필 이미지 로딩 ---
        JLabel profileImageLabel;
        ImageIcon icon = loadProfileIcon();   // 아래 메서드에서 안전하게 로드

        if (icon != null) {
            profileImageLabel = new JLabel(icon);
        } else {
            // 이미지 못 찾았을 때 대비 – 원형 대신 이니셜 텍스트
            profileImageLabel = new JLabel("🙂");
            profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 26));
        }

        myProfilePanel.add(profileImageLabel);

        JLabel lblMyName = new JLabel(myName);
        lblMyName.setFont(new Font("Dialog", Font.PLAIN, 15));
        myProfilePanel.add(lblMyName);

        topArea.add(myProfilePanel);

        topArea.add(Box.createVerticalStrut(8));

        // 3) 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(210, 210, 210));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(separator);

        add(topArea, BorderLayout.NORTH);

        // =======================
        // 4) 친구 목록 (스크롤)
        // =======================
        friendList.setFixedCellHeight(40);
        friendList.setFont(new Font("Dialog", Font.PLAIN, 14));
        friendList.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(friendList);
        scroll.setBorder(new EmptyBorder(5, 15, 15, 15));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE); // ★ 안쪽도 흰색

        add(scroll, BorderLayout.CENTER);
    }

    // 프로필 이미지 로딩 (클래스패스에서)
    private ImageIcon loadProfileIcon() {
        try {
            // src/icons/tomato_face.png 라고 저장해놨다고 가정
            // (빌드 후에는 /icons/tomato_face.png 로 클래스패스에 올라감)
            java.net.URL url = getClass().getResource("/icons/tomato_face.png");
            if (url == null) {
                System.out.println("프로필 이미지 리소스를 찾을 수 없음: /icons/tomato_face.png");
                return null;
            }
            Image raw = new ImageIcon(url).getImage();
            Image scaled = raw.getScaledInstance(55, 55, Image.SCALE_SMOOTH);
            return makeRoundedImage(scaled, 55);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 원형 이미지로 변환
    private ImageIcon makeRoundedImage(Image srcImg, int size) {
        BufferedImage circularImg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circularImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(srcImg, 0, 0, size, size, null);
        g2.dispose();
        return new ImageIcon(circularImg);
    }

    // TabView 구현
    @Override
    public String getTitle() {
        return "Freinds";
    }

    @Override
    public void refresh() {
        // 나중에 서버에서 목록 다시 받으면 여기서 리프레시
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    // 친구 추가/삭제
    public void addFriend(String name) {
        if (name == null) return;
        name = name.trim();
        if (!name.equals(myName) && !model.contains(name)) {
            model.addElement(name);
        }
    }

    public void removeFriend(String name) {
        if (name == null) return;
        model.removeElement(name.trim());
    }

    public void clearFriends() {
        model.clear();
    }
}
