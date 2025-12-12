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

    private String tempProfileImagePath;
    private String tempBackgroundImagePath;

    private boolean editing = false;

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

        BackgroundPanel mainArea = new BackgroundPanel();
        mainArea.setOpaque(false);
        mainArea.setLayout(new BoxLayout(mainArea, BoxLayout.Y_AXIS));
        mainArea.setBorder(new EmptyBorder(60, 20, 20, 20));

        viewProfileImageLabel = new JLabel();
        viewProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon icon = null;
        if (profileData.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(profileData.getProfileImageIcon(), 90, 90);
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

        card.add(mainArea, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 0, 20, 20));

        JButton btnEdit = new JButton("Edit");
        btnEdit.setBackground(new Color(200, 200, 200));
        btnEdit.setPreferredSize(new Dimension(80, 32));
        btnEdit.setFocusPainted(false);
        btnEdit.addActionListener(e -> {
            editing = true;
            enterEditModeFromModel();
            cardLayout.show(cardPanel, CARD_EDIT);
            cardPanel.repaint();
        });
        bottom.add(btnEdit);

        card.add(bottom, BorderLayout.SOUTH);
        cardPanel.add(card, CARD_VIEW);
    }

    private void buildEditCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);

        BackgroundPanel mainArea = new BackgroundPanel();
        mainArea.setOpaque(false);
        mainArea.setLayout(new BoxLayout(mainArea, BoxLayout.Y_AXIS));
        mainArea.setBorder(new EmptyBorder(60, 20, 20, 20));

        mainArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;
                if (e.getY() <= HEADER_HEIGHT) chooseImageFile(false);
            }
        });

        editProfileImageLabel = new JLabel();
        editProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editProfileImageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editProfileImageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editing) return;
                chooseImageFile(true);
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

        card.add(mainArea, BorderLayout.CENTER);

        JPanel bottomEdit = new JPanel(new BorderLayout());
        bottomEdit.setBackground(Color.WHITE);
        bottomEdit.setBorder(new EmptyBorder(0, 0, 25, 25));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(80, 32));
        btnCancel.setBackground(new Color(210, 210, 210));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> {
            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        JButton btnSave = new JButton("Save");
        btnSave.setPreferredSize(new Dimension(80, 32));
        btnSave.setBackground(new Color(60, 179, 113));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            saveEditToModel();
            editing = false;
            if (onSavedCallback != null) onSavedCallback.run();
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
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

    private void enterEditModeFromModel() {
        tempProfileImagePath = profileData.getProfileImagePath();
        tempBackgroundImagePath = profileData.getBackgroundImagePath();

        txtName.setText(profileData.getName());
        txtStatus.setText(profileData.getStatusMessage());

        ImageIcon icon = loadImageIcon(tempProfileImagePath, 90, 90);
        if (icon != null) {
            editProfileImageLabel.setIcon(icon);
            editProfileImageLabel.setText("");
        } else {
            editProfileImageLabel.setIcon(null);
            editProfileImageLabel.setText("🙂");
            editProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }
    }

    private void saveEditToModel() {
        profileData.setName(txtName.getText().trim());
        profileData.setStatusMessage(txtStatus.getText().trim());
        profileData.setProfileImagePath(tempProfileImagePath);
        profileData.setBackgroundImagePath(tempBackgroundImagePath);

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

    private void chooseImageFile(boolean isProfile) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null && file.exists()) {
                if (isProfile) {
                    tempProfileImagePath = file.getAbsolutePath();
                    ImageIcon icon = loadImageIcon(tempProfileImagePath, 90, 90);
                    if (icon != null) {
                        editProfileImageLabel.setIcon(icon);
                        editProfileImageLabel.setText("");
                    }
                } else {
                    tempBackgroundImagePath = file.getAbsolutePath();
                    cardPanel.repaint();
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

    private class BackgroundPanel extends JPanel {
        private String lastBgPath;
        private Image bgImage;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            ImageIcon icon = null;
            if (!editing && profileData.getBackgroundImageIcon() != null) {
                icon = profileData.getBackgroundImageIcon();
            }

            String bgPath;
            if (editing && tempBackgroundImagePath != null && !tempBackgroundImagePath.isEmpty()) bgPath = tempBackgroundImagePath;
            else bgPath = profileData.getBackgroundImagePath();

            Graphics2D g2 = (Graphics2D) g;

            if (icon != null && icon.getImage() != null) {
                g2.drawImage(icon.getImage(), 0, 0, w, HEADER_HEIGHT, this);
            } else {
                if (bgPath == null || bgPath.isEmpty()) {
                    bgImage = null;
                    lastBgPath = null;
                } else if (!bgPath.equals(lastBgPath)) {
                    lastBgPath = bgPath;
                    bgImage = loadBackgroundImage(bgPath);
                }

                if (bgImage != null) g2.drawImage(bgImage, 0, 0, w, HEADER_HEIGHT, this);
                else {
                    g2.setColor(new Color(220, 220, 220));
                    g2.fillRect(0, 0, w, HEADER_HEIGHT);
                }
            }

            g2.setColor(Color.WHITE);
            g2.fillRect(0, HEADER_HEIGHT, w, h - HEADER_HEIGHT);
        }

        private Image loadBackgroundImage(String path) {
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
