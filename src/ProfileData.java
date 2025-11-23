// ProfileData.java
public class ProfileData {
    private String name;
    private String statusMessage;
    private String profileImagePath;     // 프로필 사진 경로 (리소스 경로나 파일 경로)
    private String backgroundImagePath;  // 배경 사진 경로 (옵션)

    public ProfileData(String name, String statusMessage,
                       String profileImagePath, String backgroundImagePath) {
        this.name = name;
        this.statusMessage = statusMessage;
        this.profileImagePath = profileImagePath;
        this.backgroundImagePath = backgroundImagePath;
    }

    public ProfileData(String name) {
        this(name,
             "One line Introduction",
             "/icons/tomato_face.png",
             "/icons/profile_bg_default.png");  // 기본 값 가정
    }

    // ===== Getter / Setter =====
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    public String getBackgroundImagePath() { return backgroundImagePath; }
    public void setBackgroundImagePath(String backgroundImagePath) { this.backgroundImagePath = backgroundImagePath; }
}
