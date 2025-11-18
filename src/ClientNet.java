import java.io.*;
import java.net.Socket;

public class ClientNet {
    Socket socket;
    private InputStream is;
    private OutputStream os;
    private DataInputStream dis;
    private DataOutputStream dos;

    public ClientNet(String username, String ip, String port) {
        try{
            socket = new Socket(username, Integer.parseInt(port));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF("/login " + username);

        }catch(IOException e){
            throw new RuntimeException("연결 실패", e);
        }

    }
}
