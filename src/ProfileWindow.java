// ProfileWindow.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;

public class ProfileWindow extends JDialog {

    private static final String CARD_VIEW = "CARD_VIEW";
    private static final String CARD_EDIT = "CARD_EDIT";
    private static final int HEADER_HEIGHT = 150;

    private final ProfileData profileData;
    private final Runnable onSavedCallback;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private JLabel viewProfileImageLabel;
    private JLabel viewNameLabel;
    private JLabel viewStatusLabel;

    private JLabel editProfileImageLabel;
    private JTextField txtName;
    private JTextField txtStatus;

    private String tempProfileImagePath;      // 수정함
    private String tempBackgroundImagePath;   // 수정함

    private boolean editing = false;          // 수정함

    public ProfileWindow(Frame owner, ProfileData profileData, Runnable onSavedCallback) {
        super(owner, "My Profile", true);
        this.profileData = profileData;
        this.onSavedCallback = onSavedCallback;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(owner);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cardPanel, BorderLayout.CENTER);

        buildViewCard();
        buildEditCard();

        cardLayout.show(cardPanel, CARD_VIEW);
    }

    private void buildViewCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        BackgroundPanel mainArea = new BackgroundPanel(); // 수정함
        mainArea.setOpaque(false);                        // 수정함
        mainArea.setLayout(new BoxLayout(mainArea, BoxLayout.Y_AXIS)); // 수정함
        mainArea.setBorder(new EmptyBorder(60, 20, 20, 20));           // 수정함

        viewProfileImageLabel = new JLabel();
        viewProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon icon = null;
        if (profileData.getProfileImageIcon() != null) { // 수정함
            icon = ProfileData.scaleIcon(profileData.getProfileImageIcon(), 90, 90); // 수정함
        } else {
            icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        }

        if (icon != null) viewProfileImageLabel.setIcon(icon);
        else {
            viewProfileImageLabel.setText("🙂");
            viewProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }

        mainArea.add(Box.createVerticalStrut(10));
        mainArea.add(viewProfileImageLabel);
        mainArea.add(Box.createVerticalStrut(18));

        viewNameLabel = new JLabel(profileData.getName(), SwingConstants.CENTER);
        viewNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewNameLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        mainArea.add(viewNameLabel);
        mainArea.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));

        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        mainArea.add(sepWrapper);
        mainArea.add(Box.createVerticalStrut(14));

        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        viewStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        mainArea.add(Box.createVerticalStrut(4));
        mainArea.add(viewStatusLabel);
        mainArea.add(Box.createVerticalStrut(30));

        card.add(mainArea, BorderLayout.CENTER); // 수정함

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 0, 20, 20));

        JButton btnEdit = new JButton("Edit");
        btnEdit.setBackground(new Color(200, 200, 200));
        btnEdit.setPreferredSize(new Dimension(80, 32));
        btnEdit.setFocusPainted(false);
        btnEdit.addActionListener(e -> { // 수정함
            editing = true;              // 수정함
            enterEditModeFromModel();    // 수정함
            cardLayout.show(cardPanel, CARD_EDIT); // 수정함
            cardPanel.repaint();         // 수정함
        });
        bottom.add(btnEdit);

        card.add(bottom, BorderLayout.SOUTH);
        cardPanel.add(card, CARD_VIEW);
    }

    private void buildEditCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        BackgroundPanel mainArea = new BackgroundPanel(); // 수정함
        mainArea.setOpaque(false);                        // 수정함
        mainArea.setLayout(new BoxLayout(mainArea, BoxLayout.Y_AXIS)); // 수정함
        mainArea.setBorder(new EmptyBorder(60, 20, 20, 20));           // 수정함

        mainArea.addMouseListener(new MouseAdapter() { // 수정함
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;                 // 수정함
                if (e.getY() <= HEADER_HEIGHT) chooseImageFile(false); // 수정함
            }
        });

        editProfileImageLabel = new JLabel();
        editProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editProfileImageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editProfileImageLabel.addMouseListener(new MouseAdapter() { // 수정함
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;                 // 수정함
                chooseImageFile(true);                // 수정함
            }
        });

        mainArea.add(Box.createVerticalStrut(10));
        mainArea.add(editProfileImageLabel);
        mainArea.add(Box.createVerticalStrut(18));

        txtName = new JTextField();
        txtName.setHorizontalAlignment(JTextField.CENTER);
        txtName.setBorder(null);
        txtName.setFont(new Font("Dialog", Font.BOLD, 18));
        txtName.setMaximumSize(new Dimension(180, 28));
        txtName.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainArea.add(txtName);
        mainArea.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));

        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        mainArea.add(sepWrapper);
        mainArea.add(Box.createVerticalStrut(14));

        txtStatus = new JTextField();
        txtStatus.setHorizontalAlignment(JTextField.CENTER);
        txtStatus.setBorder(null);
        txtStatus.setFont(new Font("Dialog", Font.PLAIN, 13));
        txtStatus.setMaximumSize(new Dimension(220, 28));
        txtStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainArea.add(Box.createVerticalStrut(4));
        mainArea.add(txtStatus);
        mainArea.add(Box.createVerticalStrut(30));
        mainArea.add(Box.createVerticalGlue());

        card.add(mainArea, BorderLayout.CENTER); // 수정함

        JPanel bottomEdit = new JPanel(new BorderLayout());
        bottomEdit.setBackground(Color.WHITE);
        bottomEdit.setBorder(new EmptyBorder(0, 0, 25, 25));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.setBackground(new Color(210, 210, 210));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> { // 수정함
            editing = false;               // 수정함
            cardLayout.show(cardPanel, CARD_VIEW); // 수정함
            cardPanel.repaint();           // 수정함
        });

        JButton btnSave = new JButton("Save");
        btnSave.setPreferredSize(new Dimension(80, 32));
        btnSave.setBackground(new Color(60, 179, 113));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> { // 수정함
            saveEditToModel();            // 수정함
            editing = false;              // 수정함
            if (onSavedCallback != null) onSavedCallback.run(); // 수정함
            cardLayout.show(cardPanel, CARD_VIEW); // 수정함
            cardPanel.repaint();          // 수정함
        });

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        leftWrap.setBackground(Color.WHITE);
        leftWrap.add(btnCancel);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setBackground(Color.WHITE);
        rightWrap.add(btnSave);

        bottomEdit.add(leftWrap, BorderLayout.WEST);
        bottomEdit.add(rightWrap, BorderLayout.EAST);

        card.add(bottomEdit, BorderLayout.SOUTH);
        cardPanel.add(card, CARD_EDIT);
    }

    private void enterEditModeFromModel() { // 수정함
        tempProfileImagePath = profileData.getProfileImagePath();     // 수정함
        tempBackgroundImagePath = profileData.getBackgroundImagePath(); // 수정함

        txtName.setText(profileData.getName());
        txtStatus.setText(profileData.getStatusMessage());

        ImageIcon icon = loadImageIcon(tempProfileImagePath, 90, 90); // 수정함
        if (icon != null) {                                           // 수정함
            editProfileImageLabel.setIcon(icon);                       // 수정함
            editProfileImageLabel.setText("");                         // 수정함
        } else {                                                       // 수정함
            editProfileImageLabel.setIcon(null);                       // 수정함
            editProfileImageLabel.setText("🙂");                       // 수정함
            editProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32)); // 수정함
        }
    }

    private void saveEditToModel() { // 수정함
        profileData.setName(txtName.getText().trim());                 // 수정함
        profileData.setStatusMessage(txtStatus.getText().trim());      // 수정함
        profileData.setProfileImagePath(tempProfileImagePath);         // 수정함
        profileData.setBackgroundImagePath(tempBackgroundImagePath);   // 수정함

        viewNameLabel.setText(profileData.getName());

        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel.setText("“ " + status + " ”");

        ImageIcon icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        if (icon != null) {
            viewProfileImageLabel.setIcon(icon);
            viewProfileImageLabel.setText("");
        }

        cardPanel.repaint();
    }

    private void chooseImageFile(boolean isProfile) { // 수정함
        JFileChooser chooser = new JFileChooser();    // 수정함
        int result = chooser.showOpenDialog(this);    // 수정함
        if (result == JFileChooser.APPROVE_OPTION) {  // 수정함
            File file = chooser.getSelectedFile();    // 수정함
            if (file != null && file.exists()) {      // 수정함
                if (isProfile) {                      // 수정함
                    tempProfileImagePath = file.getAbsolutePath(); // 수정함
                    ImageIcon icon = loadImageIcon(tempProfileImagePath, 90, 90); // 수정함
                    if (icon != null) {               // 수정함
                        editProfileImageLabel.setIcon(icon); // 수정함
                        editProfileImageLabel.setText("");   // 수정함
                    }
                } else {                               // 수정함
                    tempBackgroundImagePath = file.getAbsolutePath(); // 수정함
                    cardPanel.repaint();               // 수정함
                }
            }
        }
    }

    private ImageIcon loadImageIcon(String path, int w, int h) {
        if (path == null || path.isEmpty()) return null;
        Image raw = null;
        try {
            if (path.startsWith("/")) {
                URL url = getClass().getResource(path);
                if (url != null) raw = new ImageIcon(url).getImage();
            } else {
                File f = new File(path);
                if (f.exists()) raw = new ImageIcon(path).getImage();
            }
            if (raw == null) return null;
            Image scaled = raw.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    private class BackgroundPanel extends JPanel { // 수정함
        private String lastBgPath;                  // 수정함
        private Image bgImage;                      // 수정함

        @Override
        protected void paintComponent(Graphics g) { // 수정함
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            ImageIcon icon = null; // 수정함
            if (!editing && profileData.getBackgroundImageIcon() != null) { // 수정함
                icon = profileData.getBackgroundImageIcon();                // 수정함
            }

            String bgPath; // 수정함
            if (editing && tempBackgroundImagePath != null && !tempBackgroundImagePath.isEmpty()) bgPath = tempBackgroundImagePath; // 수정함
            else bgPath = profileData.getBackgroundImagePath(); // 수정함

            Graphics2D g2 = (Graphics2D) g;

            if (icon != null && icon.getImage() != null) { // 수정함
                g2.drawImage(icon.getImage(), 0, 0, w, HEADER_HEIGHT, this); // 수정함
            } else { // 수정함
                if (bgPath == null || bgPath.isEmpty()) { // 수정함
                    bgImage = null;                        // 수정함
                    lastBgPath = null;                     // 수정함
                } else if (!bgPath.equals(lastBgPath)) {   // 수정함
                    lastBgPath = bgPath;                   // 수정함
                    bgImage = loadBackgroundImage(bgPath); // 수정함
                }

                if (bgImage != null) g2.drawImage(bgImage, 0, 0, w, HEADER_HEIGHT, this); // 수정함
                else {
                    g2.setColor(new Color(220, 220, 220));
                    g2.fillRect(0, 0, w, HEADER_HEIGHT);
                }
            }

            g2.setColor(Color.WHITE);
            g2.fillRect(0, HEADER_HEIGHT, w, h - HEADER_HEIGHT);
        }

        private Image loadBackgroundImage(String path) { // 수정함
            try {
                Image raw = null;
                if (path.startsWith("/")) {
                    URL url = getClass().getResource(path);
                    if (url != null) raw = new ImageIcon(url).getImage();
                } else {
                    File f = new File(path);
                    if (f.exists()) raw = new ImageIcon(path).getImage();
                }
                return raw;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
