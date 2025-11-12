import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

// 사용자가 로그인하면 뜨는 메인홈
public class ChatHomeFrame extends JFrame {

    //친구탭 누르면 그 탭이 맨앞으로, 방 탭 누르면 그 탭이 앞으로 -> CardLayout씀
    // 카드 이름 = ( 친구탭 , 채팅방탭 )
    private static final String CARD_chat   = "CARD_chat";
    private static final String CARD_friends = "CARD_friends";

    //배치관리자 카드레이아웃 생성
    private CardLayout cl = new CardLayout();
    private JPanel jp = new JPanel();

    //( 로그인 사용자 목록, 채팅방 목록 "데이터") 넣을 리스트 모델
    private DefaultListModel<String> chatModel = new DefaultListModel<>();
    JList<String> chatList = new JList<>(chatModel); // 화면 상 보이는 리스트
    private DefaultListModel<String> friendModel = new DefaultListModel<>();
    JList<String> friendList = new JList<>(friendModel);


    //생성자
    public ChatHomeFrame(String username, String Ip_adrr, String Port_no) {
        //기본 배경 깔기

    }
}
