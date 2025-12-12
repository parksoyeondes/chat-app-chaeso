// ProfileWindow.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;

// 내 프로필을 조회하고 수정하는 다이얼로그
// - View 카드와 Edit 카드를 CardLayout으로 전환
// - 프로필 이미지 클릭으로 프로필 사진 변경
// - 상단(헤더) 클릭으로 배경 이미지 변경
// - Save 시 모델(ProfileData)에 반영하고 콜백 실행

public class ProfileWindow extends JDialog {

    // =========================
    // [1] 카드 전환(View / Edit)
    // =========================
    private static final String CARD_VIEW = "CARD_VIEW";
    private static final String CARD_EDIT = "CARD_EDIT";
    private static final int HEADER_HEIGHT = 150;

    // =========================
    // [2] 프로필 데이터와 저장 콜백
    // =========================
    private final ProfileData profileData;
    private final Runnable onSavedCallback;

    // =========================
    // [3] 카드 레이아웃 컨테이너
    // =========================
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // =========================
    // [4] View 모드 UI
    // =========================
    private JLabel viewProfileImageLabel;
    private JLabel viewNameLabel;
    private JLabel viewStatusLabel;

    // =========================
    // [5] Edit 모드 UI
    // =========================
    private JLabel editProfileImageLabel;
    private JTextField txtName;
    private JTextField txtStatus;

    // =========================
    // [6] 편집 중 임시 경로
    // - Cancel 누르면 모델에 안 들어가고 버려짐
    // =========================
    private String tempProfileImagePath;
    private String tempBackgroundImagePath;

    // =========================
    // [7] 편집 상태
    // =========================
    private boolean editing = false;

    // =========================
    // [0] 생성자
    // =========================
    public ProfileWindow(Frame owner, ProfileData profileData, Runnable onSavedCallback) {
        super(owner, "My Profile", true);
        this.profileData = profileData;
        this.onSavedCallback = onSavedCallback;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(owner);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cardPanel, BorderLayout.CENTER);

        buildViewCard();
        buildEditCard();

        cardLayout.show(cardPanel, CARD_VIEW);
    }

    // =========================
    // [A] View 카드 구성
    // - 배경 헤더 + 프로필 이미지 + 이름 + 상태메시지
    // - 하단 오른쪽 Edit 버튼
    // =========================
    private void buildViewCard() {
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

        // ----- 프로필 이미지 -----
        viewProfileImageLabel = new JLabel();
        viewProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon icon;
        if (profileData.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(profileData.getProfileImageIcon(), 90, 90);
        } else {
            icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        }

        if (icon != null) viewProfileImageLabel.setIcon(icon);
        else {
            viewProfileImageLabel.setText("🙂");
            viewProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }

        center.add(Box.createVerticalStrut(10));
        center.add(viewProfileImageLabel);
        center.add(Box.createVerticalStrut(18));

        // ----- 이름 -----
        viewNameLabel = new JLabel(profileData.getName(), SwingConstants.CENTER);
        viewNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewNameLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        center.add(viewNameLabel);
        center.add(Box.createVerticalStrut(10));

        // ----- 구분선 -----
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));
        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        center.add(sepWrapper);
        center.add(Box.createVerticalStrut(14));

        // ----- 상태 메시지 -----
        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        viewStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        center.add(Box.createVerticalStrut(4));
        center.add(viewStatusLabel);
        center.add(Box.createVerticalStrut(30));

        card.add(center, BorderLayout.CENTER);

        // ----- 하단 Edit 버튼 영역 -----
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 0, 20, 20));

        JButton btnEdit = new JButton("Edit");
        btnEdit.setBackground(new Color(200, 200, 200));
        btnEdit.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnEdit.setPreferredSize(new Dimension(80, 32));
        btnEdit.setFocusPainted(false);

        btnEdit.addActionListener(e -> {
            editing = true;
            enterEditModeFromModel();
            cardLayout.show(cardPanel, CARD_EDIT);
            cardPanel.repaint();
        });

        bottom.add(btnEdit);
        card.add(bottom, BorderLayout.SOUTH);

        cardPanel.add(card, CARD_VIEW);
    }

    // =========================
    // [B] Edit 카드 구성
    // - 상단 헤더 클릭하면 배경 이미지 선택
    // - 프로필 이미지 클릭하면 프로필 사진 선택
    // - 하단 Cancel 왼쪽 Save 오른쪽
    // =========================
    private void buildEditCard() {
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

        // ----- 배경(헤더) 클릭으로 배경 이미지 선택 -----
        center.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;
                if (e.getY() <= (HEADER_HEIGHT - 60)) chooseImageFile(false);
            }
        });

        // ----- 프로필 이미지 클릭으로 프로필 사진 선택 -----
        editProfileImageLabel = new JLabel();
        editProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editProfileImageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editProfileImageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;
                chooseImageFile(true);
            }
        });

        center.add(Box.createVerticalStrut(10));
        center.add(editProfileImageLabel);
        center.add(Box.createVerticalStrut(18));

        // ----- 이름 입력 -----
        txtName = new JTextField();
        txtName.setHorizontalAlignment(JTextField.CENTER);
        txtName.setBorder(null);
        txtName.setFont(new Font("Dialog", Font.BOLD, 18));
        txtName.setMaximumSize(new Dimension(180, 28));
        txtName.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(txtName);
        center.add(Box.createVerticalStrut(10));

        // ----- 구분선 -----
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));
        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        center.add(sepWrapper);
        center.add(Box.createVerticalStrut(14));

        // ----- 상태 메시지 입력 -----
        txtStatus = new JTextField();
        txtStatus.setHorizontalAlignment(JTextField.CENTER);
        txtStatus.setBorder(null);
        txtStatus.setFont(new Font("Dialog", Font.PLAIN, 13));
        txtStatus.setMaximumSize(new Dimension(220, 28));
        txtStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(Box.createVerticalStrut(4));
        center.add(txtStatus);
        center.add(Box.createVerticalStrut(30));

        card.add(center, BorderLayout.CENTER);

        // ----- 하단 Cancel Save 버튼 영역 FriendProfileWindow 스타일로 통일 -----
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(0, 0, 25, 25));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.setBackground(new Color(210, 210, 210));
        btnCancel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnCancel.setFocusPainted(false);

        btnCancel.addActionListener(e -> {
            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        JButton btnSave = new JButton("Save");
        btnSave.setPreferredSize(new Dimension(80, 32));
        btnSave.setBackground(new Color(60, 179, 113));
        btnSave.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);

        btnSave.addActionListener(e -> {
            saveEditToModel();
            editing = false;
            if (onSavedCallback != null) onSavedCallback.run();
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

    // =========================
    // [C] Edit 진입 시 모델 값을 UI에 세팅
    // =========================
    private void enterEditModeFromModel() {
        tempProfileImagePath = profileData.getProfileImagePath();
        tempBackgroundImagePath = profileData.getBackgroundImagePath();

        txtName.setText(profileData.getName());
        txtStatus.setText(profileData.getStatusMessage());

        ImageIcon icon = loadImageIcon(tempProfileImagePath, 90, 90);
        if (icon != null) {
            editProfileImageLabel.setIcon(icon);
            editProfileImageLabel.setText("");
        } else {
            editProfileImageLabel.setIcon(null);
            editProfileImageLabel.setText("🙂");
            editProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }
    }

    // =========================
    // [D] Save 시 UI 내용을 모델에 반영하고 View 화면도 갱신
    // =========================
    private void saveEditToModel() {
        profileData.setName(txtName.getText().trim());
        profileData.setStatusMessage(txtStatus.getText().trim());
        profileData.setProfileImagePath(tempProfileImagePath);
        profileData.setBackgroundImagePath(tempBackgroundImagePath);

        viewNameLabel.setText(profileData.getName());

        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel.setText("“ " + status + " ”");

        ImageIcon icon;
        if (profileData.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(profileData.getProfileImageIcon(), 90, 90);
        } else {
            icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        }

        if (icon != null) {
            viewProfileImageLabel.setIcon(icon);
            viewProfileImageLabel.setText("");
        } else {
            viewProfileImageLabel.setIcon(null);
            viewProfileImageLabel.setText("🙂");
            viewProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }

        cardPanel.repaint();
    }

    // =========================
    // [E] 이미지 파일 선택
    // - isProfile true면 프로필 사진
    // - false면 배경 사진
    // =========================
    private void chooseImageFile(boolean isProfile) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (file == null || !file.exists()) return;

        if (isProfile) {
            tempProfileImagePath = file.getAbsolutePath();
            ImageIcon icon = loadImageIcon(tempProfileImagePath, 90, 90);
            if (icon != null) {
                editProfileImageLabel.setIcon(icon);
                editProfileImageLabel.setText("");
            }
        } else {
            tempBackgroundImagePath = file.getAbsolutePath();
        }

        cardPanel.repaint();
    }

    // =========================
    // [F] 이미지 로딩 유틸
    // =========================
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

    // =========================
    // [G] 배경(헤더) 이미지를 그리는 패널
    // - View일 때는 profileData 아이콘이 있으면 우선 사용
    // - Edit일 때는 tempBackgroundImagePath를 우선 사용
    // =========================
    private class BackgroundPanel extends JPanel {
        private String lastBgPath;
        private Image bgImage;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            ImageIcon icon = null;
            if (!editing && profileData.getBackgroundImageIcon() != null) {
                icon = profileData.getBackgroundImageIcon();
            }

            String bgPath;
            if (editing && tempBackgroundImagePath != null && !tempBackgroundImagePath.isEmpty()) bgPath = tempBackgroundImagePath;
            else bgPath = profileData.getBackgroundImagePath();

            Graphics2D g2 = (Graphics2D) g;

            if (icon != null && icon.getImage() != null) {
                g2.drawImage(icon.getImage(), 0, 0, w, HEADER_HEIGHT, this);
            } else {
                if (bgPath == null || bgPath.isEmpty()) {
                    bgImage = null;
                    lastBgPath = null;
                } else if (!bgPath.equals(lastBgPath)) {
                    lastBgPath = bgPath;
                    bgImage = loadBackgroundImage(bgPath);
                }

                if (bgImage != null) g2.drawImage(bgImage, 0, 0, w, HEADER_HEIGHT, this);
                else {
                    g2.setColor(new Color(220, 220, 220));
                    g2.fillRect(0, 0, w, HEADER_HEIGHT);
                }
            }

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
