// ChatsPanel.java
import javax.swing.*;
import java.awt.*;

public class ChatsPanel extends JPanel implements TabView {
    private DefaultListModel<String> model = new DefaultListModel<>();
    private JList<String> list = new JList<>(model);

    public ChatsPanel() {
        setLayout(new BorderLayout());
        list.setFixedCellHeight(44);
        add(new JScrollPane(list), BorderLayout.CENTER);

        // 데모용 데이터
        model.addElement("알고리즘 스터디");
        model.addElement("VR 팀방");
    }

    public String getTitle() { return "채팅"; }

    public void refresh() {
        // 나중에 서버/세션에서 최신 목록 갱신
        // ex) model.clear(); for(...) model.addElement(...);
    }

    public JComponent getComponent() { return this; }

    // 외부에서 사용할 수 있는 편의 메서드
    public void addRoom(String name) { model.addElement(name); }
}
