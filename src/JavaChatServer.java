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

    // -------------------------------------------------------------
    // [1] AcceptServer
    // - 서버소켓.accept()를 계속 돌면서
    //   새로 들어온 클라이언트마다 UserService 스레드를 하나씩 생성
    // --------------------------------------------------------------
    class AcceptServer extends Thread {
        public void run() {
            while (true) {
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

    // ----------------------------------------------------------------
    // [2] UserService
    // - 클라이언트 1명당 생성되는 스레드
    // - 이 스레드에서 그 클라이언트가 보낸 메시지를 계속 읽으면서
    //   프로토콜(/openRoom, /roomMsg 등)을 처리
    // -----------------------------------------------------------------

    class UserService extends Thread {

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

        // 현재 접속중인 모든 유저 이름을 "유저1,유저2,..." 문자열로 바꿈
        public String getCurrentUserList() {
            StringBuilder builder = new StringBuilder();
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
        // -------------------------------------------------------------
        // [run()]: 클라이언트에서 오는 메시지를 계속 읽으면서
        //         /openRoom, /roomMsg 등 각종 프로토콜 처리
        // -------------------------------------------------------------
        public void run() {
            while (true) {
                try {
                    String msg = dis.readUTF();
                    msg = msg.trim();
                    // 예:
                    // "/openRoom 아무개,손채림,박소연"
                    // "/roomMsg 아무개,손채림,박소연 안녕~ 난 채림이야"
                    String[] args = msg.split(" ",3); //
                    // [0] 이 명령어 프로토콜이고
                    // [1]가 룸 아이디
                    // [2]이 이 뒤로 오는 나머지 문자열 하나로 덩어리로 받겠다
                    String cmd =  args[0];
                    if (cmd.equals("/openRoom")) {
                        String roomId = args[1]; // 채팅방 소속인들? 무튼 "아무개,손채림,박소연"
                        String[] memberNames = roomId.split(",");
                        // 전체 접속자(user_vc)를 돌면서
                        for (int i = 0; i < user_vc.size(); i++) { // 일단 유저벡터에 순서대로 들어와있는 "전체" 클라 돌기
                            UserService user = user_vc.get(i);
                            if (user.UserName == null) {
                                continue;
                            }
                            // 그리고 두번쨰로 이름 검사하기
                            for (int j = 0; j < memberNames.length; j++) {
                                String name = memberNames[j].trim();

                                // 동일 인물 나왔으면 이제 그 유저에게 메시지 전송
                                if (user.UserName.equals(name)) {
                                    // 이 유저는 이 방 멤버 → 방 열라고! 신호 주는거 그러면 GUI 채팅방 각자 만듦
                                    user.WriteOne("/openRoom " + roomId); //
                                    break;
                                }
                            }
                        }
                    }
                    else if (cmd.equals("/roomMsg")) {
                        // /roomMsg 아무개,손채림,박소연 안녕 난 채림이야

                        if (args.length < 3) {
                            WriteOne("사용법: /roomMsg [roomId] [message]\n");
                            continue;
                        }
                        String roomId = args[1]; //채팅방 유저
                        String body = args[2];   // 클라가 보낸 메세지

                        // 이 유저서비스 스레드의 UserName이 곧 보낸 사람이니
                        String sendText = "[" + UserName + "] " + body; // GUI창에 올라갈 메시지라고 생각하면 됨
                        sendToRoom(roomId, sendText);
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
        // -------------------------------------------------------------
        // [sendToRoom]
        // - roomId에 해당하는 사람들에게만
        //   "/roomMsg roomId [보낸이] 내용..." 형식으로 메시지 전송
        // ------------------------------------------------------------
        public void sendToRoom(String roomId, String sendText) {
            String[] memberNames = roomId.split(",");
            // 전체 접속자(user_vc)를 돌면서
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                if (user.UserName == null) {
                    continue;
                }
                // 이 유저가 roomId에 포함된 멤버인지 검사
                for (int j = 0; j < memberNames.length; j++) {
                    String name = memberNames[j].trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    // 방 멤버가 맞다면 그 유저에게만 /roomMsg 전송
                    if (user.UserName.equals(name)) {
                        user.WriteOne("/roomMsg " + roomId + " " + sendText);
                        break;
                    }
                }
            }
        }

    }
}

