// ChatHomeFrame.java
// 테스트
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// 사용자가 로그인하면 뜨는 메인홈 ( 이건 GUI 용 )
public class ChatHomeFrame extends JFrame {

    private ClientNet clientNet; // ( 네트워크 용 따로 )
    private String username;
    private String Ip_adrr;
    private String Port_no;

    // 여기서는 선언만 해두고
    private FriendsPanel friendsPanel;
    private ChatsPanel chatsPanel;

    // 친구탭 / 채팅탭 카드 이름
    private static final String CARD_friends = "CARD_friends";
    private static final String CARD_chat    = "CARD_chat";
    private CardLayout cardLayout = new CardLayout();
    private JPanel jp = new JPanel(cardLayout);

    // 생성자
    public ChatHomeFrame(String username, String Ip_adrr, String Port_no) {
        this.username = username;
        this.Ip_adrr = Ip_adrr;
        this.Port_no = Port_no;

        // 채팅탭 + 유저 로그인 탭
        friendsPanel = new FriendsPanel(username);
        chatsPanel   = new ChatsPanel();
        chatsPanel.setFriendsList(friendsPanel); // 수정함

        // 기본 배경 깔기
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(null);

        setTitle("Friends");

        getContentPane().setLayout(new BorderLayout());
        jp.setBackground(Color.WHITE);
        getContentPane().add(jp, BorderLayout.CENTER);

        // 왼쪽 사이드바
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(80, 0));
        left.setBackground(new Color(220, 220, 220));
        left.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

        getContentPane().add(left, BorderLayout.WEST);

        JButton btnFriends = new JButton("👥");
        JButton btnChats   = new JButton("💬");
        Dimension btnSize = new Dimension(48, 34);

        btnFriends.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFriends.setMaximumSize(btnSize);
        btnFriends.setPreferredSize(btnSize);
        btnFriends.setMinimumSize(btnSize);
        btnFriends.setFocusPainted(false);
        btnFriends.setBackground(Color.WHITE);
        btnFriends.setForeground(Color.BLACK);

        btnChats.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnChats.setMaximumSize(btnSize);
        btnChats.setPreferredSize(btnSize);
        btnChats.setMinimumSize(btnSize);
        btnChats.setFocusPainted(false);
        btnChats.setBackground(Color.WHITE);
        btnChats.setForeground(Color.BLACK);

        left.add(btnFriends);
        left.add(Box.createVerticalStrut(12));
        left.add(btnChats);
        left.add(Box.createVerticalGlue());

        // 가운데 카드 등록
        jp.add(friendsPanel.getComponent(), CARD_friends);
        jp.add(chatsPanel.getComponent(),   CARD_chat);

        btnChats.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(jp, CARD_chat);
                chatsPanel.refresh();
                setTitle("Chats");
            }
        });

        btnFriends.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(jp, CARD_friends);
                friendsPanel.refresh();
                setTitle("Friends");
            }
        });

        cardLayout.show(jp, CARD_friends);

        setVisible(true);

        // 통신을 위한 소켓생성 -> 이걸 ClientNet에서 할거임
        clientNet = new ClientNet(username, Ip_adrr, Port_no, friendsPanel, chatsPanel);
        chatsPanel.setClientNet(clientNet);    // 수정함
        friendsPanel.setClientNet(clientNet);  // 수정함
    }
}
