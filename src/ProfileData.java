// ProfileData.java
import javax.swing.*;
import java.awt.*;

public class ProfileData {
    private String name;
    private String statusMessage;
    private String profileImagePath;
    private String backgroundImagePath;

    // 실시간 동기화용 실제 이미지 아이콘
    private ImageIcon profileImageIcon;      // 수정함
    private ImageIcon backgroundImageIcon;   // 수정함

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
                "/icons/profile_bg_default.png");
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    public String getBackgroundImagePath() { return backgroundImagePath; }
    public void setBackgroundImagePath(String backgroundImagePath) { this.backgroundImagePath = backgroundImagePath; }

    public ImageIcon getProfileImageIcon() { return profileImageIcon; }                 // 수정함
    public void setProfileImageIcon(ImageIcon profileImageIcon) {                      // 수정함
        this.profileImageIcon = profileImageIcon;                                      // 수정함
    }

    public ImageIcon getBackgroundImageIcon() { return backgroundImageIcon; }          // 수정함
    public void setBackgroundImageIcon(ImageIcon backgroundImageIcon) {                // 수정함
        this.backgroundImageIcon = backgroundImageIcon;                                // 수정함
    }

    // 화면용 스케일
    public static ImageIcon scaleIcon(ImageIcon src, int w, int h) {                   // 수정함
        if (src == null) return null;                                                   // 수정함
        Image img = src.getImage();                                                     // 수정함
        if (img == null) return null;                                                   // 수정함
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);                 // 수정함
        return new ImageIcon(scaled);                                                   // 수정함
    }
}
