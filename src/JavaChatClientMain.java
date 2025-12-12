//JavaChatClientMain.java
//Java Client 시작

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// 클라이언트 프로그램의 "시작 로그인 창"을 담당하는 클래스
public class JavaChatClientMain extends JFrame {
    
    private JPanel contentPane;
    private JTextField txtUserName;
    private static final String IPAddress = "127.0.0.1";
    private static final String PortNum   = "30000";

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    // 로그인 창 프레임 생성
                    JavaChatClientMain frame = new JavaChatClientMain();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public JavaChatClientMain() {
    	setTitle("Log in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 300, 400);

        // 바탕이 되는 판넬 생성
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setBackground(Color.WHITE);
        // 이 판넬을 프레임의 contentPane으로 사용
        setContentPane(contentPane);
        contentPane.setLayout(null);// 배치관리자 사용 안 함

        // ------------------ 상단 아이콘 이미지 ------------------
        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/friend.png"));

        // 이미지 사이즈 축소
        Image scaled = icon.getImage().getScaledInstance(80, 100, Image.SCALE_SMOOTH); 
        ImageIcon smallIcon = new ImageIcon(scaled);

        // 이미지 라벨 생성
        JLabel img = new JLabel(smallIcon);
        img.setBounds(105, 30, 80, 100); 
        contentPane.add(img);

        // --------------------  타이틀 라벨  --------------------
         JLabel title = new JLabel("Vegetable Talk");
         title.setFont(new Font("Arial", Font.BOLD, 30));
         title.setForeground(Color.BLACK);
         title.setBounds(40, 150, 260, 40);
         contentPane.add(title);

        // ------------------ Name 라벨 ------------------
         JLabel lblNewLabel = new JLabel("Name:");
         lblNewLabel.setFont(new Font("Arial", Font.BOLD, 17));
         lblNewLabel.setBounds(50, 205, 82, 33);
         contentPane.add(lblNewLabel);


        // ------------------ 이름 입력 텍스트 필드 ------------------
        txtUserName = new JTextField();
        txtUserName.setHorizontalAlignment(SwingConstants.CENTER);
        txtUserName.setBounds(115, 205, 116, 33);
        contentPane.add(txtUserName);
        txtUserName.setColumns(10);

        // ------------------ Log in 버튼 ------------------
        JButton btnConnect = new JButton("Log in");
        btnConnect.setBounds(67, 280, 155, 38);
        contentPane.add(btnConnect);
        btnConnect.setBackground(new Color(220, 220, 220));
        btnConnect.setForeground(Color.BLACK);
        btnConnect.setFocusPainted(false);
        btnConnect.setOpaque(true);
        btnConnect.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        // 버튼 클릭 이벤트
        Myaction action = new Myaction();
        btnConnect.addActionListener(action);
        txtUserName.addActionListener(action);
    }

    // 버튼 클릭하면 실행될 액션이 담긴 "이벤트 리스너 클래스" 작성
    class Myaction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = txtUserName.getText().trim();
            // 상수로 정의된 IP / PORT 사용
            String ip_addr  = IPAddress.trim();
            String port_no  = PortNum.trim();

            // 메인 화면(친구목록/채팅방 탭이 있는 프레임) 생성
            // 이 안에서 ClientNet을 만들고 서버에 접속 + /login 보내는 구조
            ChatHomeFrame home = new ChatHomeFrame(username, ip_addr, port_no);
            setVisible(false);
        }
    }
}
