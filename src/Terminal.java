import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

public class Terminal {

    JTextAreaOutputStream outputStream;
    PrintStream print;

    public Terminal(String username, String password, String host) {
        init(); // draw interface
        launchTerminalSession(username, password, host); // attempt to connect to server
    }

    public void init() {
        // settings for the frame
        JFrame terminalPage = new JFrame("Terminal");
        terminalPage.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        terminalPage.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //disconnect from ssh server
                if (print != null) {
                    print.println("exit");
                }
            }
        });
        terminalPage.setLocationRelativeTo(null);
        terminalPage.setResizable(false);

        // text area for output
        JTextArea outputFeed = new JTextArea(40, 80);
        outputFeed.setEditable(false);

        // container for terminal
        Container contentPane = terminalPage.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(new JScrollPane(outputFeed, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        terminalPage.pack();

        // set up passing through system.out
        outputStream = new JTextAreaOutputStream(outputFeed);

        // input panel with input box
        JPanel inputPanel = new JPanel();
        JTextField inputField = new JTextField(70);
        inputPanel.add(inputField);

        // passing from text field to the print stream
        inputField.addActionListener(actionEvent -> {
            String input = inputField.getText();
            // close window if user has ended connection
            if (input.equals("exit")) {
                print.println(input);
                terminalPage.dispose();
            }
            // manually handle clear command
            else if (input.equals("clear")) {
                outputFeed.setText("");
            } else {
                print.println(input);
            }
            // clear input box
            inputField.setText("");
        });

        // render frame
        contentPane.add(inputPanel);
        terminalPage.setVisible(true);
    }

    public void launchTerminalSession(String username, String password, String host) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no"); // SSH tends to send ECDSA fingerprints, JSch likes SHA_RSA. To save the user having to convert their known_hosts file key checking is disabled

            // try to connect to the server, if connection fails print error message on screen
            try {
                session.connect(30000); // connection timeout of 30 seconds
            } catch (Exception e) {
                outputStream.write(e.toString().getBytes());
                return;
            }
            Channel channel = session.openChannel("shell");

            // set up inputStream
            PipedInputStream pipedInputStream = new PipedInputStream(40);
            channel.setInputStream(pipedInputStream);

            PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
            print = new PrintStream(pipedOutputStream);

            // set the outputStream
            channel.setOutputStream(outputStream);

            // connect to server
            channel.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // class for taking the output stream of the JSch shell and displaying on a swing text area
    public static class JTextAreaOutputStream extends OutputStream {
        private final JTextArea destination;

        public JTextAreaOutputStream(JTextArea destination) {
            if (destination == null) {
                throw new IllegalArgumentException("Destination is null");
            }
            this.destination = destination;
        }

        @Override
        public void write(byte[] buffer, int offset, int length) {
            final String text = new String(buffer, offset, length);
            SwingUtilities.invokeLater(() -> destination.append(text));
        }

        @Override
        public void write(int b) {
            write(new byte[]{(byte) b}, 0, 1);
        }

    }
}