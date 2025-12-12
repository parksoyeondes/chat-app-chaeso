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

// 채팅 서버 메인 클래스
// - 포트에서 클라이언트 접속을 받는다
// - 접속한 클라이언트마다 UserService 스레드를 만들어 관리한다
// - 각 UserService가 클라이언트가 보낸 프로토콜을 처리한다
public class JavaChatServer {

    // 서버가 포트를 열고 대기하는 소켓
    private ServerSocket socket;

    // accept로 들어오는 임시 클라이언트 소켓
    private Socket client_socket;

    // 접속 중인 모든 사용자 스레드 목록
    private Vector<UserService> UserVec = new Vector<>();

    public static void main(String[] args) {
        new JavaChatServer();
    }

    // 서버 시작
    public JavaChatServer() {
        try {
            socket = new ServerSocket(30000);
            System.out.println("[Server] Server Start: port 30000");

            // 접속 수락 스레드 시작
            AcceptServer accept_server = new AcceptServer();
            accept_server.start();

        } catch (IOException e) {
            log("Server start error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------- 클라이언트 접속 수락 스레드 --------------------
    // - 새 클라이언트가 접속할 때마다 UserService를 만들어서 목록에 넣고 시작한다
    class AcceptServer extends Thread {
        public void run() {
            while (true) {
                try {
                    client_socket = socket.accept();
                    System.out.println("[Server] Client Connect: " + client_socket);

                    UserService new_user = new UserService(client_socket);
                    UserVec.add(new_user);
                    new_user.start();
                } catch (IOException e) {
                    log("!!!! accept error... !!!!");
                    e.printStackTrace();
                }
            }
        }
    }

    // ----------------------- 개별 클라이언트 담당 스레드 ------------------------
    // - 클라이언트 1명당 1개 스레드
    // - 클라이언트에서 오는 프로토콜을 계속 읽어서 처리한다
    class UserService extends Thread {

        // 네트워크 스트림
        private InputStream is;
        private OutputStream os;
        private DataInputStream dis;
        private DataOutputStream dos;

        // 이 유저의 소켓
        private Socket client_socket;

        // 전체 유저 목록 참조
        private Vector<UserService> user_vc;

        // 이 유저의 로그인 아이디
        private String UserName = "";

        // 같은 유저에게 동시에 write가 겹치면 패킷이 깨질 수 있어서 보호용 락
        private final Object outLock = new Object();

        // ---------------------- 생성자 ----------------------
        // - 스트림 준비
        // - 첫 메시지로 /login UserName 받기
        // - 나에게 현재 접속자 목록 보내기
        // - 모두에게 새 유저 접속 알리기
        public UserService(Socket client_socket) {
            this.client_socket = client_socket;
            this.user_vc = UserVec;

            try {
                is = client_socket.getInputStream();
                dis = new DataInputStream(is);

                os = client_socket.getOutputStream();
                dos = new DataOutputStream(os);

                // 첫 메시지는 /login username 형태라고 가정
                String line1 = dis.readUTF();
                String[] msg = line1.split(" ", 2);

                if (msg.length >= 2) UserName = msg[1].trim();
                else UserName = "Unknown";

                System.out.println("[Server] Log in: " + UserName);

                // 나에게만 현재 접속자 목록 전송
                WriteOne("/userName " + getCurrentUserList());

                // 모두에게 새 유저 접속 알림
                WriteAll("/newUser " + UserName);

            } catch (IOException e) {
                System.out.println("[Server] UserService creation failed: " + e);
                e.printStackTrace();
            }
        }

        // -------------------- 현재 접속자 목록 만들기 --------------------
        // - "user1,user2,user3" 형태로 만든다
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
        // - 전체 목록에서 제거
        // - 모두에게 퇴장 메시지 전송
        public void logout() {
            UserVec.removeElement(this);

            String br_msg = "[" + UserName + "] has left the chat.\n";
            WriteAll(br_msg);

            System.out.println("[Server] Logout: " + UserName);
        }

        // -------------------- 문자열 패킷 전송 --------------------
        // - writeUTF를 스레드 안전하게 보낸다
        private void writeUTFPacket(String msg) throws IOException {
            synchronized (outLock) {
                dos.writeUTF(msg);
                dos.flush();
            }
        }

        // -------------------- 바이너리 패킷 전송 --------------------
        // - header를 writeUTF로 보내고
        // - length와 bytes를 이어서 보낸다
        private void writeBinaryPacket(String header, byte[] bytes) throws IOException {
            synchronized (outLock) {
                dos.writeUTF(header);
                dos.writeInt(bytes.length);
                dos.write(bytes);
                dos.flush();
            }
        }

        // -------------------- 나에게만 전송 --------------------
        // - 전송 실패하면 연결 종료하고 logout 처리
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

        // -------------------- 모두에게 전송 --------------------
        public void WriteAll(String str) {
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                user.WriteOne(str);
            }
        }

        // -------------------- 클라이언트 요청 처리 메인 루프 --------------------
        // - 클라이언트가 보내는 프로토콜을 계속 읽어 처리한다
        public void run() {
            while (true) {
                try {
                    String msg = dis.readUTF();
                    if (msg == null) break;

                    msg = msg.trim();

                    // cmd와 인자를 최대 3덩어리로 분리
                    // /roomMsg 같은 경우 메시지 본문에 공백이 있어도 3번째 덩어리에 통째로 남는다
                    String[] args = msg.split(" ", 3);
                    String cmd = args[0];

                    System.out.println("[Server] RECV(" + UserName + "): " + msg);

                    // ================== 방 열기 요청 ==================
                    // 클라 → 서버: /openRoom a,b,c
                    // 서버 → 방 멤버에게만: /openRoom a,b,c
                    if (cmd.equals("/openRoom")) {
                        if (args.length < 2) continue;

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

                    // ================== 채팅 텍스트 메시지 ==================
                    // 클라 → 서버: /roomMsg roomId body...
                    // 서버 → 방 멤버에게: /roomMsg roomId [UserName] body...
                    else if (cmd.equals("/roomMsg")) {
                        if (args.length < 3) continue;

                        String roomId = args[1];

                        // 메시지 본문은 공백 포함 가능하니까 3번째 덩어리를 통째로 사용
                        String body = msg.split(" ", 3)[2];

                        String sendText = "[" + UserName + "] " + body;
                        sendToRoom(roomId, sendText);
                    }

                    // ================== 채팅 이미지 메시지 ==================
                    // 클라 → 서버:
                    //   /roomImg roomId sender
                    //   이어서 length + bytes
                    // 서버 → 방 멤버에게:
                    //   /roomImg roomId sender
                    //   이어서 length + bytes
                    else if (cmd.equals("/roomImg")) {
                        if (args.length < 3) continue;

                        String roomId = args[1];
                        String sender = args[2];

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] imgBytes = new byte[length];
                        dis.readFully(imgBytes);

                        sendRoomImageToRoom(roomId, sender, imgBytes);
                    }

                    // ================== 프로필 텍스트 업데이트 ==================
                    // 클라 → 서버: /profileUpdate user encodedPayload
                    // 서버 → 모두에게: 그대로 브로드캐스트
                    else if (cmd.equals("/profileUpdate")) {
                        WriteAll(msg);
                    }

                    // ================== 프로필 사진 바이너리 ==================
                    // 클라 → 서버:
                    //   /profileImg user ext
                    //   이어서 length + bytes
                    // 서버 → 모두에게:
                    //   /profileImg user ext
                    //   이어서 length + bytes
                    else if (cmd.equals("/profileImg")) {
                        // 최소 /profileImg user 는 있어야 한다
                        if (args.length < 2) continue;

                        // args는 3덩어리로 split했으니
                        // /profileImg user ext 라면 args[2]에 "ext"가 들어간다
                        String[] tmp = msg.split(" ", 3);
                        if (tmp.length < 2) continue;

                        String user = tmp[1].trim();
                        String ext = (tmp.length >= 3) ? tmp[2].trim() : "";

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);

                        String header = ext.isEmpty()
                                ? "/profileImg " + user
                                : "/profileImg " + user + " " + ext;

                        broadcastProfileBytes(header, bytes);
                    }

                    // ================== 배경 사진 바이너리 ==================
                    // 클라 → 서버:
                    //   /profileBg user ext
                    //   이어서 length + bytes
                    // 서버 → 모두에게:
                    //   /profileBg user ext
                    //   이어서 length + bytes
                    else if (cmd.equals("/profileBg")) {
                        if (args.length < 2) continue;

                        String[] tmp = msg.split(" ", 3);
                        if (tmp.length < 2) continue;

                        String user = tmp[1].trim();
                        String ext = (tmp.length >= 3) ? tmp[2].trim() : "";

                        int length = dis.readInt();
                        if (length <= 0) continue;

                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);

                        String header = ext.isEmpty()
                                ? "/profileBg " + user
                                : "/profileBg " + user + " " + ext;

                        broadcastProfileBytes(header, bytes);
                    }

                    // ================== 행맨 게임 시작 ==================
                    // 클라 → 서버: /hangStart roomId
                    // 서버 → 방 멤버에게: /hangStart roomId wordIdx themeIdx
                    else if (cmd.equals("/hangStart")) {
                        if (args.length < 2) continue;
                        String roomId = args[1];

                        Random r = new Random();

                        // 클라이언트 HangmanPanel의 THEMES / WORDS_BY_THEME "개수"와만 맞추면 됨
                        // THEMES = { "My Friend Name", "Country", "Animal" }
                        // WORDS_BY_THEME = { {2개}, {3개}, {3개} }
                        int[] WORD_COUNTS = { 2, 3, 3 };

                        int themeIdx = r.nextInt(WORD_COUNTS.length);     // 0~2
                        int wordIdx  = r.nextInt(WORD_COUNTS[themeIdx]);  // 테마별 단어 개수에 맞게

                        String send = "/hangStart " + roomId + " " + wordIdx + " " + themeIdx;
                        sendRawToRoom(roomId, send);
                    }


                    // ================== 행맨 글자 추측 ==================
                    // 클라 → 서버: /hangGuess roomId letter
                    // 서버 → 방 멤버에게: 그대로 중계
                    else if (cmd.equals("/hangGuess")) {
                        String[] tmp = msg.split(" ", 3);
                        if (tmp.length < 3) continue;

                        String roomId = tmp[1];
                        String letter = tmp[2];

                        String send = "/hangGuess " + roomId + " " + letter;
                        sendRawToRoom(roomId, send);
                    }

                    // ================== 행맨 종료 ==================
                    // 클라 → 서버: /hangEnd roomId
                    // 서버 → 방 멤버에게: /hangEnd roomId
                    else if (cmd.equals("/hangEnd")) {
                        if (args.length < 2) continue;
                        String roomId = args[1];

                        String send = "/hangEnd " + roomId;
                        sendRawToRoom(roomId, send);
                    }

                } catch (EOFException eof) {
                    System.out.println("[Server] EOF (" + UserName + ") - disconnected");
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

        // -------------------- 방 멤버에게 텍스트 전송 --------------------
        // - roomId에 포함된 멤버에게만 /roomMsg roomId sendText 전송
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

        // -------------------- 방 멤버에게 원본 명령 그대로 전송 --------------------
        // - 이미 완성된 프로토콜 문자열을 그대로 중계한다
        // - 행맨 이벤트 같은 것에 사용한다
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

        // -------------------- 방 멤버에게 이미지 바이너리 전송 --------------------
        // - header: /roomImg roomId sender
        // - header 뒤에 length와 bytes를 붙여서 보낸다
        private void sendRoomImageToRoom(String roomId, String sender, byte[] imgBytes) {
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
                            user.logout();
                        }
                        break;
                    }
                }
            }
        }

        // -------------------- 프로필 이미지 바이너리 전체 브로드캐스트 --------------------
        // - header: /profileImg user ext 또는 /profileBg user ext
        // - header 뒤에 length와 bytes를 붙여서 보낸다
        private void broadcastProfileBytes(String header, byte[] bytes) {
            for (int i = 0; i < user_vc.size(); i++) {
                UserService user = user_vc.get(i);
                try {
                    user.writeBinaryPacket(header, bytes);
                } catch (IOException e) {
                    user.logout();
                }
            }
        }
    }
}
