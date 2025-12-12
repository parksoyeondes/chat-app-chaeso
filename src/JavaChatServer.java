// JavaChatServer.java
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;

import static java.rmi.server.LogStream.log;

public class JavaChatServer {

    private ServerSocket socket;
    private Socket client_socket;
    private Vector<UserService> UserVec = new Vector<>();

    public static void main(String[] args) {
        new JavaChatServer();
    }

    public JavaChatServer() {
        try {
            socket = new ServerSocket(30000);
            System.out.println("[Server] 서버 시작: 포트 30000");
            AcceptServer accept_server = new AcceptServer();
            accept_server.start();
        } catch (IOException e) {
            log("Server start error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    class AcceptServer extends Thread {
        public void run() {
            while (true) {
                try {
                    client_socket = socket.accept();
                    System.out.println("[Server] 클라이언트 접속: " + client_socket);

                    UserService new_user = new UserService(client_socket);
                    UserVec.add(new_user);
                    new_user.start();
                } catch (IOException e) {
                    log("!!!! accept 에러 발생... !!!!");
                    e.printStackTrace();
                }
            }
        }
    }

    class UserService extends Thread {

        private InputStream is;
        private OutputStream os;
        private DataInputStream dis;
        private DataOutputStream dos;
        private Socket client_socket;
        private Vector<UserService> user_vc;
        private String UserName = "";

        private final Object outLock = new Object();

        public UserService(Socket client_socket) {
            this.client_socket = client_socket;
            this.user_vc = UserVec;
            try {
                is = client_socket.getInputStream();
                dis = new DataInputStream(is);
                os = client_socket.getOutputStream();
                dos = new DataOutputStream(os);

                String line1 = dis.readUTF(); // "/login UserName"
                String[] msg = line1.split(" ", 2);
                if (msg.length >= 2) UserName = msg[1].trim();
                else UserName = "Unknown";

                System.out.println("[Server] 로그인: " + UserName);

                WriteOne("/userName " + getCurrentUserList());
                WriteAll("/newUser " + UserName);

            } catch (IOException e) {
                System.out.println("[Server] UserService 생성 중 예외: " + e);
                e.printStackTrace();
            }
        }

        public String getCurrentUserList() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                if (user.UserName == null || user.UserName.isEmpty()) continue;
                if (builder.length() > 0) builder.append(",");
                builder.append(user.UserName);
            }
            return builder.toString();
        }

        public void logout() {
            UserVec.removeElement(this);
            String br_msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
            WriteAll(br_msg);
            System.out.println("[Server] 로그아웃: " + UserName);
        }

        private void writeUTFPacket(String msg) throws IOException {
            synchronized (outLock) {
                dos.writeUTF(msg);
                dos.flush();
            }
        }

        private void writeBinaryPacket(String header, byte[] bytes) throws IOException {
            synchronized (outLock) {
                dos.writeUTF(header);
                dos.writeInt(bytes.length);
                dos.write(bytes);
                dos.flush();
            }
        }

        public void WriteOne(String msg) {
            try {
                writeUTFPacket(msg);
            } catch (IOException e) {
                System.out.println("[Server] WriteOne IOException(" + UserName + "): " + e);
                e.printStackTrace();
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

        public void WriteAll(String str) {
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                user.WriteOne(str);
            }
        }

        public void run() {
            while (true) {
                try {
                    String msg = dis.readUTF();
                    if (msg == null) {
                        System.out.println("[Server] readUTF() -> null, " + UserName + " 연결 종료");
                        break;
                    }

                    msg = msg.trim();
                    String[] args = msg.split(" ", 4);
                    String cmd = args[0];

                    System.out.println("[Server] RECV(" + UserName + "): " + msg);

                    if (cmd.equals("/openRoom")) {
                        String roomId = args[1];
                        String[] memberNames = roomId.split(",");

                        for (int i = 0; i < user_vc.size(); i++) {
                            UserService user = user_vc.get(i);
                            if (user.UserName == null) continue;

                            for (int j = 0; j < memberNames.length; j++) {
                                String name = memberNames[j].trim();
                                if (user.UserName.equals(name)) {
                                    user.WriteOne("/openRoom " + roomId);
                                    break;
                                }
                            }
                        }
                    }

                    else if (cmd.equals("/roomMsg")) {
                        if (args.length < 3) continue;
                        String roomId = args[1];
                        String body   = msg.split(" ", 3)[2];

                        String sendText = "[" + UserName + "] " + body;
                        sendToRoom(roomId, sendText);
                    }

                    else if (cmd.equals("/roomImg")) {
                        if (args.length < 3) continue;

                        String roomId = args[1];
                        String sender = args[2];

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] imgBytes = new byte[length];
                        dis.readFully(imgBytes);

                        sendImageToRoom(roomId, sender, imgBytes);
                    }

                    // ====== 프로필 메타(이름 상태메시지) ======
                    else if (cmd.equals("/profileUpdate")) {
                        // /profileUpdate user payload
                        WriteAll(msg);
                    }

                    // ====== 프로필 사진 바이트 ======
                    else if (cmd.equals("/profileImg")) {
                        // /profileImg user ext
                        if (args.length < 3) continue;
                        String user = args[1];
                        String ext  = args[2];

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);

                        // 전체에게 브로드캐스트
                        for (int i = 0; i < user_vc.size(); i++) {
                            UserService u = user_vc.get(i);
                            try {
                                u.writeBinaryPacket("/profileImg " + user + " " + ext, bytes);
                            } catch (IOException e) {
                                System.out.println("[Server] /profileImg send fail to " + u.UserName);
                                try {
                                    u.dos.close();
                                    u.dis.close();
                                    u.client_socket.close();
                                } catch (Exception ex) { }
                                u.logout();
                            }
                        }
                    }

                    // ====== 배경 사진 바이트 ======
                    else if (cmd.equals("/profileBg")) {
                        // /profileBg user ext
                        if (args.length < 3) continue;
                        String user = args[1];
                        String ext  = args[2];

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);

                        for (int i = 0; i < user_vc.size(); i++) {
                            UserService u = user_vc.get(i);
                            try {
                                u.writeBinaryPacket("/profileBg " + user + " " + ext, bytes);
                            } catch (IOException e) {
                                System.out.println("[Server] /profileBg send fail to " + u.UserName);
                                try {
                                    u.dos.close();
                                    u.dis.close();
                                    u.client_socket.close();
                                } catch (Exception ex) { }
                                u.logout();
                            }
                        }
                    }

                    else if (cmd.equals("/hangStart")) {
                        if (args.length < 2) continue;
                        String roomId = args[1];

                        Random r = new Random();
                        int wordIdx  = r.nextInt(4);
                        int themeIdx = 0;

                        String send = "/hangStart " + roomId + " " + wordIdx + " " + themeIdx;
                        sendRawToRoom(roomId, send);
                    }

                    else if (cmd.equals("/hangGuess")) {
                        if (args.length < 3) continue;
                        String roomId = args[1];
                        String letter = args[2];

                        String send = "/hangGuess " + roomId + " " + letter;
                        sendRawToRoom(roomId, send);
                    }

                    else if (cmd.equals("/hangEnd")) {
                        if (args.length < 2) continue;
                        String roomId = args[1];

                        String send = "/hangEnd " + roomId;
                        sendRawToRoom(roomId, send);
                    }

                } catch (EOFException eof) {
                    System.out.println("[Server] 클라이언트 EOF (" + UserName + ") - 연결 종료");
                    try {
                        dos.close();
                        dis.close();
                        client_socket.close();
                    } catch (IOException e1) { }
                    logout();
                    break;
                } catch (IOException e) {
                    System.out.println("[Server] run() IOException(" + UserName + "): " + e);
                    e.printStackTrace();
                    try {
                        dos.close();
                        dis.close();
                        client_socket.close();
                    } catch (IOException e1) { }
                    logout();
                    break;
                }
            }
        }

        public void sendToRoom(String roomId, String sendText) {
            String[] memberNames = roomId.split(",");
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                if (user.UserName == null) continue;

                for (int j = 0; j < memberNames.length; j++) {
                    String name = memberNames[j].trim();
                    if (name.isEmpty()) continue;

                    if (user.UserName.equals(name)) {
                        user.WriteOne("/roomMsg " + roomId + " " + sendText);
                        break;
                    }
                }
            }
        }

        public void sendRawToRoom(String roomId, String msg) {
            String[] memberNames = roomId.split(",");
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                if (user.UserName == null) continue;

                for (int j = 0; j < memberNames.length; j++) {
                    String name = memberNames[j].trim();
                    if (name.isEmpty()) continue;

                    if (user.UserName.equals(name)) {
                        user.WriteOne(msg);
                        break;
                    }
                }
            }
        }

        public void sendImageToRoom(String roomId, String sender, byte[] imgBytes) {
            String[] memberNames = roomId.split(",");
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                if (user.UserName == null) continue;

                for (int j = 0; j < memberNames.length; j++) {
                    String name = memberNames[j].trim();
                    if (name.isEmpty()) continue;

                    if (user.UserName.equals(name)) {
                        try {
                            user.writeBinaryPacket("/roomImg " + roomId + " " + sender, imgBytes);
                        } catch (IOException e) {
                            try {
                                user.dos.close();
                                user.dis.close();
                                user.client_socket.close();
                            } catch (Exception ex) { }
                            user.logout();
                        }
                        break;
                    }
                }
            }
        }
    }
}
