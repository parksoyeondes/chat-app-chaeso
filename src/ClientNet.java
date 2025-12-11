import javax.swing.ImageIcon;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
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

    // roomId -> ChatRoom
    private Map<String, ChatRoom> roomMap = new HashMap<>();

    // 내 이름
    private String me;

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
            System.out.println("[Client] 서버 연결 성공: " + ip + ":" + port);

            is = socket.getInputStream();
            os = socket.getOutputStream();
            dis = new DataInputStream(is);
            dos = new DataOutputStream(os);

            // 로그인
            SendMessage("/login " + username);

            ListenNetwork net = new ListenNetwork();
            net.start();

        } catch (IOException e) {
            throw new RuntimeException("연결 실패", e);
        }
    }

    class ListenNetwork extends Thread {
        public void run() {
            while (true) {
                try {
                    // ===== 항상 UTF 헤더 먼저 받음 =====
                    String msg = dis.readUTF();
                    if (msg == null) {
                        System.out.println("[Client] readUTF() -> null, 수신 종료");
                        break;
                    }

                    String[] msgs = msg.split(" ", 2);
                    String cmd = msgs[0];
                    String rest = (msgs.length > 1) ? msgs[1] : "";

                    // -------------------- 유저 목록 --------------------
                    if (cmd.equals("/userName")) {
                        String[] names = rest.split(",");
                        if (friendsPanel != null) {
                            friendsPanel.setUserList(names);
                        }

                    } else if (cmd.equals("/newUser")) {
                        String newUser = rest;
                        if (friendsPanel != null) {
                            friendsPanel.addUser(newUser);
                        }

                    // -------------------- 방 열기 -----------------------
                    } else if (cmd.equals("/openRoom")) {
                        String roomId = rest;

                        ChatRoom room = roomMap.get(roomId);
                        if (room == null) {
                            room = new ChatRoom(roomId, ClientNet.this);
                            roomMap.put(roomId, room);
                        } else {
                            room.setVisible(true);
                            room.toFront();
                            room.requestFocus();
                        }

                        if (chatsPanel != null) {
                            chatsPanel.addRoom(roomId);
                        }
                        room.setVisible(true);

                    // -------------------- 텍스트 메시지 -------------------
                    } else if (cmd.equals("/roomMsg")) {
                        String[] parts = msg.split(" ", 3);
                        if (parts.length < 3) continue;

                        String roomId  = parts[1];
                        String chatMsg = parts[2];

                        String senderName = null;
                        String body = chatMsg;

                        if (chatMsg.startsWith("[")) {
                            int idx = chatMsg.indexOf("]");
                            if (idx > 1) {
                                senderName = chatMsg.substring(1, idx);
                                body = chatMsg.substring(idx + 1).trim();
                            }
                        }

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) {
                            room.appendMessage(senderName, body);
                        }

                    // ===================== 이미지 수신 =====================
                    } else if (cmd.equals("/roomImg")) {
                        // rest = "roomId senderName"
                        String[] parts = rest.split(" ", 2);
                        if (parts.length < 2) {
                            System.out.println("[Client] /roomImg 형식 이상: " + rest);
                            continue;
                        }

                        String roomId     = parts[0];
                        String senderName = parts[1];

                        // 바로 뒤에 이미지 길이 + 바이트가 따라옴
                        int length = dis.readInt();
                        if (length <= 0) {
                            System.out.println("[Client] /roomImg length <= 0: " + length);
                            continue;
                        }

                        byte[] buf = new byte[length];
                        dis.readFully(buf);

                        System.out.println("[Client] /roomImg 수신 완료 roomId=" + roomId +
                                " sender=" + senderName + " len=" + length);

                        ImageIcon icon = new ImageIcon(buf);
                        boolean isMine = senderName.equals(me);

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) {
                            room.appendImage(isMine, icon);
                        } else {
                            System.out.println("[Client] roomId=" + roomId + " 채팅방 없음");
                        }

                    // ===================== 행맨 시작 ======================
                    } else if (cmd.equals("/hangStart")) {
                        String[] parts = rest.split(" ");
                        if (parts.length < 3) {
                            System.out.println("[Client] /hangStart 형식 이상: " + rest);
                            continue;
                        }

                        String roomId = parts[0];
                        int wordIdx;
                        int themeIdx;
                        try {
                            wordIdx  = Integer.parseInt(parts[1]);
                            themeIdx = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException ex) {
                            System.out.println("[Client] /hangStart 숫자 파싱 실패: " + rest);
                            continue;
                        }

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) {
                            room.openHangman(wordIdx, themeIdx);
                        } else {
                            System.out.println("[Client] /hangStart room 없음: " + roomId);
                        }

                    } else if (cmd.equals("/hangGuess")) {
                        String[] parts = rest.split(" ");
                        if (parts.length < 2) continue;

                        String roomId = parts[0];
                        char ch = parts[1].charAt(0);

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) {
                            room.applyHangmanGuess(ch);
                        }

                    } else if (cmd.equals("/hangEnd")) {
                        String roomId = rest.trim();

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) {
                            room.closeHangman();
                        }
                    }

                } catch (EOFException eof) {
                    System.out.println("[Client] 서버와 연결 종료됨 (EOF) – 수신 스레드 종료");
                    break;
                } catch (IOException e) {
                    System.out.println("[Client] ListenNetwork IOException: " + e);
                    e.printStackTrace();
                    try {
                        if (dos != null) dos.close();
                        if (dis != null) dis.close();
                        if (socket != null && !socket.isClosed()) socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    public void SendMessage(String msg) {
        try {
            // System.out.println("[Client] SEND: " + msg);
            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException e) {
            System.out.println("[Client] SendMessage IOException: " + e);
            try {
                if (dos != null) dos.close();
                if (dis != null) dis.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public String getUsername() {
        return me;
    }

    // ★★★ 여기! 실제 이름 → 내 클라 기준 표시 이름 ★★★
    public String getDisplayName(String name) {
        if (friendsPanel != null) {
            return friendsPanel.getDisplayName(name);
        }
        return name;
    }

    public void openRoom(String roomId) {
        ChatRoom room = roomMap.get(roomId);

        if (room == null) {
            room = new ChatRoom(roomId, this);
            roomMap.put(roomId, room);
        }

        room.setVisible(true);
        room.toFront();
        room.requestFocus();
    }

    // ================== 이미지 전송 ==================
    public void sendImage(String roomId, File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            System.out.println("[Client] sendImage roomId=" + roomId + " size=" + bytes.length + " bytes");

            // 1) 헤더 UTF
            dos.writeUTF("/roomImg " + roomId + " " + me);

            // 2) 길이 int
            dos.writeInt(bytes.length);

            // 3) 실제 데이터
            dos.write(bytes);
            dos.flush();

        } catch (IOException e) {
            System.out.println("[Client] sendImage IOException: " + e);
            e.printStackTrace();
        }
    }
}
