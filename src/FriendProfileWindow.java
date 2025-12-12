// FriendProfileWindow.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

// 친구 프로필을 보여주는 다이얼로그
// - View 모드(보기) / Edit 모드(별명 수정) 2개의 카드로 구성
// - FriendsPanel에서 더블클릭으로 열리고, Save 시 별명 콜백을 통해 목록에도 반영

public class FriendProfileWindow extends JDialog {

    // =========================
    // [1] 카드 전환(View / Edit)
    // =========================
    private static final String CARD_VIEW = "CARD_VIEW"; // 보기 화면 카드 이름
    private static final String CARD_EDIT = "CARD_EDIT"; // 수정 화면 카드 이름
    private static final int HEADER_HEIGHT = 150;        // 배경(헤더) 이미지 높이

    // =========================
    // [2] 프로필 데이터/식별 정보
    // =========================
    private final ProfileData profileData; // 친구의 프로필 데이터(사진/상메/배경 등)
    private final String realName;         // 실제 ID(서버에서 식별되는 값)
    private String displayName;            // 화면에 보여줄 표시 이름(별명)

    // =========================
    // [3] 저장 콜백
    // - Save 시 외부(FriendsPanel 등)에 변경된 displayName을 전달
    // =========================
    private final Consumer<String> onNameSaved;

    // =========================
    // [4] 카드 레이아웃 컨테이너
    // - cardPanel 안에 View 카드와 Edit 카드를 넣고 CardLayout으로 전환
    // =========================
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // =========================
    // [5] View(보기) 모드 UI 컴포넌트
    // =========================
    private JLabel viewProfileImageLabel; // 프로필 사진(보기)
    private JLabel viewDisplayNameLabel;  // 표시 이름(보기)
    private JLabel viewStatusLabel;       // 상태 메시지(보기)

    // =========================
    // [6] Edit(수정) 모드 UI 컴포넌트
    // =========================
    private JLabel editProfileImageLabel; // 프로필 사진(수정)
    private JTextField txtDisplayName;    // 표시 이름 입력창(수정)
    private JLabel editStatusLabel;       // 상태 메시지(수정에서는 라벨로만 표시)

    // =========================
    // [7] 편집 상태 플래그
    // - 현재는 참고용(원하면 이후 기능 확장할 때 사용 가능)
    // =========================
    private boolean editing = false;

    // =========================
    // [0] 생성자
    // - 다이얼로그 기본 세팅
    // - View 카드 / Edit 카드 생성 후 View로 시작
    // =========================
    public FriendProfileWindow(Frame owner,
                               ProfileData profileData,
                               String realName,
                               String displayName,
                               Consumer<String> onNameSaved) {
        super(owner, "Friend Profile", true); // true = 모달 다이얼로그
        this.profileData = profileData;
        this.realName = realName;

        // displayName이 비어있으면 realName을 기본 표시 이름으로 사용
        this.displayName = (displayName == null || displayName.isEmpty()) ? realName : displayName;

        // 저장 버튼을 눌렀을 때 외부로 알려줄 콜백
        this.onNameSaved = onNameSaved;

        // 다이얼로그 기본 설정
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(owner);

        // 카드 패널을 CENTER에 배치
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cardPanel, BorderLayout.CENTER);

        // 두 카드(View/Edit) 구성
        buildViewCard();
        buildEditCard();

        // 초기 화면은 보기(View) 모드
        cardLayout.show(cardPanel, CARD_VIEW);
    }

    // =========================
    // [A] View(보기) 카드 구성
    // - 배경(헤더) + 프로필 사진 + 표시 이름 + 상태 메시지
    // - Edit 버튼 or 표시이름 더블클릭 → Edit 카드로 전환
    // =========================
    private void buildViewCard() {
        // 배경(헤더)을 그려주는 패널(아래 BackgroundPanel 참고)
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        // 가운데 컨텐츠(프로필사진/이름/상메)
        JPanel center = new JPanel();
        center.setOpaque(false); // 배경은 BackgroundPanel이 그리므로 투명 처리
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

        // ----- 프로필 이미지(보기) -----
        viewProfileImageLabel = new JLabel();
        viewProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ProfileData에 아이콘(바이너리)이 있으면 우선 사용, 없으면 경로로 로딩
        ImageIcon icon = null;
        if (profileData.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(profileData.getProfileImageIcon(), 90, 90);
        } else {
            icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        }

        // 아이콘이 없으면 기본 텍스트(이모지)로 대체
        if (icon != null) viewProfileImageLabel.setIcon(icon);
        else {
            viewProfileImageLabel.setText("🙂");
            viewProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }

        center.add(Box.createVerticalStrut(10));
        center.add(viewProfileImageLabel);
        center.add(Box.createVerticalStrut(18));

        // ----- 표시 이름(보기) -----
        viewDisplayNameLabel = new JLabel(displayName, SwingConstants.CENTER);
        viewDisplayNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewDisplayNameLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        center.add(viewDisplayNameLabel);
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

        // ----- 상태 메시지(보기) -----
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
        btnEdit.setPreferredSize(new Dimension(80, 32));
        btnEdit.setFocusPainted(false);

        // Edit 버튼 클릭 → Edit 모드로 전환
        btnEdit.addActionListener(e -> {
            editing = true;
            enterEditMode();                 // 입력창에 현재 displayName 반영
            cardLayout.show(cardPanel, CARD_EDIT);
            cardPanel.repaint();
        });

        bottom.add(btnEdit);
        card.add(bottom, BorderLayout.SOUTH);

        // 표시이름 더블클릭 → Edit 모드로 전환(빠른 수정)
        viewDisplayNameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editing = true;
                    enterEditMode();         // 입력창에 현재 displayName 반영
                    cardLayout.show(cardPanel, CARD_EDIT);
                    cardPanel.repaint();
                }
            }
        });

        // 카드 패널에 View 카드 등록
        cardPanel.add(card, CARD_VIEW);
    }

    // =========================
    // [B] Edit(수정) 카드 구성
    // - 표시이름을 JTextField로 편집
    // - Cancel: 저장 없이 View로 복귀
    // - Save: displayName 저장 + 콜백 호출 + View 라벨 갱신 후 View로 복귀
    // =========================
    private void buildEditCard() {
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        // 가운데 컨텐츠(프로필사진/이름 입력/상메)
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

        // ----- 프로필 이미지(수정) -----
        editProfileImageLabel = new JLabel();
        editProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon icon = null;
        if (profileData.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(profileData.getProfileImageIcon(), 90, 90);
        } else {
            icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        }

        if (icon != null) editProfileImageLabel.setIcon(icon);
        else {
            editProfileImageLabel.setText("🙂");
            editProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }

        center.add(Box.createVerticalStrut(10));
        center.add(editProfileImageLabel);
        center.add(Box.createVerticalStrut(18));

        // ----- 표시이름 입력창(수정) -----
        txtDisplayName = new JTextField(displayName);
        txtDisplayName.setHorizontalAlignment(JTextField.CENTER);
        txtDisplayName.setBorder(null); // 테두리 제거로 깔끔하게
        txtDisplayName.setFont(new Font("Dialog", Font.BOLD, 18));
        txtDisplayName.setMaximumSize(new Dimension(180, 28));
        txtDisplayName.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(txtDisplayName);
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

        // ----- 상태 메시지(수정 화면에서는 라벨로 보여주기만) -----
        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        editStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        editStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));

        center.add(Box.createVerticalStrut(4));
        center.add(editStatusLabel);
        center.add(Box.createVerticalStrut(30));

        card.add(center, BorderLayout.CENTER);

        // ----- 하단 버튼 영역(Cancel / Save) -----
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(0, 0, 25, 25));

        // Cancel: 저장하지 않고 View로 복귀
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.setBackground(new Color(200, 200, 200));
        btnCancel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnCancel.setFocusPainted(false);

        btnCancel.addActionListener(e -> {
            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        // Save: displayName 저장 + 외부 콜백 + View UI 갱신
        JButton btnSave = new JButton("Save");
        btnSave.setPreferredSize(new Dimension(80, 32));
        btnSave.setBackground(new Color(60, 179, 113));
        btnSave.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);

        btnSave.addActionListener(e -> {
            // 입력이 비어 있으면 기본값(realName)으로 대체
            String newName = txtDisplayName.getText().trim();
            if (newName.isEmpty()) newName = realName;

            // 현재 창 내부 상태 업데이트
            displayName = newName;

            // 바깥(FriendsPanel 등)에 저장 결과 전달(닉네임 맵 갱신 등에 사용)
            if (onNameSaved != null) onNameSaved.accept(displayName);

            // View 카드에 보이는 라벨도 즉시 갱신
            viewDisplayNameLabel.setText(displayName);

            // View로 복귀
            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        // 버튼 좌/우 배치용 래퍼 패널
        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        leftWrap.setBackground(Color.WHITE);
        leftWrap.add(btnCancel);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setBackground(Color.WHITE);
        rightWrap.add(btnSave);

        bottom.add(leftWrap, BorderLayout.WEST);
        bottom.add(rightWrap, BorderLayout.EAST);

        card.add(bottom, BorderLayout.SOUTH);

        // 카드 패널에 Edit 카드 등록
        cardPanel.add(card, CARD_EDIT);
    }

    // =========================
    // [C] Edit 모드 진입 시 입력창 초기화
    // - 현재 displayName을 입력창에 넣어줌
    // =========================
    private void enterEditMode() {
        if (txtDisplayName != null) txtDisplayName.setText(displayName);
    }

    // =========================
    // [D] 이미지 로딩 유틸(프로필 이미지용)
    // - path가 "/..."면 리소스에서 로딩
    // - 아니면 로컬 파일에서 로딩
    // - w,h로 스케일링 후 ImageIcon 반환
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
    // [E] 배경(헤더) 이미지를 직접 그려주는 패널
    // - profileData에 BackgroundImageIcon이 있으면 그걸 우선 사용
    // - 없으면 BackgroundImagePath로 로딩해서 캐싱(lastBgPath) 후 그림
    // - 헤더 아래 영역은 흰색으로 덮어서 카드 배경 완성
    // =========================
    private class BackgroundPanel extends JPanel {
        private String lastBgPath; // 마지막으로 로딩한 배경 경로(같으면 재로딩 방지)
        private Image bgImage;     // 캐싱된 배경 이미지

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            ImageIcon icon = profileData.getBackgroundImageIcon();
            Graphics2D g2 = (Graphics2D) g;

            // 1) 배경 아이콘이 있으면 우선 사용(주로 네트워크로 받은 아이콘)
            if (icon != null && icon.getImage() != null) {
                g2.drawImage(icon.getImage(), 0, 0, w, HEADER_HEIGHT, this);
            } else {
                // 2) 없으면 경로 기반 로딩(리소스 또는 로컬 파일)
                String bgPath = profileData.getBackgroundImagePath();

                // 경로가 없으면 배경 없음 처리
                if (bgPath == null || bgPath.isEmpty()) {
                    bgImage = null;
                    lastBgPath = null;
                } else if (!bgPath.equals(lastBgPath)) {
                    // 경로가 바뀌었을 때만 새로 로딩해서 캐싱
                    lastBgPath = bgPath;
                    bgImage = loadBackgroundImage(bgPath);
                }

                // 배경 이미지가 있으면 그리기, 없으면 기본 회색 헤더
                if (bgImage != null) g2.drawImage(bgImage, 0, 0, w, HEADER_HEIGHT, this);
                else {
                    g2.setColor(new Color(220, 220, 220));
                    g2.fillRect(0, 0, w, HEADER_HEIGHT);
                }
            }

            // 헤더 아래 영역을 흰색으로 덮어서 카드 배경을 통일
            g2.setColor(Color.WHITE);
            g2.fillRect(0, HEADER_HEIGHT, w, h - HEADER_HEIGHT);
        }

        // =========================
        // [E-1] 배경 이미지 로딩(스케일링은 paintComponent에서 drawImage로 맞춤)
        // - 리소스 경로 or 로컬 파일 경로 지원
        // =========================
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
