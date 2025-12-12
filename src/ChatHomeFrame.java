// ChatHomeFrame.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// 사용자가 로그인에 성공하면 뜨는 메인 홈 화면 (Friends / Chats 탭 + 네트워크 연결)

public class ChatHomeFrame extends JFrame {

    // 서버와 실제로 통신하는 네트워크 담당 객체
    private ClientNet clientNet;

    // 로그인 창에서 넘겨받은 정보들
    private String username;
    private String Ip_adrr;
    private String Port_no;

    // 화면 가운데 들어갈 두 개의 탭(친구 / 채팅)
    private FriendsPanel friendsPanel;
    private ChatsPanel chatsPanel;

    // CardLayout에서 쓸 카드 이름 상수
    private static final String CARD_friends = "CARD_friends";
    private static final String CARD_chat    = "CARD_chat";

    // 여러 화면을 카드처럼 바꿔 보여줄 패널
    private CardLayout cardLayout = new CardLayout();
    private JPanel jp = new JPanel(cardLayout);

    // ------------------- 생성자 -------------------
    public ChatHomeFrame(String username, String Ip_adrr, String Port_no) {
        this.username = username;
        this.Ip_adrr = Ip_adrr;
        this.Port_no = Port_no;

        //가운데에 들어갈 두 패널(친구 / 채팅) 먼저 생성
        friendsPanel = new FriendsPanel(username); // -> 내 이름도 넘기기
        chatsPanel   = new ChatsPanel();

        // chatsPanel이 친구 목록을 사용할 수 있도록 연결
        chatsPanel.setFriendsList(friendsPanel);

        //---------  프레임 기본 설정  ---------
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(null);
        setTitle("Friends");

        getContentPane().setLayout(new BorderLayout());

        // 가운데 카드 패널(jp) 설정
        jp.setBackground(Color.WHITE);
        getContentPane().add(jp, BorderLayout.CENTER);

        // ------- 왼쪽 사이드바(탭 전환 버튼들) -------
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(80, 0));
        left.setBackground(new Color(220, 220, 220));
        left.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        left.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

        getContentPane().add(left, BorderLayout.WEST);

        JButton btnFriends = new JButton("👥");
        JButton btnChats   = new JButton("💬");
        Dimension btnSize = new Dimension(48, 34);

        // 친구 버튼 모양 세팅
        btnFriends.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFriends.setMaximumSize(btnSize);
        btnFriends.setPreferredSize(btnSize);
        btnFriends.setMinimumSize(btnSize);
        btnFriends.setFocusPainted(false);
        btnFriends.setBackground(Color.WHITE);
        btnFriends.setForeground(Color.BLACK);

        // 채팅 버튼 모양 세팅
        btnChats.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnChats.setMaximumSize(btnSize);
        btnChats.setPreferredSize(btnSize);
        btnChats.setMinimumSize(btnSize);
        btnChats.setFocusPainted(false);
        btnChats.setBackground(Color.WHITE);
        btnChats.setForeground(Color.BLACK);

        // // 왼쪽 사이드바에 버튼 실제 배치 : 친구(위) / 채팅 (아래)
        left.add(btnFriends);
        left.add(Box.createVerticalStrut(12));
        left.add(btnChats);
        left.add(Box.createVerticalGlue());

        //  --------------  카드 레이아웃에 실제 화면 등록 ------------------
        // friendsPanel과 chatsPanel은 각각 내부에 실제 JPanel을 가지고 있고,
        // 그 컴포넌트를 getComponent()로 가져와서 카드에 넣는다.
        jp.add(friendsPanel.getComponent(), CARD_friends);
        jp.add(chatsPanel.getComponent(),   CARD_chat);

        // 채팅 탭 버튼 클릭 시
        btnChats.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(jp, CARD_chat);
                // 채팅탭일 때 타이틀
                setTitle("Chats");
            }
        });

        // 친구 탭 버튼 클릭 시
        btnFriends.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(jp, CARD_friends);
                // 친구탭일 때 타이틀
                setTitle("Friends");
            }
        });

        // 처음 화면: 친구탭 보이게
        cardLayout.show(jp, CARD_friends);
        setVisible(true);

        //-------------  이제 네트워크 연결 객체(ClientNet) 생성  -------------
        clientNet = new ClientNet(username, Ip_adrr, Port_no, friendsPanel, chatsPanel);
        // 만들어진 clientNet을 두 패널에 넘겨준다.
        // → 패널들이 버튼을 눌렀을 때 clientNet.SendMessage()를 사용해서 서버와 통신할 수 있게 됨.
        chatsPanel.setClientNet(clientNet);
        friendsPanel.setClientNet(clientNet);
    }
}
