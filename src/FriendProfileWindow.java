// FriendProfileWindow.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

public class FriendProfileWindow extends JDialog {

    private static final String CARD_VIEW = "CARD_VIEW";
    private static final String CARD_EDIT = "CARD_EDIT";
    private static final int HEADER_HEIGHT = 150;

    private final ProfileData profileData;
    private final String realName;
    private String displayName;

    private final Consumer<String> onNameSaved;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private JLabel viewProfileImageLabel;
    private JLabel viewDisplayNameLabel;
    private JLabel viewStatusLabel;

    private JLabel editProfileImageLabel;
    private JTextField txtDisplayName;
    private JLabel editStatusLabel;

    private boolean editing = false;

    public FriendProfileWindow(Frame owner,
                               ProfileData profileData,
                               String realName,
                               String displayName,
                               Consumer<String> onNameSaved) {
        super(owner, "Friend Profile", true);
        this.profileData = profileData;
        this.realName = realName;
        this.displayName = (displayName == null || displayName.isEmpty()) ? realName : displayName;
        this.onNameSaved = onNameSaved;

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
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

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

        center.add(Box.createVerticalStrut(10));
        center.add(viewProfileImageLabel);
        center.add(Box.createVerticalStrut(18));

        viewDisplayNameLabel = new JLabel(displayName, SwingConstants.CENTER);
        viewDisplayNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewDisplayNameLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        center.add(viewDisplayNameLabel);
        center.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));
        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        center.add(sepWrapper);
        center.add(Box.createVerticalStrut(14));

        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        viewStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        viewStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        center.add(Box.createVerticalStrut(4));
        center.add(viewStatusLabel);
        center.add(Box.createVerticalStrut(30));

        card.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 0, 20, 20));

        JButton btnEdit = new JButton("Edit");
        btnEdit.setBackground(new Color(200, 200, 200));
        btnEdit.setPreferredSize(new Dimension(80, 32));
        btnEdit.setFocusPainted(false);
        btnEdit.addActionListener(e -> {
            editing = true;
            enterEditMode();
            cardLayout.show(cardPanel, CARD_EDIT);
            cardPanel.repaint();
        });

        bottom.add(btnEdit);
        card.add(bottom, BorderLayout.SOUTH);

        viewDisplayNameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editing = true;
                    enterEditMode();
                    cardLayout.show(cardPanel, CARD_EDIT);
                    cardPanel.repaint();
                }
            }
        });

        cardPanel.add(card, CARD_VIEW);
    }

    private void buildEditCard() {
        JPanel card = new BackgroundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 20, 20, 20));

        editProfileImageLabel = new JLabel();
        editProfileImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon icon = null;
        if (profileData.getProfileImageIcon() != null) {
            icon = ProfileData.scaleIcon(profileData.getProfileImageIcon(), 90, 90);
        } else {
            icon = loadImageIcon(profileData.getProfileImagePath(), 90, 90);
        }

        if (icon != null) editProfileImageLabel.setIcon(icon);
        else {
            editProfileImageLabel.setText("🙂");
            editProfileImageLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
        }

        center.add(Box.createVerticalStrut(10));
        center.add(editProfileImageLabel);
        center.add(Box.createVerticalStrut(18));

        txtDisplayName = new JTextField(displayName);
        txtDisplayName.setHorizontalAlignment(JTextField.CENTER);
        txtDisplayName.setBorder(null);
        txtDisplayName.setFont(new Font("Dialog", Font.BOLD, 18));
        txtDisplayName.setMaximumSize(new Dimension(180, 28));
        txtDisplayName.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(txtDisplayName);
        center.add(Box.createVerticalStrut(10));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));
        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(new EmptyBorder(10, 40, 0, 40));
        sepWrapper.add(sep, BorderLayout.CENTER);
        center.add(sepWrapper);
        center.add(Box.createVerticalStrut(14));

        String status = profileData.getStatusMessage();
        if (status == null || status.isEmpty()) status = "One line Introduction";
        editStatusLabel = new JLabel("“ " + status + " ”", SwingConstants.CENTER);
        editStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editStatusLabel.setFont(new Font("Dialog", Font.PLAIN, 13));

        center.add(Box.createVerticalStrut(4));
        center.add(editStatusLabel);
        center.add(Box.createVerticalStrut(30));

        card.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(0, 0, 25, 25));

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
            String newName = txtDisplayName.getText().trim();
            if (newName.isEmpty()) newName = realName;
            displayName = newName;

            if (onNameSaved != null) onNameSaved.accept(displayName);

            viewDisplayNameLabel.setText(displayName);

            editing = false;
            cardLayout.show(cardPanel, CARD_VIEW);
            cardPanel.repaint();
        });

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        leftWrap.setBackground(Color.WHITE);
        leftWrap.add(btnCancel);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setBackground(Color.WHITE);
        rightWrap.add(btnSave);

        bottom.add(leftWrap, BorderLayout.WEST);
        bottom.add(rightWrap, BorderLayout.EAST);

        card.add(bottom, BorderLayout.SOUTH);

        cardPanel.add(card, CARD_EDIT);
    }

    private void enterEditMode() {
        if (txtDisplayName != null) txtDisplayName.setText(displayName);
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

            ImageIcon icon = profileData.getBackgroundImageIcon();
            Graphics2D g2 = (Graphics2D) g;

            if (icon != null && icon.getImage() != null) {
                g2.drawImage(icon.getImage(), 0, 0, w, HEADER_HEIGHT, this);
            } else {
                String bgPath = profileData.getBackgroundImagePath();

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