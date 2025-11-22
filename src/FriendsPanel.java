// FriendsPanel.java
import javax.swing.*;
import java.awt.*;

public class FriendsPanel extends JPanel implements TabView {
    private DefaultListModel<String> model = new DefaultListModel<>();
    private JList<String> list = new JList<>(model);

    public FriendsPanel() {
        setLayout(new BorderLayout());
        list.setFixedCellHeight(40);
        add(new JScrollPane(list), BorderLayout.CENTER);

        // 데모용 데이터
        model.addElement("홍길동");
        model.addElement("김개발");
    }

    public String getTitle() { return "친구"; }

    public void refresh() {
        // 나중에 서버/세션에서 최신 목록 갱신
    }

    public JComponent getComponent() { return this; }

    public void addFriend(String name) { model.addElement(name); }
    public void removeFriend(String name) { model.removeElement(name); }
}
