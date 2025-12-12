// ProfileData.java
import javax.swing.*;
import java.awt.*;

// 사용자 프로필 정보를 담는 데이터 클래스
// - 이름, 상태메시지, 프로필 이미지, 배경 이미지 관리
// - 서버에서 받은 이미지(byte)를 Icon으로 보관해 실시간 반영 가능
public class ProfileData {

    // -------------------- 기본 프로필 정보 --------------------
    private String name;                 // 표시 이름
    private String statusMessage;         // 상태 메시지
    private String profileImagePath;      // 프로필 이미지 경로
    private String backgroundImagePath;   // 배경 이미지 경로

    // -------------------- 실시간 동기화용 이미지 --------------------
    // 서버에서 전송받은 이미지 바이트를 ImageIcon으로 저장
    private ImageIcon profileImageIcon;
    private ImageIcon backgroundImageIcon;

    // -------------------- 전체 필드 생성자 --------------------
    public ProfileData(String name,
                       String statusMessage,
                       String profileImagePath,
                       String backgroundImagePath) {
        this.name = name;
        this.statusMessage = statusMessage;
        this.profileImagePath = profileImagePath;
        this.backgroundImagePath = backgroundImagePath;
    }

    // -------------------- 기본값 생성자 --------------------
    // - 이름만 주어졌을 때 기본 이미지/상태메시지 설정
    public ProfileData(String name) {
        this(
                name,
                "One line Introduction",
                "/icons/tomato_face.png",
                "/icons/profile_bg_default.png"
        );
    }

    // -------------------- 이름 --------------------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // -------------------- 상태 메시지 --------------------
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    // -------------------- 프로필 이미지 경로 --------------------
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    // -------------------- 배경 이미지 경로 --------------------
    public String getBackgroundImagePath() { return backgroundImagePath; }
    public void setBackgroundImagePath(String backgroundImagePath) {
        this.backgroundImagePath = backgroundImagePath;
    }

    // -------------------- 프로필 이미지 아이콘 --------------------
    // 서버에서 받은 이미지가 있으면 이 아이콘을 우선 사용
    public ImageIcon getProfileImageIcon() {
        return profileImageIcon;
    }
    public void setProfileImageIcon(ImageIcon profileImageIcon) {
        this.profileImageIcon = profileImageIcon;
    }

    // -------------------- 배경 이미지 아이콘 --------------------
    public ImageIcon getBackgroundImageIcon() {
        return backgroundImageIcon;
    }
    public void setBackgroundImageIcon(ImageIcon backgroundImageIcon) {
        this.backgroundImageIcon = backgroundImageIcon;
    }

    // -------------------- 아이콘 스케일 유틸 --------------------
    // - 채팅 리스트, 프로필 화면 등에서 크기 맞춰 보여주기용
    public static ImageIcon scaleIcon(ImageIcon src, int w, int h) {
        if (src == null) return null;
        Image img = src.getImage();
        if (img == null) return null;

        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
