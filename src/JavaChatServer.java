//JavaChatServer.java (Java Chatting Server)

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import static java.rmi.server.LogStream.log;

public class JavaChatServer {

    private ServerSocket socket;
    private Socket client_socket;
    private Vector<UserService> UserVec = new Vector<>();
    // 연결된 사용자를 저장할 벡터, ArrayList와 같이 동적 배열을 만들어주는 컬렉션 객체
    //private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의

    public static void main(String[] args) {   // 스윙 비주얼 디자이너를 이용해 GUI를 만들면 자동으로 생성되는 main 함수
       JavaChatServer server = new JavaChatServer();
    }

    public JavaChatServer() {
        try {
            socket = new ServerSocket(30000);//오픈 소켓 생성
            AcceptServer accept_server = new AcceptServer(); // 블로킹 클라이언트소켓 생성
            accept_server.start();
        } catch (IOException e) {
            log("Server start error: " + e.getMessage());
        }
    }
    
    // 새로운 참가자 accept() 하고 user thread를 새로 생성한다. 한번 만들어서 계속 사용하는 스레드
    class AcceptServer extends Thread {
        public void run() {
            while (true) { // 사용자 접속을 계속해서 받기 위해 while문
                try {
                    client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
                    // User 당 하나씩 전용 Thread 생성
                    UserService new_user = new UserService(client_socket);

                    //만들면서 정보 저장하기
                    //-> 유저 한 명당 "하나씩" 존재하는 스레드객체 넣기 = client_socket임
                    UserVec.add(new_user);
                    new_user.start(); // 만든 객체의 스레드 실행
                } catch (IOException e) {
                    log("!!!! accept 에러 발생... !!!!");
                }
            }
        }
    }

    class UserService extends Thread {
    	//참고로 서버와 클라이언트 사이의 1:1 채팅이 아니기 때문에 
    	//(서버에서는) 서버가 스스로 메시지를 먼저 보낼 일이 없으니(한 클라이언트한테 받은 메시지를 다른 클라이언트들한테 전달만 하면 됩니다)
    	//따라서 run() 안에 작성되어 있는 '보내는 기능을 수행하는 코드'와 '받는 기능을 수행하는 코드'를 이 서버에서는 스레드로 분리할 필요가 없음
    	
        private InputStream is;
        private OutputStream os;
        private DataInputStream dis;
        private DataOutputStream dos;
        private Socket client_socket;
        private Vector<UserService> user_vc; // 제네릭 타입 사용
        private String UserName = "";

        public UserService(Socket client_socket) {
            // 매개변수로 넘어온 소켓 객체 저장 
            this.client_socket = client_socket;
            this.user_vc = UserVec;
            try {
                is = client_socket.getInputStream();
                dis = new DataInputStream(is);
                os = client_socket.getOutputStream();
                dos = new DataOutputStream(os);

                //유저이름
                String line1 = dis.readUTF();      // 제일 처음 연결되면 클라이언트의 SendMessage("/login " + UserName);에 의해 "/login UserName" 문자열이 들어옴
                String[] msg = line1.split(" ");   //line1이라는 문자열을 공백(" ")을 기준으로 분할
                UserName = msg[1].trim();          //분할된 문자열 배열 msg의 두 번째 요소(인덱스 1)를 가져와 trim 메소드를 사용하여 앞뒤의 공백을 제거

                //새로 들어온 클라에게 현재 접속자 전체 목록 내려주기
                WriteOne("/userName " + getCurrentUserList());
                //다른 클라에게도 new 사람이 들어왔다고 알리기
                WriteAll("/newUser " + UserName);


            } catch (Exception e) {
                //AppendText("userService error");
            }
        }

        public String getCurrentUserList() {
            StringBuilder builder = new StringBuilder(); // 모드 유저이름 하나의 문자열로 만들어주는 클래스
            for(int i = 0; i < user_vc.size(); i++){
                UserService user = user_vc.get(i);
                if(user.UserName == null || user.UserName.isEmpty()){
                    continue;
                }
                if(builder.length()>0)builder.append(",");
                builder.append(user.UserName);
            }
            return builder.toString();
        }

        public void logout() {
            UserVec.removeElement(this); // 에러가난 현재 객체를 벡터에서 지운다
        	String br_msg ="["+UserName+"]님이 퇴장 하였습니다.\n";   // 다른 User들에게 전송할 메시지 생성  [추가]
        	WriteAll(br_msg); // 다른 User들에게 전송  [추가]

        }
        
        // 클라이언트로 메시지 전송
        public void WriteOne(String msg) {
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
               // AppendText("dos.write() error");
                try {
                    dos.close();
                    dis.close();
                    client_socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                logout();
            }
        }
        
        //모든 다중 클라이언트에게 순차적으로 채팅 메시지 전달
        public void WriteAll(String str) {  
            for (int i = 0; i < user_vc.size(); i++) {
            	UserService user = user_vc.get(i);     // get(i) 메소드는 user_vc 컬렉션의 i번째 요소를 반환
                user.WriteOne(str);
            }
        }

        public void run() {
        	// dis.readUTF()에서 대기하다가 메시지가 들어오면 -> Write All로 전체 접속한 사용자한테 메시지 전송(단톡방), 이걸 클라이언트별로 무한히 실행
        	// 추가적으로 지금은 dis.readUTF()에서 예외가 발생하면 '예외처리에 의해 정상적으로 스레드가 종료하게 작성'되었으나
        	// '/exit'가 들어와도 종료하게 코드를 추가하면 더 완성도 있는 코드가 됩니다.
        	// 지금은 다양한 사용자 프로토콜(/list, /to, /exit 등)을 정의하고 있지 않지만 추후 /exit 프로토콜 등의 정의시 추가

            while (true) {
                try {
                    String msg = dis.readUTF(); 
                    msg = msg.trim();   //msg를 가져와 trim 메소드를 사용하여 앞뒤의 공백을 제거
                    //AppendText(msg); // server 화면에 출력
                    
                    String[] args = msg.split(" "); // 명령어와 매개변수 분리
                    
                    // 명령어 처리
                    switch (args[1]) {
                        case "/exit": // 종료 명령
                            logout(); // 사용자 로그아웃 처리
                            return; // 스레드 종료

                        case "/list": // 접속자 목록 보기
                            WriteOne("**현재 사용자 목록**\n");
                            for (int i = 0; i < user_vc.size(); i++) {
                                UserService user = user_vc.get(i); // 명시적인 인덱스 접근
                                WriteOne("- " + user.UserName + "\n");
                            }
                            break;

                        case "/to": // 귓속말 처리, [홍길동] /to 신데렐라 안녕~ 반갑다. ^^\n
                            if (args.length < 4) {
                                WriteOne("사용법: /to [username] [message]\n");
                                break;
                            }
                            String targetUser = args[2];
                            String privateMessage = "";
                            for (int i = 3; i < args.length; i++) {
                                privateMessage += args[i];
                                if (i < args.length - 1) privateMessage += " ";
                            }
                            boolean found = false;
                            for (int i = 0; i < user_vc.size(); i++) {
                                UserService user = user_vc.get(i); // 명시적인 인덱스 접근
                                if (user.UserName.equals(targetUser)) {
                                    user.WriteOne("[" + UserName + "님의 귓속말] " + privateMessage + "\n");
                                    WriteOne("[" + UserName + "님의 귓속말] " + privateMessage + "\n");
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                WriteOne("사용자 " + targetUser + "를 찾을 수 없습니다.\n");
                            }
                            break;

                        default: // 일반 메시지 처리
                            WriteAll(msg + "\n"); // 모든 사용자에게 전송
                            break;
                    }
                    
                } catch (IOException e) {
                   // AppendText("dis.readUTF() error");
                    try {
                        dos.close();
                        dis.close();
                        client_socket.close();
                        logout();
                        break;  
                    } catch (Exception ee) {
                        break;
                    } 
                }
            }
        }
        
    }
}

