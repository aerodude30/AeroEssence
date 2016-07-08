package aerodude30.com.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URI;

public class GUI extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    public JTextField username;
    public JTextField email;
    private JButton button;
    private JButton signUp;
    private JCheckBox rememberMe;

    public boolean optedOut = false;

    public GUI() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setButtonLink(button, signUp);
        try {
            BufferedReader in = new BufferedReader(new FileReader("user_creds.txt"));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equalsIgnoreCase("")) {
                    break;
                }
                String[] data = line.split(",");
                username.setText(data[0]);
                email.setText(data[1]);
                rememberMe.setSelected(true);
            }
        } catch (Exception e) {
            System.err.println("[Error] File could not be located (GUI 32)");
        }

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        button.addActionListener(e -> onQuestion());

        signUp.addActionListener(e -> onRegister());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void setButtonLink(JButton buttonStyleOne, JButton buttonStyleTwo) {
        buttonStyleOne.setText("<HTML>What is <FONT color=\"#000099\"><U>Aeroscripts?</U></FONT></HTML>");
        buttonStyleOne.setHorizontalAlignment(SwingConstants.LEFT);
        buttonStyleOne.setBorderPainted(false);
        buttonStyleOne.setOpaque(false);
        buttonStyleOne.setBackground(Color.WHITE);
        //Sign up button
        buttonStyleTwo.setText("<HTML>Register with <FONT color=\"#000099\"><U>Aeroscripts!</U></FONT></HTML>");
        buttonStyleTwo.setHorizontalAlignment(SwingConstants.LEFT);
        buttonStyleTwo.setBorderPainted(false);
        buttonStyleTwo.setOpaque(false);
        buttonStyleTwo.setBackground(Color.WHITE);
    }

    private void onQuestion() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://aeroscripts.x10host.com"));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Desktop is not supported cannot open hyperlink to: http://aeroscripts.x10host.com");
        }
    }

    private void onRegister() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://aeroscripts.x10host.com/User/register.php"));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Desktop is not supported cannot open hyperlink to: http://aeroscripts.x10host.com/User/register.php");
        }
    }

    private void onOK() {
        if (rememberMe.isSelected()) {
            try {
                PrintWriter writer = new PrintWriter("user_creds.txt", "UTF-8");
                writer.println(username.getText() + "," + email.getText());
                writer.close();
            } catch (Exception e) {
                System.err.println("[Error] Error writing user data to file...try again (PrintWriter 93)");
                e.printStackTrace();
            }
        } else {
            try {
                PrintWriter writer = new PrintWriter("user_creds.txt", "UTF-8");
                writer.println("");
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dispose();
    }

    private void onCancel() {
        System.err.println("[Error] User opted out of Aeroscripts feature");
        dispose();
        optedOut = true;
    }


    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
