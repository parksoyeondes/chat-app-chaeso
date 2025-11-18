import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

// 사용자가 로그인하면 뜨는 메인홈 ( 이건 GUI 용 )
public class ChatHomeFrame extends JFrame {

    private ClientNet clientNet; // ( 네트워크 용 따로 )
    private String username;
    private String Ip_adrr;
    private String Port_no;
    private ChatsPanel chatsPanel = new ChatsPanel();
    private FriendsPanel friendsPanel = new FriendsPanel();

    //친구탭 누르면 그 탭이 맨앞으로, 방 탭 누르면 그 탭이 앞으로 -> CardLayout씀
    // 카드 이름 = ( 친구탭 , 채팅방탭 )
    private static final String CARD_chat   = "CARD_chat";
    private static final String CARD_friends = "CARD_friends";
    private CardLayout cardLayout = new CardLayout();
    private JPanel jp = new JPanel(cardLayout);


    //생성자
    public ChatHomeFrame(String username, String Ip_adrr, String Port_no) {
        this.username = username;
        this.Ip_adrr = Ip_adrr;
        this.Port_no = Port_no;

        //기본 배경 깔기
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //창의 X 버튼을 누르면 프로세스까지 종료하도록 설정.
        setSize(300, 400);
        getContentPane().setLayout(new BorderLayout());
        //프레임의 컨텐트 영역 레이아웃을 BorderLayout으로.
        //북(NORTH)/서(WEST)/중앙(CENTER)/동(EAST)/남(SOUTH) 영역
        jp.setBackground(Color.WHITE);
        getContentPane().add(jp, BorderLayout.CENTER);//중앙(CENTER)에 jp 패널을 추가 + JP핀넬은 현재 카드레이아웃 관리자임

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(80, 0));
        left.setBackground(new Color(220, 220, 220));
        left.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8)); // 안쪽 여백
        getContentPane().add(left, BorderLayout.WEST);

        JButton btnChats   = new JButton("💬");
        JButton btnFriends = new JButton("👥");
        Dimension btnSize = new Dimension(48, 34);

        // btnChats 설정
        btnChats.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnChats.setMaximumSize(btnSize);
        btnChats.setPreferredSize(btnSize);
        btnChats.setMinimumSize(btnSize);
        btnChats.setFocusPainted(false);
        btnChats.setBackground(Color.WHITE);
        btnChats.setForeground(Color.BLACK);

        // btnFriends 설정
        btnFriends.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFriends.setMaximumSize(btnSize);
        btnFriends.setPreferredSize(btnSize);
        btnFriends.setMinimumSize(btnSize);
        btnFriends.setFocusPainted(false);
        btnFriends.setBackground(Color.WHITE);
        btnFriends.setForeground(Color.BLACK);

        left.add(btnChats);
        left.add(Box.createVerticalStrut(12));
        left.add(btnFriends);
        left.add(Box.createVerticalGlue());

        // 가운데 카드 등록
        jp.add(chatsPanel.getComponent(),  CARD_chat);
        jp.add(friendsPanel.getComponent(), CARD_friends);

        // 버튼 → 카드 전환
        btnChats.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(jp, CARD_chat);
                chatsPanel.refresh();
            }
        });
        btnFriends.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(jp, CARD_friends);
                friendsPanel.refresh();
            }
        });
        cardLayout.show(jp, CARD_chat);
        setVisible(true);//이제 GUI는 위에서 끝났고

        // 통신을 위한 소켓생성 -> 이걸 ClientNet에서 할거임 ㄱㄱ
        clientNet = new ClientNet(username, Ip_adrr, Port_no);
    }
}
