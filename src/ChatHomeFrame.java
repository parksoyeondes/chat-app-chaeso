import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

// 사용자가 로그인하면 뜨는 메인홈
public class ChatHomeFrame extends JFrame {
    //채팅앱 전체를 대표하는 클라이언트 ( 네트워크를 관리하는 )
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private String username;
    private ChatsPanel chatsPanel = new ChatsPanel();
    private FriendsPanel friendsPanel = new FriendsPanel();

    //친구탭 누르면 그 탭이 맨앞으로, 방 탭 누르면 그 탭이 앞으로 -> CardLayout씀
    // 카드 이름 = ( 친구탭 , 채팅방탭 )
    private static final String CARD_chat   = "CARD_chat";
    private static final String CARD_friends = "CARD_friends";

    //배치관리자 카드레이아웃 생성
    private CardLayout cardLayout = new CardLayout();
    private JPanel jp = new JPanel(cardLayout);



//    //( 로그인 사용자 목록, 채팅방 목록 "데이터") 넣을 리스트 모델
//    private DefaultListModel<String> chatModel = new DefaultListModel<>();
//    JList<String> chatList = new JList<>(chatModel); // 화면 상 보이는 리스트
//
//    private DefaultListModel<String> friendModel = new DefaultListModel<>();
//    JList<String> friendList = new JList<>(friendModel);


    //생성자
    public ChatHomeFrame(String username, String Ip_adrr, String Port_no) {
        this.username = username;


        //기본 배경 깔기
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //창의 X 버튼을 누르면 프로세스까지 종료하도록 설정.
        setSize(300, 400); // 창 크기 설정
        //setLocationRelativeTo(null); // 창을 화면 정중앙에 위치.
        getContentPane().setLayout(new BorderLayout());
        //프레임의 컨텐트 영역 레이아웃을 BorderLayout으로.
        //북(NORTH)/서(WEST)/중앙(CENTER)/동(EAST)/남(SOUTH) 영역으로 배치 가능.
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

        setVisible(true);
        try{
            //소켓 생성
            socket = new Socket(Ip_adrr, Integer.parseInt(Port_no));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

        }catch(IOException e){

        }
    }
}
