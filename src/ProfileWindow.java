// ProfileWindow.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;

public class ProfileWindow extends JDialog {

    private static final String CARD_VIEW = "CARD_VIEW";
    private static final String CARD_EDIT = "CARD_EDIT";

    // 회색 헤더 높이 (배경 이미지 영역)
    private static final int HEADER_HEIGHT = 150;

    private final ProfileData profileData;
    private final Runnable onSavedCallback;  // 저장 후 FriendsPanel 갱신용

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // VIEW 모드 컴포넌트
    private JLabel viewProfileImageLabel;
    private JLabel viewNameLabel;
    private JLabel viewStatusLabel;

    // EDIT 모드 컴포넌트
    private JLabel editProfileImageLabel;
    private JTextField txtName;
    private JTextField txtStatus;

    // 임시 편집값 (Save 누르기 전까지 여기만 바뀜)
    private String tempProfileImagePath;
    private String tempBackgroundImagePath;

    // 지금 편집 모드인지 여부 (배경 미리보기용)
    private boolean editing = false;

    public ProfileWindow(Frame owner, ProfileData profileData, Runnable onSavedCallback) {
        super(owner, "My Profile", true);   // 다이얼로그 제목
        this.profileData = profileData;
        this.onSavedCallback = onSavedCallback;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 로그인/메인홈(300 x 400) 과 사이즈 맞추기
        setSize(300, 400);
        setLocationRelativeTo(owner);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cardPanel, BorderLayout.CENTER);

        buildViewCard();
        buildEditCard();

        cardLayout.show(cardPanel, CARD_VIEW);
    }

    // ============================
    // 1. VIEW 카드 (Edit 버튼 있는 화면)
    // ============================
    private void buildViewCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        BackgroundPanel mainArea = new BackgroundPanel();
        mainArea.setOpaque(false);
        mainArea.setLayout(new BoxLayout(mainArea, BoxLayout.Y_AXIS));
        mainArea.setBorder(new EmptyBorder(60, 20, 20, 20));

        // ⚠ VIEW 모드에선 배경 클릭해도 아무 일 안 일어나게 함
        // (마우스 리스너 안 붙임)

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
        mainArea.add(Box.createVerticalStrut(10));
        mainArea.add(viewProfileImageLabel);
        mainArea.add(Box.createVerticalStrut(18));

        // 이름
        viewNameLabel = new JLabel(profileData.getName(), SwingConstants.CENTER);
        viewNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewNameLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        mainArea.add(viewNameLabel);
        mainArea.add(Box.createVerticalStrut(10));

        // 이름 밑 구분선
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));   // 연한 회색 선

        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        mainArea.add(sepWrapper);
        mainArea.add(Box.createVerticalStrut(14));

        // 상태메시지
        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        viewStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        mainArea.add(Box.createVerticalStrut(4));  // 구분선과 살짝 간격
        mainArea.add(viewStatusLabel);
        mainArea.add(Box.createVerticalStrut(30)); // 아래 여백

        card.add(mainArea, BorderLayout.CENTER);

        // 오른쪽 하단 Edit 버튼
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 0, 20, 20));  // (top, left, bottom, right)

        JButton btnEdit = new JButton("Edit");
        btnEdit.setBackground(new Color(200, 200, 200));
        btnEdit.setPreferredSize(new Dimension(80, 32));
        btnEdit.setFocusPainted(false);
        btnEdit.addActionListener(e -> {
            // 편집 모드 진입
            editing = true;
            enterEditModeFromModel();
            cardLayout.show(cardPanel, CARD_EDIT);
            cardPanel.repaint();
        });
        bottom.add(btnEdit);

        card.add(bottom, BorderLayout.SOUTH);

        cardPanel.add(card, CARD_VIEW);
    }

    // ============================
    // 2. EDIT 카드 (기본 프로필과 동일한 배치 + 수정 가능)
    // ============================
    private void buildEditCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        BackgroundPanel mainArea = new BackgroundPanel();
        mainArea.setOpaque(false);
        mainArea.setLayout(new BoxLayout(mainArea, BoxLayout.Y_AXIS));
        // VIEW와 같은 위쪽 여백
        mainArea.setBorder(new EmptyBorder(60, 20, 20, 20));

        // ✅ EDIT 모드에서만 배경 클릭 → 배경 미리보기 변경
        mainArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;          // 편집 모드 아니면 무시
                if (e.getY() <= HEADER_HEIGHT) {
                    chooseImageFile(false);    // 배경 사진 (temp 값만 바뀜)
                }
            }
        });

        // 프로필 이미지 (클릭하면 사진 변경)
        editProfileImageLabel = new JLabel();
        editProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editProfileImageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editProfileImageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;
                chooseImageFile(true);   // 프로필 사진
            }
        });
        mainArea.add(Box.createVerticalStrut(10));
        mainArea.add(editProfileImageLabel);
        mainArea.add(Box.createVerticalStrut(18));

        // ===== 이름 (VIEW와 거의 같은 위치에 텍스트필드만) =====
        txtName = new JTextField();
        txtName.setHorizontalAlignment(JTextField.CENTER);
        txtName.setBorder(null);
        txtName.setFont(new Font("Dialog", Font.BOLD, 18));
        txtName.setMaximumSize(new Dimension(180, 28));
        txtName.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainArea.add(txtName);
        mainArea.add(Box.createVerticalStrut(10));           // VIEW에서 이름 아래 간격과 동일

        // 이름 밑 구분선 (VIEW와 동일)
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));

        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        mainArea.add(sepWrapper);
        mainArea.add(Box.createVerticalStrut(14));

        // ===== 상태 메시지 (중앙 정렬) =====
        txtStatus = new JTextField();
        txtStatus.setHorizontalAlignment(JTextField.CENTER);
        txtStatus.setBorder(null);
        txtStatus.setFont(new Font("Dialog", Font.PLAIN, 13));
        txtStatus.setMaximumSize(new Dimension(220, 28));
        txtStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainArea.add(Box.createVerticalStrut(4));  // 구분선과의 간격
        mainArea.add(txtStatus);
        mainArea.add(Box.createVerticalStrut(30)); // 아래 여백
        mainArea.add(Box.createVerticalGlue());

        card.add(mainArea, BorderLayout.CENTER);

        // ===== 하단 Cancel / Save 버튼 =====
        JPanel bottomEdit = new JPanel(new BorderLayout());
        bottomEdit.setBackground(Color.WHITE);
        // Save 위치 = VIEW 쪽 Edit 와 동일
        bottomEdit.setBorder(new EmptyBorder(0, 0, 25, 25));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.setBackground(new Color(210, 210, 210));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> {
            // 편집 취소 → temp 값 버리고, 저장하지 않음
            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        JButton btnSave   = new JButton("Save");
        btnSave.setPreferredSize(new Dimension(80, 32));
        btnSave.setBackground(new Color(60, 179, 113));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            // 실제 ProfileData에 반영
            saveEditToModel();
            editing = false;
            if (onSavedCallback != null) onSavedCallback.run();
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        // Cancel = 왼쪽 하단
        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        leftWrap.setBackground(Color.WHITE);
        leftWrap.add(btnCancel);

        // Save = 오른쪽 하단
        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setBackground(Color.WHITE);
        rightWrap.add(btnSave);

        bottomEdit.add(leftWrap, BorderLayout.WEST);
        bottomEdit.add(rightWrap, BorderLayout.EAST);

        card.add(bottomEdit, BorderLayout.SOUTH);

        cardPanel.add(card, CARD_EDIT);
    }

    // ============================
    // 3. EDIT <-> VIEW 데이터 연동
    // ============================
    private void enterEditModeFromModel() {
        // 현재 저장된 값 기준으로 temp 초기화
        tempProfileImagePath    = profileData.getProfileImagePath();
        tempBackgroundImagePath = profileData.getBackgroundImagePath();

        txtName.setText(profileData.getName());
        txtStatus.setText(profileData.getStatusMessage());

        // 프로필 이미지 미리보기
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

    private void saveEditToModel() {
        // Save 눌렀을 때만 진짜 데이터 변경
        profileData.setName(txtName.getText().trim());
        profileData.setStatusMessage(txtStatus.getText().trim());
        profileData.setProfileImagePath(tempProfileImagePath);
        profileData.setBackgroundImagePath(tempBackgroundImagePath);

        // VIEW 화면 동기화
        viewNameLabel.setText(profileData.getName());

        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel.setText("“ " + status + " ”");

        ImageIcon icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        if (icon != null) {
            viewProfileImageLabel.setIcon(icon);
            viewProfileImageLabel.setText("");
        }

        // 배경도 갱신
        cardPanel.repaint();
    }

    // ============================
    // 4. 이미지 파일 선택
    // isProfile == true  → 프로필 사진 (tempProfile에만 반영)
    // isProfile == false → 배경 사진 (tempBackground에만 반영)
    // ============================
    private void chooseImageFile(boolean isProfile) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null && file.exists()) {
                if (isProfile) {
                    // 프로필 사진: tempProfileImagePath만 변경
                    tempProfileImagePath = file.getAbsolutePath();
                    ImageIcon icon = loadImageIcon(tempProfileImagePath, 90, 90);
                    if (icon != null) {
                        editProfileImageLabel.setIcon(icon);
                        editProfileImageLabel.setText("");
                    }
                } else {
                    // 배경 사진: tempBackgroundImagePath만 변경 (ProfileData에는 아직 안 넣음)
                    tempBackgroundImagePath = file.getAbsolutePath();
                    cardPanel.repaint();   // BackgroundPanel이 temp 값으로 다시 그림
                }
            }
        }
    }

    // ============================
    // 5. 이미지 로딩 (아이콘용)
    // ============================
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

    // ============================
    // 6. 위 회색 / 아래 흰색 + 배경 이미지 그리는 패널
    // ============================
    private class BackgroundPanel extends JPanel {

        private String lastBgPath;
        private Image bgImage;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            // 🔥 현재 표시해야 할 배경 경로:
            //   - 편집 중이면 tempBackgroundImagePath
            //   - 아니면 저장된 profileData.getBackgroundImagePath()
            String bgPath;
            if (editing && tempBackgroundImagePath != null && !tempBackgroundImagePath.isEmpty()) {
                bgPath = tempBackgroundImagePath;
            } else {
                bgPath = profileData.getBackgroundImagePath();
            }

            // 배경 이미지 경로가 바뀌었으면 다시 로드
            if (bgPath == null || bgPath.isEmpty()) {
                bgImage = null;
                lastBgPath = null;
            } else if (!bgPath.equals(lastBgPath)) {
                lastBgPath = bgPath;
                bgImage = loadBackgroundImage(bgPath);
            }

            Graphics2D g2 = (Graphics2D) g;

            // 위쪽 헤더 영역(회색 부분)에 배경 이미지 / 회색
            if (bgImage != null) {
                g2.drawImage(bgImage, 0, 0, w, HEADER_HEIGHT, this);
            } else {
                g2.setColor(new Color(220, 220, 220)); // 연한 회색
                g2.fillRect(0, 0, w, HEADER_HEIGHT);
            }

            // 아래 흰색 영역
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
