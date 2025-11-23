import java.io.*;
import java.net.Socket;

public class ClientNet {
    Socket socket;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;
    private FriendsPanel friendsPanel;  // 🔹 UI 참조 보관
    private ChatsPanel chatsPanel;


    //ChatHomeFrame에서 인자로 넘긴거 생성자로 받기
    public ClientNet(String username, String ip, String port,FriendsPanel friendsPanel, ChatsPanel chatsPanel) {
        try{
            this.chatsPanel = chatsPanel;
            this.friendsPanel = friendsPanel;
            socket = new Socket(ip, Integer.parseInt(port));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            //서버에게 방금 로그인 한 유저이름 전달
            SendMessage("/login " + username); // => 서버에게
            // 수신 스레드 만들기
            ListenNetwork net = new ListenNetwork();
            net.start();

        }catch(IOException e){
            throw new RuntimeException("연결 실패", e);
        }
    }


    // run()을 계속 돌면서 항상 서버에서 오는 메세지 수신 받을 준비
    class ListenNetwork extends Thread {
        public void run() {
            while (true) {
                try {
                    // 서버에게서 읽음
                    String msg = dis.readUTF();
                    String [] msgs = msg.split(" ",2);
                    String cmd = msgs[0];
                    String listname = msgs[1]; // /userName이라는 프로토콜이랑 유저이름,분리함

                    //이제 또 합쳐진 이름 리스트 분리해야함
                    if (cmd.equals("/userName")) {
                        String[] names = listname.split(","); // ["손채림", "박소연", "아무개"]
                        System.out.println("[RECV] /userName, names.length=" + names.length);

                        // UI 갱신
                        friendsPanel.setUserList(names);

                    } else if (cmd.equals("/newUser")) {
                        String newUser = listname; // "새로온사람"
                        // → 목록에 한 명 추가
                        friendsPanel.addUser(newUser);
                    }



                } catch (IOException e) {
                    System.out.println("[ListenNetwork] 예외 발생, 스레드 종료");
                    e.printStackTrace();
                    try {
                        dos.close();
                        dis.close();
                        socket.close();
                        break;
                    } catch (Exception ee) {
                        break;
                    }
                }
            }
        }
    }
    // Server에게 network로 전송
    public void SendMessage(String msg) {
        try {
            // Use writeUTF to send messages
            dos.writeUTF(msg);
        } catch (IOException e) {
           // log("dos.write() error");
            try {
                dos.close();
                dis.close();
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                System.exit(0);
            }
        }
    }
}


