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

// Friends 탭을 담당하는 패널
// - 상단에 내 프로필 요약(사진 + 이름)을 보여주고 클릭 시 ProfileWindow를 연다
// - 가운데에 친구 리스트를 보여주고 더블클릭 시 FriendProfileWindow를 연다
// - 서버에서 들어오는 프로필 변경(이름 상메 사진 배경)을 실시간으로 반영한다
// - 친구별 표시이름(별명)을 따로 저장해서 리스트/채팅에서 보여줄 수 있다

public class FriendsPanel extends JPanel implements TabView {

    // =========================
    // [1] 내 계정 정보 + 내 프로필 데이터
    // =========================
    private final String myName;         // 내 실제 아이디(로그인 ID)
    private final ProfileData myProfile; // 내 프로필(이름 상메 프로필사진 배경)

    // =========================
    // [2] 친구 리스트 모델/뷰
    // =========================
    private final DefaultListModel<String> model = new DefaultListModel<>(); // 친구 realName 목록 저장
    private final JList<String> friendList = new JList<>(model);             // 화면에 표시되는 리스트

    // =========================
    // [3] 상단 내 프로필 요약 UI
    // =========================
    private JLabel profileImageLabel; // 내 프로필 사진 미리보기 라벨
    private JLabel lblMyName;         // 내 표시 이름 라벨

    // =========================
    // [4] 친구 별명/친구 프로필 캐시
    // =========================
    private final Map<String, String> friendNicknameMap = new HashMap<>();     // realName -> 별명(표시이름)
    private final Map<String, ProfileData> friendProfileMap = new HashMap<>(); // realName -> ProfileData(상메/사진/배경)

    // =========================
    // [5] 기본 친구 아이콘
    // - 친구 프로필 사진이 없을 때 리스트에 보여줄 기본 이미지
    // =========================
    private ImageIcon defaultFriendIcon;

    // =========================
    // [6] 네트워크 객체
    // - 내 프로필 변경사항(이름 상메 사진 배경)을 서버에 전송할 때 사용
    // =========================
    private ClientNet clientNet;

    // =========================
    // [0] 생성자
    // - UI 구성(상단 내 프로필 + 친구 리스트)
    // - 이벤트 연결(내 프로필 클릭, 친구 더블클릭)
    // =========================
    public FriendsPanel(String myName) {
        this.myName = myName;

        // 내 프로필 초기값 설정(기본 이름/상메/프로필아이콘/배경)
        this.myProfile = new ProfileData(
                myName,
                "One line Introduction",
                "/icons/tomato_face.png",
                "/icons/profile_bg_default.png"
        );

        // 친구 리스트에서 사용할 기본 아이콘(아이콘 없을 때)
        defaultFriendIcon = loadProfileIconSimple("/icons/tomato_face.png", 40, 32);

        // 패널 기본 레이아웃/배경
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // =========================
        // [A] 상단 영역
        // - "Friends" 제목
        // - 내 프로필 요약(사진 + 이름)
        // - 구분선
        // =========================
        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        JLabel lblTitle = new JLabel("Friends");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(lblTitle);
        topArea.add(Box.createVerticalStrut(10));

        // 내 프로필 요약 패널(사진 + 이름)
        JPanel myProfilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        myProfilePanel.setBackground(Color.WHITE);
        myProfilePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 내 프로필 사진 라벨 준비(아이콘이 있으면 스케일링해서 표시, 없으면 기본 이모지)
        profileImageLabel = new JLabel();
        ImageIcon icon = null;

        if (myProfile.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(myProfile.getProfileImageIcon(), 55, 45);
        } else {
            icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45);
        }

        if (icon != null) profileImageLabel.setIcon(icon);
        else {
            profileImageLabel.setText("🙂");
            profileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 26));
        }
        myProfilePanel.add(profileImageLabel);

        // 내 표시 이름 라벨
        lblMyName = new JLabel(myProfile.getName());
        lblMyName.setFont(new Font("Dialog", Font.PLAIN, 15));
        myProfilePanel.add(lblMyName);

        // =========================
        // [A-1] 내 프로필 클릭 이벤트
        // - ProfileWindow를 열어서 내 프로필 편집
        // - 저장 후 UI 즉시 반영 + 서버에 변경사항 전송
        // =========================
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
                                // 내 프로필 요약 UI 갱신(상단 이름/사진)
                                refreshMyProfileView();

                                // 서버에도 반영(실시간 업데이트)
                                if (clientNet != null) {
                                    // 텍스트(이름/상메) 전송
                                    clientNet.sendProfileUpdate(myProfile);

                                    // 프로필 사진이 로컬 파일이면 바이트 전송
                                    File pFile = tryFile(myProfile.getProfileImagePath());
                                    if (pFile != null) clientNet.sendMyProfileImage(pFile);

                                    // 배경 사진이 로컬 파일이면 바이트 전송
                                    File bFile = tryFile(myProfile.getBackgroundImagePath());
                                    if (bFile != null) clientNet.sendMyBackgroundImage(bFile);
                                }
                            }
                        }
                );
                dialog.setVisible(true);
            }
        });

        topArea.add(myProfilePanel);
        topArea.add(Box.createVerticalStrut(8));

        // 상단 구분선
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(210, 210, 210));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(separator);

        add(topArea, BorderLayout.NORTH);

        // =========================
        // [B] 친구 리스트 영역
        // - FriendCellRenderer로 아이콘 + 표시이름을 그린다
        // - 더블클릭 시 FriendProfileWindow를 열어서 별명(표시이름)을 수정할 수 있다
        // =========================
        friendList.setFixedCellHeight(40);
        friendList.setBackground(Color.WHITE);
        friendList.setCellRenderer(new FriendCellRenderer());

        // 친구 더블클릭 -> 친구 프로필 창 열기
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

        // 리스트 스크롤 + 여백
        JScrollPane scroll = new JScrollPane(friendList);
        scroll.setBorder(new EmptyBorder(5, 15, 15, 15));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    // =========================
    // [6-1] 네트워크 객체 주입
    // - ChatHomeFrame에서 ClientNet 생성 후 FriendsPanel에 넣어줌
    // =========================
    public void setClientNet(ClientNet clientNet) {
        this.clientNet = clientNet;
    }

    // =========================
    // [A-1-보조] 경로가 로컬 파일이면 File로 반환
    // - "/icons/..." 같은 리소스 경로는 전송 대상 아님(null 반환)
    // =========================
    private File tryFile(String path) {
        if (path == null) return null;
        String t = path.trim();
        if (t.isEmpty()) return null;
        if (t.startsWith("/")) return null;
        File f = new File(t);
        if (!f.exists()) return null;
        return f;
    }

    // =========================
    // [A-2] 내 프로필 요약 UI 갱신
    // - 상단 이름 라벨 업데이트
    // - 상단 프로필 사진 라벨 업데이트
    // =========================
    private void refreshMyProfileView() {
        lblMyName.setText(myProfile.getName());

        ImageIcon icon = null;
        if (myProfile.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(myProfile.getProfileImageIcon(), 55, 45);
        } else {
            icon = loadProfileIconSimple(myProfile.getProfileImagePath(), 55, 45);
        }

        if (icon != null) {
            profileImageLabel.setIcon(icon);
            profileImageLabel.setText("");
        }
        repaint();
    }

    // =========================
    // [B-1] 친구 프로필 창 열기
    // - friendProfileMap에 없으면 ProfileData 생성
    // - FriendProfileWindow에서 별명 저장 시 setFriendNickname으로 반영
    // =========================
    private void openFriendProfile(String realName) {
        if (realName == null || realName.trim().isEmpty()) return;

        ProfileData friendProfile = friendProfileMap.get(realName);
        if (friendProfile == null) {
            friendProfile = new ProfileData(realName);
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

    // =========================
    // [4-1] 친구 별명 저장/삭제
    // - nick이 비어있으면 별명 삭제
    // - 저장 후 리스트 repaint로 즉시 반영
    // =========================
    public void setFriendNickname(String realName, String nick) {
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (nick == null || nick.trim().isEmpty()) friendNicknameMap.remove(key);
        else friendNicknameMap.put(key, nick.trim());

        friendList.repaint();
    }

    // =========================
    // [4-2] 표시 이름 얻기
    // - 별명이 있으면 별명 우선
    // - 없으면 realName 그대로 반환
    // =========================
    public String getDisplayName(String realName) {
        if (realName == null) return "";
        String trimmed = realName.trim();
        if (trimmed.isEmpty()) return "";
        String nick = friendNicknameMap.get(trimmed);
        if (nick != null && !nick.trim().isEmpty()) return nick.trim();
        return trimmed;
    }

    // =========================
    // [실시간 반영] 프로필 텍스트 갱신
    // - 서버에서 /profileUpdate를 받으면 ClientNet이 호출
    // - 내 프로필이면 myProfile도 갱신하고 상단 UI도 갱신
    // - 친구면 friendProfileMap 갱신 + 리스트 표시이름도 즉시 바뀌게 처리
    // =========================
    public void updateFriendProfile(String realName, String displayName, String status) {
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        // 내 프로필 업데이트
        if (key.equals(myName)) {
            myProfile.setName(displayName);
            myProfile.setStatusMessage(status);
            refreshMyProfileView();
            return;
        }

        // 친구 ProfileData 준비
        ProfileData p = friendProfileMap.get(key);
        if (p == null) {
            p = new ProfileData(key);
            friendProfileMap.put(key, p);
        }

        // 친구 프로필 데이터 갱신
        p.setName(displayName);
        p.setStatusMessage(status);

        // 친구 목록에도 이름이 즉시 바뀌도록 별명 맵 업데이트
        setFriendNickname(key, displayName);

        friendList.repaint();
    }

    // =========================
    // [실시간 반영] 프로필 사진 갱신
    // - 서버에서 /profileImg를 받으면 ClientNet이 호출
    // - 내 사진이면 상단 내 프로필 사진도 갱신
    // - 친구면 리스트 렌더러가 icon을 보여주게 됨
    // =========================
    public void updateFriendProfileImage(String realName, ImageIcon icon) {
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (key.equals(myName)) {
            myProfile.setProfileImageIcon(icon);
            refreshMyProfileView();
            return;
        }

        ProfileData p = friendProfileMap.get(key);
        if (p == null) {
            p = new ProfileData(key);
            friendProfileMap.put(key, p);
        }
        p.setProfileImageIcon(icon);

        friendList.repaint();
    }

    // =========================
    // [실시간 반영] 배경 사진 갱신
    // - 서버에서 /profileBg를 받으면 ClientNet이 호출
    // - 친구 프로필 창(BackgroundPanel)이 그릴 때 icon을 쓰게 됨
    // =========================
    public void updateFriendBackgroundImage(String realName, ImageIcon icon) {
        if (realName == null) return;
        String key = realName.trim();
        if (key.isEmpty()) return;

        if (key.equals(myName)) {
            myProfile.setBackgroundImageIcon(icon);
            repaint();
            return;
        }

        ProfileData p = friendProfileMap.get(key);
        if (p == null) {
            p = new ProfileData(key);
            friendProfileMap.put(key, p);
        }
        p.setBackgroundImageIcon(icon);

        repaint();
    }

    // =========================
    // [유틸] 프로필 아이콘 로딩 + 스케일링
    // - 리소스 경로("/icons/...")면 getResource로 로딩
    // - 로컬 파일 경로면 파일로 로딩
    // =========================
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

    // =========================
    // [TabView] 탭에 필요한 기본 메소드들
    // - getTitle: 탭에 표시될 이름
    // - refresh: 탭 갱신 훅(필요하면 구현)
    // - getComponent: 실제 탭에 붙일 컴포넌트 반환
    // =========================
    @Override public String getTitle() { return "Friends"; }
    @Override public void refresh() { }
    @Override public JComponent getComponent() { return this; }

    // =========================
    // [친구목록] 서버에서 전체 유저 목록을 받았을 때 세팅
    // - model(리스트)을 새로 채우고
    // - friendProfileMap에도 기본 ProfileData를 만들어둠
    // =========================
    public void setUserList(String[] names) {
        model.clear();
        if (names == null) return;

        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) continue;

            String trimmed = names[i].trim();
            if (trimmed.isEmpty()) continue;

            if (!model.contains(trimmed)) model.addElement(trimmed);

            friendProfileMap.computeIfAbsent(trimmed, n -> new ProfileData(n));
        }
    }

    // =========================
    // [친구목록] 신규 유저 1명 추가
    // =========================
    public void addUser(String name) {
        if (name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;

        if (!model.contains(trimmed)) model.addElement(trimmed);
        friendProfileMap.computeIfAbsent(trimmed, n -> new ProfileData(n));
    }

    // =========================
    // [채팅방 생성] 친구 리스트(실제ID 배열) 반환
    // - ChatsPanel에서 방 만들기 체크박스 목록으로 사용
    // =========================
    public String[] getFriendsList() {
        int size = model.getSize();
        String[] usersForChat = new String[size];
        for (int i = 0; i < model.size(); i++) usersForChat[i] = model.getElementAt(i);
        return usersForChat;
    }

    // =========================
    // [리스트 렌더러] 친구 한 줄을 아이콘 + 표시이름으로 그리기
    // - ProfileData에 사진(icon)이 있으면 그걸 사용
    // - 없으면 defaultFriendIcon 사용
    // - 표시이름은 getDisplayName(별명 우선)
    // =========================
    private class FriendCellRenderer extends JPanel implements ListCellRenderer<String> {

        private JLabel iconLabel = new JLabel(); // 좌측 아이콘
        private JLabel nameLabel = new JLabel(); // 중앙 이름

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

            // 화면에 보여줄 이름(별명 있으면 별명)
            String displayName = getDisplayName(realName);

            // 친구 프로필 데이터(아이콘 가져오기용)
            ProfileData p = friendProfileMap.get(realName);

            // 보여줄 아이콘 결정
            ImageIcon show = null;
            if (p != null && p.getProfileImageIcon() != null) {
                show = ProfileData.scaleIcon(p.getProfileImageIcon(), 40, 32);
            } else {
                show = defaultFriendIcon;
            }

            // 아이콘/텍스트 적용
            if (show != null) {
                iconLabel.setIcon(show);
                iconLabel.setText("");
            } else {
                iconLabel.setIcon(null);
                iconLabel.setText("🙂");
            }

            nameLabel.setText(displayName);

            // 선택 색상 처리
            if (isSelected) setBackground(new Color(230, 230, 230));
            else setBackground(Color.WHITE);

            return this;
        }
    }
}
