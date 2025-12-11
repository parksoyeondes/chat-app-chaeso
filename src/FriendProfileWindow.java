// FriendProfileWindow.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

public class FriendProfileWindow extends JDialog {

    private static final String CARD_VIEW = "CARD_VIEW";
    private static final String CARD_EDIT = "CARD_EDIT";

    // 상단 배경 영역 높이
    private static final int HEADER_HEIGHT = 150;

    // 상대방 실제 프로필 데이터 (사진/상태메시지/배경 등)
    private final ProfileData profileData;

    // 서버에서 사용하는 실제 이름 (화면에는 직접 안 보여줘도 됨)
    private final String realName;

    // 내가 부르는 이름 (표시 이름)
    private String displayName;

    // Save 눌렀을 때, 변경된 displayName을 FriendsPanel 에 알려주는 콜백
    private final Consumer<String> onNameSaved;

    // 카드 레이아웃
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // VIEW 모드 컴포넌트
    private JLabel viewProfileImageLabel;
    private JLabel viewDisplayNameLabel;
    private JLabel viewStatusLabel;

    // EDIT 모드 컴포넌트
    private JLabel editProfileImageLabel;
    private JTextField txtDisplayName;
    private JLabel editStatusLabel;

    // 편집 중 여부 (필요하면 배경 쪽에서 쓸 수 있게 남겨둠)
    private boolean editing = false;

    public FriendProfileWindow(Frame owner,
                               ProfileData profileData,
                               String realName,
                               String displayName,
                               Consumer<String> onNameSaved) {
        super(owner, "Friend Profile", true);   // 타이틀 Friend Profile
        this.profileData = profileData;
        this.realName = realName;
        // 표시이름 없으면 기본은 실제 이름으로
        this.displayName = (displayName == null || displayName.isEmpty())
                ? realName
                : displayName;
        this.onNameSaved = onNameSaved;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 메인 홈 크기랑 맞춰서
        setSize(300, 400);
        setLocationRelativeTo(owner);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cardPanel, BorderLayout.CENTER);

        buildViewCard();
        buildEditCard();

        cardLayout.show(cardPanel, CARD_VIEW);
    }

    // ==========================
    // VIEW 카드 (친구 프로필 보는 화면)
    // ==========================
    private void buildViewCard() {
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

        // 프로필 이미지
        viewProfileImageLabel = new JLabel();
        viewProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        if (icon != null) {
            viewProfileImageLabel.setIcon(icon);
        } else {
            viewProfileImageLabel.setText("🙂");
            viewProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }
        center.add(Box.createVerticalStrut(10));
        center.add(viewProfileImageLabel);
        center.add(Box.createVerticalStrut(18));

        // 표시 이름 (내가 부르는 이름)
        viewDisplayNameLabel = new JLabel(displayName, SwingConstants.CENTER);
        viewDisplayNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewDisplayNameLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        center.add(viewDisplayNameLabel);
        center.add(Box.createVerticalStrut(10));

        // 이름 밑 구분선
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));
        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        center.add(sepWrapper);
        center.add(Box.createVerticalStrut(14));

        // 상태메시지 (상대가 설정한 것)
        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        viewStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        center.add(Box.createVerticalStrut(4));
        center.add(viewStatusLabel);
        center.add(Box.createVerticalStrut(30));

        card.add(center, BorderLayout.CENTER);

        // === 하단 Edit 버튼 ===
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 0, 20, 20));

        JButton btnEdit = new JButton("Edit");
        btnEdit.setBackground(new Color(200, 200, 200));
        btnEdit.setPreferredSize(new Dimension(80, 32));
        btnEdit.setFocusPainted(false);
        btnEdit.addActionListener(e -> {
            editing = true;
            enterEditMode();
            cardLayout.show(cardPanel, CARD_EDIT);
            cardPanel.repaint();
        });

        bottom.add(btnEdit);
        card.add(bottom, BorderLayout.SOUTH);

        // 이름 더블클릭 → 바로 Edit 모드 진입
        viewDisplayNameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editing = true;
                    enterEditMode();
                    cardLayout.show(cardPanel, CARD_EDIT);
                    cardPanel.repaint();
                }
            }
        });

        cardPanel.add(card, CARD_VIEW);
    }

    // ==========================
    // EDIT 카드 (친구 표시 이름 수정 화면)
    // ==========================
    private void buildEditCard() {
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        // VIEW 카드와 동일한 여백
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

        // 프로필 이미지는 그대로 보여주기(수정 불가)
        editProfileImageLabel = new JLabel();
        editProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        if (icon != null) {
            editProfileImageLabel.setIcon(icon);
        } else {
            editProfileImageLabel.setText("🙂");
            editProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }
        center.add(Box.createVerticalStrut(10));   // 이미지 위쪽 간격
        center.add(editProfileImageLabel);
        center.add(Box.createVerticalStrut(18));   // 이미지와 이름 사이 간격

        // 표시 이름만 수정 가능 (내가 부르는 이름)
        txtDisplayName = new JTextField(displayName);
        txtDisplayName.setHorizontalAlignment(JTextField.CENTER);
        txtDisplayName.setBorder(null);
        txtDisplayName.setFont(new Font("Dialog", Font.BOLD, 18));
        txtDisplayName.setMaximumSize(new Dimension(180, 28));
        txtDisplayName.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(txtDisplayName);
        center.add(Box.createVerticalStrut(10));   // 이름 아래 간격

        // 구분선 (VIEW랑 느낌 맞춤)
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));
        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        // VIEW 쪽이 10, 40, 0, 40 이라 그대로 맞춤
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        center.add(sepWrapper);
        center.add(Box.createVerticalStrut(14));   // 구분선 아래 간격

        // 상태메시지는 수정 불가 (그냥 라벨)
        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        editStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        editStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));

        center.add(Box.createVerticalStrut(4));   // 구분선과의 간격
        center.add(editStatusLabel);
        center.add(Box.createVerticalStrut(30));  // 아래 여백

        card.add(center, BorderLayout.CENTER);

        // === 하단 Cancel / Save ===
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(0, 0, 25, 25));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.setBackground(new Color(210, 210, 210));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> {
            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        JButton btnSave = new JButton("Save");
        btnSave.setPreferredSize(new Dimension(80, 32));
        btnSave.setBackground(new Color(60, 179, 113));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            String newName = txtDisplayName.getText().trim();
            // 비어 있으면 실제 이름으로 되돌림
            if (newName.isEmpty()) {
                newName = realName;
            }
            displayName = newName;

            // 콜백으로 FriendsPanel 에 알려주기
            if (onNameSaved != null) {
                onNameSaved.accept(displayName);
            }

            // VIEW 화면 이름 갱신
            viewDisplayNameLabel.setText(displayName);

            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        leftWrap.setBackground(Color.WHITE);
        leftWrap.add(btnCancel);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setBackground(Color.WHITE);
        rightWrap.add(btnSave);

        bottom.add(leftWrap, BorderLayout.WEST);
        bottom.add(rightWrap, BorderLayout.EAST);

        card.add(bottom, BorderLayout.SOUTH);

        cardPanel.add(card, CARD_EDIT);
    }

    // Edit 모드 들어갈 때 현재 displayName을 필드에 채워줌
    private void enterEditMode() {
        if (txtDisplayName != null) {
            txtDisplayName.setText(displayName);
        }
    }

    // ==========================
    // 공용 이미지 로더
    // ==========================
    private ImageIcon loadImageIcon(String path, int w, int h) {
        if (path == null || path.isEmpty()) return null;
        Image raw = null;
        try {
            if (path.startsWith("/")) {
                URL url = getClass().getResource(path);
                if (url != null) raw = new ImageIcon(url).getImage();
            } else {
                File f = new File(path);
                if (f.exists()) raw = new ImageIcon(path).getImage();
            }
            if (raw == null) return null;
            Image scaled = raw.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    // ==========================
    // 상단 배경(회색/이미지) 그리는 패널
    // ==========================
    private class BackgroundPanel extends JPanel {
        private String lastBgPath;
        private Image bgImage;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            // 상대방이 설정한 배경 사용
            String bgPath = profileData.getBackgroundImagePath();

            if (bgPath == null || bgPath.isEmpty()) {
                bgImage = null;
                lastBgPath = null;
            } else if (!bgPath.equals(lastBgPath)) {
                lastBgPath = bgPath;
                bgImage = loadBackgroundImage(bgPath);
            }

            Graphics2D g2 = (Graphics2D) g;

            // 상단 헤더 부분
            if (bgImage != null) {
                g2.drawImage(bgImage, 0, 0, w, HEADER_HEIGHT, this);
            } else {
                g2.setColor(new Color(220, 220, 220));
                g2.fillRect(0, 0, w, HEADER_HEIGHT);
            }

            // 아래쪽 흰색
            g2.setColor(Color.WHITE);
            g2.fillRect(0, HEADER_HEIGHT, w, h - HEADER_HEIGHT);
        }

        private Image loadBackgroundImage(String path) {
            try {
                Image raw = null;
                if (path.startsWith("/")) {
                    URL url = getClass().getResource(path);
                    if (url != null) raw = new ImageIcon(url).getImage();
                } else {
                    File f = new File(path);
                    if (f.exists()) raw = new ImageIcon(path).getImage();
                }
                return raw;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
