import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientNet {

    Socket socket;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;
    private FriendsPanel friendsPanel;
    private ChatsPanel chatsPanel;
    private Map<String, ChatRoom> roomMap = new HashMap<>(); //  roomId 이랑 채팅방 객체?
    private String me;


    //ChatHomeFrame에서 인자로 넘긴거 생성자로 받기
    public ClientNet(String username, String ip, String port,FriendsPanel friendsPanel, ChatsPanel chatsPanel) {
        try{
            me = username; // 나 자신.
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
                    String msg = dis.readUTF();

                    // 1) 우선 명령어(cmd)만 뽑기 ― 최대 2조각
                    //    예:
                    //      "/userName 손채림,박소연"
                    //      "/newUser 아무개"
                    //      "/openRoom 손채림,박소연"
                    //      "/roomMsg 손채림,박소연 [채림] 안녕 난 채리밍야"
                    String[] msgs = msg.split(" ", 2);
                    String cmd = msgs[0];                // 명령어
                    String rest = (msgs.length > 1) ? msgs[1] : ""; // 나머지 문자열

                    if (cmd.equals("/userName")) {
                        // rest = "손채림,박소연,아무개"
                        String[] names = rest.split(",");
                        friendsPanel.setUserList(names);

                    } else if (cmd.equals("/newUser")) {
                        String newUser = rest;
                        friendsPanel.addUser(newUser);

                    } else if (cmd.equals("/openRoom")) {
                        String roomId = rest; // "손채림,박소연"

                        // 이미 있는 방인지 확인
                        ChatRoom room = roomMap.get(roomId);
                        if (room == null) {
                            room = new ChatRoom(roomId, ClientNet.this);
                            roomMap.put(roomId, room);
                        } else {
                            // 이미 있는 방이라면 다시 보이게 하기
                            room.setVisible(true);
                            room.toFront();
                            room.requestFocus();
                        }

                        // Chats 탭 리스트에도 방 이름 보여주기
                        if (chatsPanel != null) {
                            chatsPanel.addRoom(roomId);
                        }
                        room.setVisible(true);

                    } else if (cmd.equals("/roomMsg")) {
                        // 형식: /roomMsg roomId [이름] 메시지...
                        String[] parts = msg.split(" ", 3);
                        if (parts.length < 3) {
                            // 형식 이상하면 무시
                            continue;
                        }
                        String roomId  = parts[1]; // "손채림,박소연"
                        String chatMsg = parts[2]; // "[채림] 안녕 난 채림이야"

                        String senderName = null;
                        String body = chatMsg;

                        //  [이름] 떼어내기
                        if (chatMsg.startsWith("[")) {
                            int idx = chatMsg.indexOf("]");
                            if (idx > 1) {
                                senderName = chatMsg.substring(1, idx);        // [] 안
                                body = chatMsg.substring(idx + 1).trim();      // 나머지
                            }
                        }

                        //roomId에 해당하는 방만 찾아서 append
                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) {
                            room.appendMessage(senderName, body);   // <-- 문자열 아니라 "이름 + 내용" 전달
                        }
                    }

                } catch (IOException e) {
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
    // 이 클라의 주인공, 즉 이름 얻기
    public String getUsername() {
        return me;
    }
    // ChatPanel에서 클릭하면 채팅방이 다시 열리게 ( 창을 닫았을 경우 )
    public void openRoom(String roomId) {
        ChatRoom room = roomMap.get(roomId);

        if (room == null) {
            // (이 경우는 거의 없겠지만 안전하게)
            room = new ChatRoom(roomId, this);
            roomMap.put(roomId, room);
        }

        room.setVisible(true);
        room.toFront();
        room.requestFocus();
    }

}


