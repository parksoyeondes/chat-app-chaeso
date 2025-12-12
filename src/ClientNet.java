import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;


// 클라이언트 쪽에서 서버와 실제로 통신(소켓, 프로토콜 처리)을 담당하는 클래스
// 역할 : 서버에서 오는 프로토콜 수신 → FriendsPanel, ChatsPanel, ChatRoom, HangmanPanel에 반영

public class ClientNet {

    // -------------------- 소켓 & 스트림 --------------------
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;

    // -------------------- GUI 패널 참조 --------------------
    private FriendsPanel friendsPanel;
    private ChatsPanel chatsPanel;

    // -------------------- 채팅방 관리 --------------------
    private Map<String, ChatRoom> roomMap = new HashMap<>();
    // 내 아이디(로그인한 유저 이름)
    private String me;

    // -------------------- 생성자 --------------------
    public ClientNet(String username,
                     String ip,
                     String port,
                     FriendsPanel friendsPanel,
                     ChatsPanel chatsPanel) {
        try {
            me = username;
            this.chatsPanel = chatsPanel;
            this.friendsPanel = friendsPanel;

            //서버에 소켓 연결
            socket = new Socket(ip, Integer.parseInt(port));
            System.out.println("[Client] server connect: " + ip + ":" + port);
            
            //연결된 소켓에서 입출력 스트림을 얻고 래핑
            is = socket.getInputStream();
            os = socket.getOutputStream();
            dis = new DataInputStream(is);
            dos = new DataOutputStream(os);

            // 접속 즉시 "로그인" 프로토콜 전송
            // 서버는 이 메시지를 보고 유저 목록에 등록하고, /userName, /newUser 등을 내려보낸다.
            SendMessage("/login " + username);

            //서버에서 오는 메시지를 계속 받는 수신 스레드 시작
            ListenNetwork net = new ListenNetwork();
            net.start();
        } catch (IOException e) {
            throw new RuntimeException("connect fail", e);
        }
    }

    // =========================
    // [4] 채팅방 열기 요청(서버에게 요청)
    // - roomId 예: "park,son"
    // =========================
    public void openRoom(String roomId) {
        if (roomId == null) return;
        roomId = roomId.trim();
        if (roomId.isEmpty()) return;

        SendMessage("/openRoom " + roomId);
    }

    // =========================
    // [4-보조] 로컬에서 즉시 방 열기(테스트/즉시 오픈용)
    // =========================
    public void openRoomLocal(String roomId) {
        if (roomId == null) return;
        roomId = roomId.trim();
        if (roomId.isEmpty()) return;

        ChatRoom room = roomMap.get(roomId);
        if (room == null) {
            room = new ChatRoom(roomId, this);
            roomMap.put(roomId, room);
        } else {
            room.setVisible(true);
            room.toFront();
            room.requestFocus();
        }

        if (chatsPanel != null) chatsPanel.addRoom(roomId);
        room.setVisible(true);
    }
    
    // ------------------------ 서버 수신 스레드 -------------------------

    class ListenNetwork extends Thread {
        public void run() {
            while (true) {
                try {
                    String msg = dis.readUTF();
                    if (msg == null) break;

                    // "명령어 + 나머지" 형태로 1차 분해
                    String[] msgs = msg.split(" ", 2);
                    String cmd = msgs[0];
                    String rest = (msgs.length > 1) ? msgs[1] : "";

                    // ===================== 프로필 텍스트 =====================
                    // 형식: /profileUpdate user encodedPayload
                    // encodedPayload = URL 인코딩된 "표시이름\t상태메시지"
                    if (cmd.equals("/profileUpdate")) {
                        String[] parts = rest.split(" ", 2);
                        if (parts.length < 2) continue;

                        String user = parts[0].trim();// 프로필을 가진 유저 아이디
                        String payload = parts[1];  // 인코딩된 이름+상태 문자열

                        String decoded;
                        try {
                            decoded = URLDecoder.decode(payload, "UTF-8");
                        } catch (Exception ex) {
                            decoded = payload;
                        }

                        // "표시이름\t상태메시지" 로 되어 있으므로 탭으로 분리
                        String[] vals = decoded.split("\t", 2);
                        String displayName = (vals.length >= 1) ? vals[0] : user;
                        String status = (vals.length >= 2) ? vals[1] : "";

                        if (friendsPanel != null) {
                            SwingUtilities.invokeLater(() ->
                                    friendsPanel.updateFriendProfile(user, displayName, status)
                            );
                        }
                        continue;
                    }

                    // ===================== 프로필 사진 =====================
                    // 형식: 서버 → 클라: "/profileImg user ext" (UTF)
                    //  바로 이어서: 이미지 바이트 길이(int) + 이미지 바이트
                    if (cmd.equals("/profileImg")) {
                        String[] parts = rest.trim().split(" ", 2);
                        if (parts.length < 1) continue;
                        
                        String user = parts[0].trim();

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] buf = new byte[length];
                        dis.readFully(buf);

                        ImageIcon icon = new ImageIcon(buf);

                        if (friendsPanel != null) {
                            SwingUtilities.invokeLater(() ->
                                    friendsPanel.updateFriendProfileImage(user, icon)
                            );
                        }
                        continue;
                    }

                    // ===================== 배경 사진 =====================
                    // 형식은 /profileImg와 동일, 적용 위치만 다름
                    if (cmd.equals("/profileBg")) {
                        String[] parts = rest.trim().split(" ", 2);
                        if (parts.length < 1) continue;

                        String user = parts[0].trim();

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] buf = new byte[length];
                        dis.readFully(buf);

                        ImageIcon icon = new ImageIcon(buf);

                        if (friendsPanel != null) {
                            SwingUtilities.invokeLater(() ->
                                    friendsPanel.updateFriendBackgroundImage(user, icon)
                            );
                        }
                        continue;
                    }


                    // -------------------- 유저 목록 전체 세팅 --------------------
                    // 형식: /userName user1,user2,user3...
                    if (cmd.equals("/userName")) {
                        String[] names = rest.split(",");
                        if (friendsPanel != null) {
                            SwingUtilities.invokeLater(() -> friendsPanel.setUserList(names));
                        }
                        continue;
                    }

                    // -------------------- 새 유저 한 명 추가 --------------------
                    // 형식: /newUser username
                    if (cmd.equals("/newUser")) {
                        String newUser = rest.trim();
                        if (friendsPanel != null) {
                            SwingUtilities.invokeLater(() -> friendsPanel.addUser(newUser));
                        }
                        continue;
                    }

                    // -------------------- 방 열기(서버 지시에 의한) --------------------
                    // 형식: /openRoom roomId
                    if (cmd.equals("/openRoom")) {
                        String roomId = rest.trim();

                        // 이미 같은 roomId의 ChatRoom이 있는지 확인
                        ChatRoom room = roomMap.get(roomId);
                        if (room == null) {
                            // 없으면 새로 생성 후 맵에 등록
                            room = new ChatRoom(roomId, ClientNet.this);
                            roomMap.put(roomId, room);
                        } else {
                            // 이미 있으면 창만 앞으로 가져오기
                            room.setVisible(true);
                            room.toFront();
                            room.requestFocus();
                        }

                        // 채팅 패널의 채팅방 목록에도 등록
                        if (chatsPanel != null) chatsPanel.addRoom(roomId);
                        room.setVisible(true);
                        continue;
                    }

                    // ---------------------- 텍스트 메시지 -----------------------
                    // 형식: /roomMsg roomId [sender] 내용...
                    if (cmd.equals("/roomMsg")) {
                        //  ==========  여기서는 msg 전체를 세 부분으로 나눈다 =========
                        String[] parts = msg.split(" ", 3);
                        if (parts.length < 3) continue;

                        String roomId = parts[1]; // 어느 방에서 온 메시지인지
                        String chatMsg = parts[2];

                        String senderName = null; // 보낸 사람 이름(대괄호 안)
                        String body = chatMsg; // 실제 메시지 본문

                        // "[이름] 내용..." 형태에서 이름을 분리
                        if (chatMsg.startsWith("[")) {
                            int idx = chatMsg.indexOf("]");
                            if (idx > 1) {
                                senderName = chatMsg.substring(1, idx);
                                body = chatMsg.substring(idx + 1).trim();
                            }
                        }

                        // 해당 roomId의 ChatRoom을 찾아서 메시지 추가
                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) room.appendMessage(senderName, body);
                        continue;
                    }

                    // -------------------- 이미지(채팅방) --------------------
                    // 형식 : "/roomImg roomId senderName" (UTF)
                    if (cmd.equals("/roomImg")) {
                        String[] parts = rest.split(" ", 2);
                        if (parts.length < 2) continue;

                        String roomId = parts[0];
                        String senderName = parts[1];

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] buf = new byte[length];
                        dis.readFully(buf);

                        ImageIcon icon = new ImageIcon(buf);
                        boolean isMine = senderName.equals(me);

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) room.appendImage(isMine, icon);
                        continue;
                    }

                    // -------------------- 행맨 게임 시작 --------------------
                    // 형식: /hangStart roomId wordIdx themeIdx
                    if (cmd.equals("/hangStart")) {
                        String[] parts = rest.split(" ");
                        if (parts.length < 3) continue;

                        String roomId = parts[0];
                        int wordIdx;
                        int themeIdx;
                        try {
                            wordIdx = Integer.parseInt(parts[1]);
                            themeIdx = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException ex) {
                            continue;
                        }

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) room.openHangman(wordIdx, themeIdx);
                        continue;
                    }

                    // -------------------- 행맨 글자 추측 --------------------
                    // 형식: /hangGuess roomId ch
                    if (cmd.equals("/hangGuess")) {
                        String[] parts = rest.split(" ");
                        if (parts.length < 2) continue;

                        String roomId = parts[0];
                        char ch = parts[1].charAt(0);

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) room.applyHangmanGuess(ch);
                        continue;
                    }

                    // -------------------- 행맨 게임 종료 --------------------
                    // 형식: /hangEnd roomId
                    if (cmd.equals("/hangEnd")) {
                        String roomId = rest.trim();
                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) room.closeHangman();
                        continue;
                    }

                } catch (EOFException eof) {
                    break;
                } catch (IOException e) {
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

    // -------------------- 서버로 문자열 메시지 전송 --------------------
    public void SendMessage(String msg) {
        try {
            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 내 아이디 조회용
    public String getUsername() {
        return me;
    }

    // 상대 이름을 화면에 표시할 때, 친구 프로필에 저장된 "표시용 이름"으로 바꿔주는 메소드
    public String getDisplayName(String name) {
        if (friendsPanel != null) return friendsPanel.getDisplayName(name);
        return name;
    }

    // ================== 채팅방 이미지 전송 ==================
    // roomId 방으로 file 이미지를 전송하는 메소드.
    // 형식: "/roomImg roomId me" (UTF) + length(int) + 이미지 바이트
    public void sendImage(String roomId, File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            dos.writeUTF("/roomImg " + roomId + " " + me);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================== 프로필 텍스트 전송 ==================
    // 내 프로필(이름 + 상태메시지)을 서버로 보내는 메소드.
    // 내부적으로: name + "\t" + status → URL 인코딩 → /profileUpdate me encodedPayload
    public void sendProfileUpdate(ProfileData myProfile) {
        if (myProfile == null) return;

        String name = myProfile.getName();
        String status = myProfile.getStatusMessage();
        if (name == null) name = "";
        if (status == null) status = "";

        String payload = name + "\t" + status;
        String encoded = URLEncoder.encode(payload, StandardCharsets.UTF_8);
        
        System.out.println("[Client] SEND /profileUpdate payload=" + payload + " encoded=" + encoded);
        SendMessage("/profileUpdate " + me + " " + encoded);
    }

    // ================== 프로필 사진 전송 ==================
    // 내 프로필 사진 파일을 서버로 전송.
    // 형식: "/profileImg me ext" + length + 이미지 바이트
    public void sendMyProfileImage(File file) {
        if (file == null || !file.exists()) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            String ext = "";
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) ext = name.substring(dot + 1).toLowerCase();
            if (ext.isEmpty()) ext = "bin";

            dos.writeUTF("/profileImg " + me + " " + ext);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================== 배경 사진 전송 ==================
    // 내 배경 사진 파일을 서버로 전송.
    // 형식: "/profileBg me ext" + length + 이미지 바이트
    public void sendMyBackgroundImage(File file) {
        if (file == null || !file.exists()) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            String ext = "";
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) ext = name.substring(dot + 1).toLowerCase();
            if (ext.isEmpty()) ext = "bin";

            dos.writeUTF("/profileBg " + me + " " + ext);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

