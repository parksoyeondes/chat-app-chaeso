import java.io.*;
import java.net.Socket;

public class ClientNet {
    Socket socket;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;


    //ChatHomeFrame에서 인자로 넘긴거 생성자로 받기
    public ClientNet(String username, String ip, String port) {
        try{
            socket = new Socket(username, Integer.parseInt(port));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            //서버에게 방금 로그인 한 유저이름 전달
            SendMessage("/login " + username); // => 서버에게
            // 수신 스레드 만들기
            ListenNetwork net = new ListenNetwork();

        }catch(IOException e){
            throw new RuntimeException("연결 실패", e);
        }
    }
    // run()을 계속 돌면서 항상 서버에서 오는 메세지 수신 받을 준비
    class ListenNetwork extends Thread {
        public void run() {
            while (true) {
                try {
                    // Use readUTF to read messages
                    String msg = dis.readUTF();
                } catch (IOException e) {

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
            log("dos.write() error");
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


