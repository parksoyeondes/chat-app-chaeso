// ClientNet.java
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ClientNet {

    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;

    private FriendsPanel friendsPanel;
    private ChatsPanel chatsPanel;

    private Map<String, ChatRoom> roomMap = new HashMap<>();
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

            SendMessage("/login " + username);

            ListenNetwork net = new ListenNetwork();
            net.start();

        } catch (IOException e) {
            throw new RuntimeException("연결 실패", e);
        }
    }

    // ✅ ChatsPanel에서 호출하는 메서드
    // roomId 예: "park,son"
    public void openRoom(String roomId) {
        if (roomId == null) return;
        roomId = roomId.trim();
        if (roomId.isEmpty()) return;

        // 서버에 "이 멤버로 방 열어줘" 요청
        SendMessage("/openRoom " + roomId);
    }

    // (선택) 내가 이미 열린 방을 로컬에서 즉시 열고 싶을 때 쓰는 용도
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

    class ListenNetwork extends Thread {
        public void run() {
            while (true) {
                try {
                    String msg = dis.readUTF();
                    if (msg == null) break;

                    String[] msgs = msg.split(" ", 2);
                    String cmd = msgs[0];
                    String rest = (msgs.length > 1) ? msgs[1] : "";

                    // ===================== 프로필 텍스트 =====================
                    if (cmd.equals("/profileUpdate")) {
                        String[] parts = rest.split(" ", 2);
                        if (parts.length < 2) continue;

                        String user = parts[0].trim();
                        String payload = parts[1];

                        String decoded;
                        try {
                            decoded = URLDecoder.decode(payload, "UTF-8");
                        } catch (Exception ex) {
                            decoded = payload;
                        }

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
                    if (cmd.equals("/profileImg")) {
                        String user = rest.trim();
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
                    if (cmd.equals("/profileBg")) {
                        String user = rest.trim();
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

                    // -------------------- 유저 목록 --------------------
                    if (cmd.equals("/userName")) {
                        String[] names = rest.split(",");
                        if (friendsPanel != null) {
                            SwingUtilities.invokeLater(() -> friendsPanel.setUserList(names));
                        }
                        continue;
                    }

                    if (cmd.equals("/newUser")) {
                        String newUser = rest.trim();
                        if (friendsPanel != null) {
                            SwingUtilities.invokeLater(() -> friendsPanel.addUser(newUser));
                        }
                        continue;
                    }

                    // -------------------- 방 열기 --------------------
                    if (cmd.equals("/openRoom")) {
                        String roomId = rest.trim();

                        ChatRoom room = roomMap.get(roomId);
                        if (room == null) {
                            room = new ChatRoom(roomId, ClientNet.this);
                            roomMap.put(roomId, room);
                        } else {
                            room.setVisible(true);
                            room.toFront();
                            room.requestFocus();
                        }

                        if (chatsPanel != null) chatsPanel.addRoom(roomId);
                        room.setVisible(true);
                        continue;
                    }

                    // -------------------- 텍스트 --------------------
                    if (cmd.equals("/roomMsg")) {
                        String[] parts = msg.split(" ", 3);
                        if (parts.length < 3) continue;

                        String roomId = parts[1];
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
                        if (room != null) room.appendMessage(senderName, body);
                        continue;
                    }

                    // -------------------- 이미지(채팅방) --------------------
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

                    // -------------------- 행맨 --------------------
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

                    if (cmd.equals("/hangGuess")) {
                        String[] parts = rest.split(" ");
                        if (parts.length < 2) continue;

                        String roomId = parts[0];
                        char ch = parts[1].charAt(0);

                        ChatRoom room = roomMap.get(roomId);
                        if (room != null) room.applyHangmanGuess(ch);
                        continue;
                    }

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

    public void SendMessage(String msg) {
        try {
            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return me;
    }

    public String getDisplayName(String name) {
        if (friendsPanel != null) return friendsPanel.getDisplayName(name);
        return name;
    }

    // ================== 채팅방 이미지 전송 ==================
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
    public void sendMyProfileImage(File file) {
        if (file == null || !file.exists()) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            dos.writeUTF("/profileImg " + me);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================== 배경 사진 전송 ==================
    public void sendMyBackgroundImage(File file) {
        if (file == null || !file.exists()) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            dos.writeUTF("/profileBg " + me);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
