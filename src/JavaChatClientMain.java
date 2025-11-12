//JavaChatClientMain.java
//Java Client 시작

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class JavaChatClientMain extends JFrame {
//마이크 테스트 ~ 인텔리~
	private JPanel contentPane;
	private JTextField txtUserName;
	//private JTextField txtIpAddress;
	//private JTextField txtPortNumber;

    private static final String IPAddress = "127.0.0.1";
    private static final String PortNum = "30000";
    /**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
                    // ChatHomeFrame 메인 화면 클래스 생성
					JavaChatClientMain frame = new JavaChatClientMain();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public JavaChatClientMain() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 300, 400);
		contentPane = new JPanel(); // 컴포넌트가 깔릴 발판
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setBackground(new Color(232, 245, 233)); // 연한 초록색 느낌
        //이거 배경색인데 다른 색으로 바꿔도 돼~ 아직 안 정함~
		setContentPane(contentPane); //판넬을 바탕의 근본 컨텐트팬으로 설정
		contentPane.setLayout(null); // 배치관리자 제거함

        // 배경 이미지 넣기
        ImageIcon icon = new ImageIcon(getClass().getResource("/icons/broccoli.png"));
        Image scaled = icon.getImage().getScaledInstance(50, 60, Image.SCALE_SMOOTH);
        ImageIcon smallIcon = new ImageIcon(scaled);
        JLabel img = new JLabel(smallIcon);
        img.setBounds(45, 50, 50, 60);
        contentPane.add(img);

        // 글자 라벨 "broccoli"
        JLabel title = new JLabel("Broccoli");
        title.setFont(new Font("Arial", Font.BOLD, 30)); // 폰트/스타일/크기
        title.setForeground(new Color(34, 85, 55));
        title.setBounds(100, 50, 205, 30);
        contentPane.add(title);
        // 글자 라벨 " Talk"
        JLabel title2 = new JLabel("Talk");
        title2.setFont(new Font("Arial", Font.BOLD, 40)); // 폰트/스타일/크기
        title2.setForeground(new Color(34, 85, 55));
        title2.setBounds(115, 90, 205, 30);
        contentPane.add(title2);

        //유저아이디
        JLabel lblNewLabel = new JLabel("유저 아이디");
        lblNewLabel.setBounds(50, 205, 82, 33);
        contentPane.add(lblNewLabel);
        //유저아이디 : 옆 빈칸임 유저가 입력하는 칸
		txtUserName = new JTextField();
		txtUserName.setHorizontalAlignment(SwingConstants.CENTER);
		txtUserName.setBounds(120, 205, 116, 33);
		contentPane.add(txtUserName);
		txtUserName.setColumns(10);

        // 로그인 버튼
		JButton btnConnect = new JButton("로그인 하기");
		btnConnect.setBounds(70, 290, 155, 38);
		contentPane.add(btnConnect);
        //버튼 클릭 이벤트
		Myaction action = new Myaction();
		btnConnect.addActionListener(action);
		txtUserName.addActionListener(action);

	}

    // 버튼 클릭하면 실행될 액션이 담긴 "이벤트 리스너 클래스" 작성
	class Myaction implements ActionListener // 내부클래스로 액션 이벤트 처리 클래스
	{
		@Override
		public void actionPerformed(ActionEvent e) {
			String username = txtUserName.getText().trim();// 공백 제거 후
			String ip_addr = IPAddress.trim();
			String port_no = PortNum.trim();

            // 자바챗클라이언트 뷰에 넘겨줌
			ChatHomeFrame home = new ChatHomeFrame(username, ip_addr, port_no);
			setVisible(false);   //dispose();  // 창을 완전히 닫고 자원 해제
		}
	}
}
