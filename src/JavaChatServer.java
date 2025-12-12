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

// 채팅 서버의 메인 클래스
//접속할 때마다 UserService 스레드를 하나씩 만들어 관리
// - 각 UserService가 클라이언트의 요청(/openRoom, /roomMsg, /profileImg, /hangStart...)을 처리하고
//   다른 클라이언트들에게 브로드캐스트하거나, 특정 방 멤버에게만 전송하는 구조.

public class JavaChatServer {

    private ServerSocket socket;
    private Socket client_socket;
    private Vector<UserService> UserVec = new Vector<>(); // 접속 중인 모든 사용자 스레드 목록

    public static void main(String[] args) {
        new JavaChatServer();
    }

    public JavaChatServer() {
        try {
            socket = new ServerSocket(30000);
            AcceptServer accept_server = new AcceptServer();
            accept_server.start();
        } catch (IOException e) {
            log("Server start error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------- 클라이언트 접속 담당 스레드 --------------------
    // 새로 접속하는 클라이언트마다 UserService 스레드를 생성해줌
    class AcceptServer extends Thread {
        public void run() {
            while (true) {
                try {
                    // 새 클라이언트 접속이 올 때까지 블로킹
                    client_socket = socket.accept();
                    // 이 클라이언트를 담당할 스레드(UserService) 생성
                    UserService new_user = new UserService(client_socket);
                    UserVec.add(new_user); // 전체 유저 목록에 추가
                    new_user.start(); // 스레드 시작(이제 run()이 돌기 시작)
                } catch (IOException e) {
                    log("!!!! accept 에러 발생... !!!!");
                    e.printStackTrace();
                }
            }
        }
    }

    // ----------------------- 개별 클라이언트 담당 스레드 ------------------------
    class UserService extends Thread {

        private InputStream is;
        private OutputStream os;
        private DataInputStream dis;
        private DataOutputStream dos;
        private Socket client_socket;

        // 모든 접속중인 유저 스레드 목록(공유)
        private Vector<UserService> user_vc;

        // 이 UserService가 담당하는 클라이언트의 이름
        private String UserName = "";

        // 멀티스레드 환경에서 동시에 dos.writeUTF를 하지 않도록 잠그기 위한 락 객체
        private final Object outLock = new Object();

        // ---------------------- 생성자 ----------------------
        public UserService(Socket client_socket) {
            this.client_socket = client_socket;
            this.user_vc = UserVec;// 서버의 유저 목록을 공유

            try {
                is = client_socket.getInputStream();
                dis = new DataInputStream(is);
                os = client_socket.getOutputStream();
                dos = new DataOutputStream(os);

                // 클라이언트가 연결되자마자 첫 번째로 보내는 메시지:
                // "/login UserName"
                String line1 = dis.readUTF();
                String[] msg = line1.split(" ", 2);
                if (msg.length >= 2) UserName = msg[1].trim();
                else UserName = "Unknown";

                // 현재 접속 중인 유저 목록을 이 유저에게만 보내기
                // 형식: /userName user1,user2,user3...
                WriteOne("/userName " + getCurrentUserList());
                // 다른 모든 유저에게 "새 유저가 들어왔다" 알리기
                // 형식: /newUser UserName
                WriteAll("/newUser " + UserName);

            } catch (IOException e) {
                System.out.println("[Server] UserService 생성 중 예외: " + e);
                e.printStackTrace();
            }
        }

        // -------------------- 현재 접속자 목록 CSV 문자열 만들기 --------------------
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

        // -------------------- 로그아웃 처리 --------------------
        public void logout() {
            // 전체 유저 목록에서 자기 자신 제거
            UserVec.removeElement(this);
            String br_msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
            WriteAll(br_msg);
            System.out.println("[Server] 로그아웃: " + UserName);
        }

        // -------------------- UTF 문자열 안전 전송(동기화 포함) --------------------
        private void writeUTFPacket(String msg) throws IOException {
            synchronized (outLock) {
                dos.writeUTF(msg);
                dos.flush();
            }
        }

        // -------------------- 바이너리(이미지 등) 안전 전송 --------------------
        // header: "/profileImg user ext" 같은 문자열
        // bytes : 실제 바이트 데이터
        private void writeBinaryPacket(String header, byte[] bytes) throws IOException {
            synchronized (outLock) {
                dos.writeUTF(header);
                dos.writeInt(bytes.length);
                dos.write(bytes);
                dos.flush();
            }
        }

        // -------------------- 현재 유저(나)에게만 문자열 전송 --------------------
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
                logout(); // 전송 실패 시 로그아웃 처리
            }
        }

        // -------------------- 전체 유저에게 문자열 브로드캐스트 --------------------
        public void WriteAll(String str) {
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                user.WriteOne(str);
            }
        }

        // -------------------- 클라이언트 요청 처리 메인 루프 --------------------
        public void run() {
            while (true) {
                try {
                    String msg = dis.readUTF();
                    if (msg == null) {
                        break;
                    }

                    msg = msg.trim();
                    // 최대 4개까지 토큰으로 분리 (명령어 + 인자들)
                    String[] args = msg.split(" ", 4);
                    String cmd = args[0];

                    // ================== 방 열기 요청 ==================
                    // 클라 → 서버: /openRoom son,park,cherry
                    // 서버는 roomId에 포함된 멤버들에게만 /openRoom roomId를 보내줌.
                    if (cmd.equals("/openRoom")) {
                        String roomId = args[1];
                        String[] memberNames = roomId.split(",");

                        // 전체 접속 유저 목록을 돌면서
                        for (int i = 0; i < user_vc.size(); i++) {
                            UserService user = user_vc.get(i);
                            if (user.UserName == null) continue;

                            // 각 유저가 roomId에 포함되어 있는지 확인
                            for (int j = 0; j < memberNames.length; j++) {
                                String name = memberNames[j].trim();
                                if (user.UserName.equals(name)) {
                                    // 포함되어 있다면, 그 유저에게 /openRoom 전송
                                    user.WriteOne("/openRoom " + roomId);
                                    break;
                                }
                            }
                        }
                    }

                    // ================== 채팅 텍스트 메시지 ==================
                    // 클라 → 서버: /roomMsg roomId 메시지내용...
                    // 서버는 해당 roomId 멤버들에게만 다시 /roomMsg roomId [보낸이] 메시지내용 전송
                    else if (cmd.equals("/roomMsg")) {
                        if (args.length < 3) continue;
                        String roomId = args[1];

                        // msg 전체에서 앞의 "/roomMsg roomId"를 제외하고 나머지를 body로 사용
                        String body   = msg.split(" ", 3)[2];

                        // 서버가 보낸다고 표시할 때는 [UserName]을 앞에 붙여줌
                        String sendText = "[" + UserName + "] " + body;
                        sendToRoom(roomId, sendText);
                    }

                    // ================== 채팅 이미지 메시지 ==================
                    // 클라 → 서버:
                    //   "/roomImg roomId sender" (UTF)
                    //   바로 뒤에 length + 이미지 바이트
                    else if (cmd.equals("/roomImg")) {
                        if (args.length < 3) continue;

                        String roomId = args[1];
                        String sender = args[2];

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] imgBytes = new byte[length];
                        dis.readFully(imgBytes);

                        // 방 멤버들에게 이미지 전송
                        sendImageToRoom(roomId, sender, imgBytes);
                    }

                    // ================== 프로필 메타(이름/상태메시지) ==================
                    // 형식: /profileUpdate user encodedPayload
                    // 서버는 내용을 손대지 않고 그대로 브로드캐스트
                    else if (cmd.equals("/profileUpdate")) {
                        // /profileUpdate user payload
                        WriteAll(msg);
                    }

                    // ================== 프로필 사진 바이트 ==================
                    // 클라 → 서버:
                    //   "/profileImg user ext"
                    //   length + 이미지 바이트
                    // 서버는 이걸 모든 유저에게 다시 브로드캐스트
                    else if (cmd.equals("/profileImg")) {
                        // /profileImg user ext
                        if (args.length < 3) continue;
                        String user = args[1];
                        String ext  = args[2];

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);

                        // 전체에게 전송 시도
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

                    // ================== 배경 사진 바이트 ==================
                    // 클라 → 서버:
                    //   "/profileBg user ext"
                    //   length + 이미지 바이트
                    // 서버는 동일하게 전체 브로드캐스트
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

                    // ================== 행맨 게임 시작 ==================
                    // 클라 → 서버: /hangStart roomId
                    // 서버: 랜덤 단어/테마 인덱스를 뽑아서
                    //       /hangStart roomId wordIdx themeIdx 를 방 전체에 전송
                    else if (cmd.equals("/hangStart")) {
                        if (args.length < 2) continue;
                        String roomId = args[1];

                        Random r = new Random();
                        int wordIdx  = r.nextInt(4); // 예: 0~3 중 랜덤
                        int themeIdx = 0; // 현재는 0으로 고정 (테마 여러 개면 랜덤도 가능)

                        String send = "/hangStart " + roomId + " " + wordIdx + " " + themeIdx;
                        sendRawToRoom(roomId, send);
                    }

                    // ================== 행맨 글자 추측 ==================
                    // 클라 → 서버: /hangGuess roomId letter
                    // 서버: 그대로 방 전체에게 중계
                    else if (cmd.equals("/hangGuess")) {
                        if (args.length < 3) continue;
                        String roomId = args[1];
                        String letter = args[2];

                        String send = "/hangGuess " + roomId + " " + letter;
                        sendRawToRoom(roomId, send);
                    }

                    // ================== 행맨 게임 종료 ==================
                    // 클라 → 서버: /hangEnd roomId
                    // 서버: 방 전체에게 /hangEnd roomId 전송
                    else if (cmd.equals("/hangEnd")) {
                        if (args.length < 2) continue;
                        String roomId = args[1];

                        String send = "/hangEnd " + roomId;
                        sendRawToRoom(roomId, send);
                    }

                } catch (EOFException eof) {
                    // 클라이언트가 소켓을 정상적으로 닫았을 때(EOF)
                    System.out.println("[Server] 클라이언트 EOF (" + UserName + ") - 연결 종료");
                    try {
                        dos.close();
                        dis.close();
                        client_socket.close();
                    } catch (IOException e1) { }
                    logout();
                    break;
                } catch (IOException e) {
                    // 통신 중 일반적인 IOException
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

        // -------------------- 텍스트를 방 멤버들에게만 전송 --------------------
        // roomId : "son,park,cherry" 같은 CSV
        // sendText : "[보낸이] 내용..." 형식의 실제 메시지
        public void sendToRoom(String roomId, String sendText) {
            String[] memberNames = roomId.split(",");
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                if (user.UserName == null) continue;

                for (int j = 0; j < memberNames.length; j++) {
                    String name = memberNames[j].trim();
                    if (name.isEmpty()) continue;

                    // 이 유저가 방 멤버에 포함되어 있으면
                    if (user.UserName.equals(name)) {
                        // 그 유저에게만 /roomMsg 전송
                        user.WriteOne("/roomMsg " + roomId + " " + sendText);
                        break;
                    }
                }
            }
        }

        // -------------------- "raw 문자열"을 방 멤버들에게 전달 --------------------
        // sendToRoom는 /roomMsg를 붙여서 보내지만,
        // sendRawToRoom은 이미 완성된 프로토콜 문자열(msg)을 그대로 보낸다.
        // (행맨 같은 특수 프로토콜을 그대로 중계할 때 사용)
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

        // -------------------- 이미지를 방 멤버들에게 전달 --------------------
        // roomId : 어느 방인지
        // sender : 보낸 유저 이름
        // imgBytes : 이미지 바이트 데이터
        // → 각 멤버에게 "/roomImg roomId sender" + length + imgBytes 전송
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
                            // 전송 실패 시 해당 유저 연결 정리 + 로그아웃
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