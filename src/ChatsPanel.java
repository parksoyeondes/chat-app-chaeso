// ChatsPanel.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChatsPanel extends JPanel implements TabView {

    private DefaultListModel<String> model = new DefaultListModel<>();
    private JList<String> chatList = new JList<>(model);

    public ChatsPanel() {

        // 전체 패널 설정
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);   // 전체 흰색

        // =======================
        // 1) 상단 타이틀 ("Chats")
        // =======================
        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(Color.WHITE);
        topArea.setBorder(new EmptyBorder(15, 15, 10, 15));

        JLabel lblTitle = new JLabel("Chats");
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        topArea.add(lblTitle);
        topArea.add(Box.createVerticalStrut(10));  // 살짝 여백

        add(topArea, BorderLayout.NORTH);

        // =======================
        // 2) 채팅방 리스트 + 주변 흰색 처리
        // =======================
        chatList.setFixedCellHeight(44);
        chatList.setFont(new Font("Dialog", Font.PLAIN, 14));
        chatList.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(chatList);

        // 스크롤팬 자체는 테두리/배경 없애기
        scroll.setBorder(null);
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);

        // 회색 안 보이게, 흰색 패널로 한 번 감싸고 여백 주기
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(Color.WHITE);                     // ★ 여기도 흰색
        centerWrapper.setBorder(new EmptyBorder(5, 15, 15, 15));      // 바깥 여백
        centerWrapper.add(scroll, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);

        // 데모용 데이터
        model.addElement("알고리즘 스터디");
        model.addElement("VR 팀방");
    }

    @Override
    public String getTitle() {
        return "Chats";
    }

    @Override
    public void refresh() {
        // 나중에 서버에서 채팅방 목록 갱신
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    public void addRoom(String name) {
        if (!model.contains(name)) {
            model.addElement(name);
        }
    }
}
