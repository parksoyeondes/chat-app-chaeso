import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientNet {

    // 서버와 연결되는 소켓 및 I/O 스트림들
    Socket socket;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;

    // 화면(친구 목록 / 채팅 목록)과 연결
    private FriendsPanel friendsPanel;
    private ChatsPanel chatsPanel;

    // roomId(예: "손채림,박소연") → 해당 채팅방 창(ChatRoom) 객체
    private Map<String, ChatRoom> roomMap = new HashMap<>();

    // 이 클라이언트 유저 자신의 이름
    private String me;


    // ChatHomeFrame에서 넘겨준 정보로 네트워크 연결 설정
    public ClientNet(String username,
                     String ip,
                     String port,
                     FriendsPanel friendsPanel,
                     ChatsPanel chatsPanel) {
        try {
            me = username;
            this.chatsPanel = chatsPanel;
            this.friendsPanel = friendsPanel;

            socket = new Socket(ip, Integer.parseInt(port));

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // 서버에게 로그인한 유저 이름 전송
            // 형식: "/login 유저이름"
            SendMessage("/login " + username);

            // 서버에서 오는 메시지를 계속 듣는 수신 스레드 시작
            ListenNetwork net = new ListenNetwork();
            net.start();

        } catch (IOException e) {
            throw new RuntimeException("연결 실패", e);
        }
    }


    //서버로 문자열 한 줄을 전송하는 메서드
    class ListenNetwork extends Thread {
        public void run() {
            while (true) {
                try {
                    // 서버에서 한 줄(UTF 문자열) 수신
                    String msg = dis.readUTF();

                    // 1차 파싱: 명령어 부분(cmd)와 나머지(rest) 분리
                    // 예)
                    //   "/userName 손채림,박소연"
                    //   "/newUser 아무개"
                    //   "/openRoom 손채림,박소연"
                    //   "/roomMsg 방아이디 [이름] 메시지..."
                    String[] msgs = msg.split(" ", 2);
                    String cmd = msgs[0];                     // 명령어 부분
                    String rest = (msgs.length > 1) ? msgs[1] : ""; // 뒤에 붙은 나머지 문자열

                    // ------------------------------------
                    // 1) 전체 유저 목록 받는 경우
                    //    형식: /userName 유저1,유저2,유저3
                    // ------------------------------------
                    if (cmd.equals("/userName")) {
                        // rest: "손채림,박소연,아무개"
                        String[] names = rest.split(",");
                        friendsPanel.setUserList(names);

                        // ------------------------------------
                        // 2) 새로운 유저 한 명이 로그인한 경우
                        //    형식: /newUser 유저이름
                        // ------------------------------------
                    } else if (cmd.equals("/newUser")) {
                        String newUser = rest;
                        friendsPanel.addUser(newUser);

                        // ------------------------------------
                        // 3) 채팅방을 열라는 명령
                        //    형식: /openRoom 방아이디
                        //    방아이디 예: "손채림,박소연"
                        // ------------------------------------
                    } else if (cmd.equals("/openRoom")) {
                        String roomId = rest;

                        // 이미 있는 방인지 확인
                        ChatRoom room = roomMap.get(roomId);
                        if (room == null) {
                            // 처음 열리는 방이면 새 ChatRoom 생성 후 map에 등록
                            room = new ChatRoom(roomId, ClientNet.this);
                            roomMap.put(roomId, room);
                        } else {
                            // 이미 만들어진 방이면 다시 보여주기만 함
                            room.setVisible(true);
                            room.toFront();
                            room.requestFocus();
                        }

                        // Chats 탭(채팅 리스트)에도 방 이름 추가
                        if (chatsPanel != null) {
                            chatsPanel.addRoom(roomId);
                        }
                        room.setVisible(true);

                        // ------------------------------------
                        // 4) 채팅방 안의 메시지 전달
                        //    형식: /roomMsg 방아이디 [보낸이] 메시지내용
                        // ------------------------------------
                    } else if (cmd.equals("/roomMsg")) {
                        // 여기서는 원문 전체에서 다시 3부분으로 나눔:
                        //   parts[0] = "/roomMsg"
                        //   parts[1] = roomId
                        //   parts[2] = "[이름] 메시지..."
                        String[] parts = msg.split(" ", 3);
                        if (parts.length < 3) {
                            // 형식이 이상하면 무시
                            continue;
                        }
                        String roomId  = parts[1];
                        String chatMsg = parts[2];

                        String senderName = null; // [] 안의 이름
                        String body = chatMsg;    // 실제 메시지 내용

                        // "[이름] 메시지..." 형식이면 이름과 본문 분리
                        if (chatMsg.startsWith("[")) {
                            int idx = chatMsg.indexOf("]");
                            if (idx > 1) {
                                senderName = chatMsg.substring(1, idx);   // 대괄호 안의 이름
                                body = chatMsg.substring(idx + 1).trim(); // 나머지 텍스트
                            }
                        }

                        // roomId에 해당하는 채팅방 찾기
                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) {
                            // 이름과 메시지 내용을 따로 넘겨서 말풍선으로 표시
                            room.appendMessage(senderName, body);
                        }
                    }

                } catch (IOException e) {
                    // 수신 중 에러가 나면 스트림/소켓 정리 후 루프 종료
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

    // 서버로 문자열 한 줄을 전송하는 메서드
    public void SendMessage(String msg) {
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
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

    //이 클라이언트의 유저 이름 리턴
    public String getUsername() {
        return me;
    }


     //ChatsPanel에서 방 이름을 클릭했을 때
     //이미 있는 방이면 다시 열고, 없으면 새로 생성해서 연다.
    public void openRoom(String roomId) {
        ChatRoom room = roomMap.get(roomId);

        if (room == null) {
            // 혹시 map에 없다면 새로 만들어서 등록
            room = new ChatRoom(roomId, this);
            roomMap.put(roomId, room);
        }

        room.setVisible(true);
        room.toFront();
        room.requestFocus();
    }

}
