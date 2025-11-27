import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;

public class ChatRoom extends JFrame{
    private String roomId;
    private ClientNet clientNet;
    private List<String> selectedusers;

    //이건 .GUI용
    private JTextArea textArea;
    private JTextField txtInput;
    private JButton btnSend;

    public ChatRoom(String roomId, ClientNet clientNet){
        // 채팅방 생성 체크박스에서 체크된것들 가져옴
        this.roomId = roomId;
        this.clientNet = clientNet;

        // 이제 GUI 올리기 -------------------
        setTitle("채팅방 - " + roomId);
        setSize(400, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        txtInput = new JTextField();
        btnSend = new JButton("전송");
        bottom.add(txtInput, BorderLayout.CENTER);
        bottom.add(btnSend, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        txtInput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // 창 닫으면 그냥 숨기기만 하기 -> 데이터 보존 위해
        setVisible(true);
    }
    private void sendMessage() {
        String msg = txtInput.getText().trim();
        //내가 친 걸 담아서
        if (msg.isEmpty())
            return;

        // 각 멤버별 채팅방을 위한 메세지 보내기
        clientNet.SendMessage("/roomMsg " + roomId + " " + msg);
        txtInput.setText("");
    }
    public void appendMessage(String msg) {
        textArea.append(msg + "\n");
    }
    public String getRoomId() {
        return roomId;
    }
}
