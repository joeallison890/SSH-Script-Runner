import com.jcraft.jsch.JSchException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class Application {

    final static String filePath = System.getenv("HOME") + "/.sshAppInfo";
    static String username = "";
    static String password = "";
    static String host = "";
    static String directory = "";
    boolean showCommandOutput = false;

    DefaultListModel<String> model;
    JTextArea outputArea;

    SSHConnection connection = new SSHConnection();

    public Application() {
        getInfoFromFile(); // read user information from the file before initialising GUI
        init();
    }

    public static void getInfoFromFile() {
        File file = new File(filePath);
        if (!file.exists()) { // first time use of application or file has been deleted
            return;
        }
        try {
            ArrayList<String> info = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                info.add("");
            }
            Scanner scanner = new Scanner(file);
            int i = 0;
            while (scanner.hasNextLine()) {
                info.set(i, scanner.nextLine());
                i++;
            }
            scanner.close(); // file structured as username 1st line, host 2nd, directory 3rd
            username = info.get(0);
            host = info.get(1);
            directory = info.get(2);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveInfoToFile() {
        try {
            FileWriter writer = new FileWriter(filePath); // file saved in user's home as a hidden text file
            writer.write(username + '\n');
            writer.write(host + '\n');
            writer.write(directory + '\n');
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() {
        //// frame
        JFrame mainFrame = new JFrame();
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setTitle("SSH Script Runner"); // title of application
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveInfoToFile(); // save user information to the file before finishing process
            }
        });
        mainFrame.setSize(500, 600);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setResizable(false); // make window non-resizeable

        //// container panel
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));

        //// panel for showing available scripts
        JPanel scriptPanel = new JPanel();
        scriptPanel.setLayout(new BoxLayout(scriptPanel, BoxLayout.Y_AXIS));
        // initialisation of elements
        model = new DefaultListModel<>();
        JList<String> scriptList = new JList<>(model);

        outputArea = new JTextArea();

        JScrollPane scriptScrollPane = new JScrollPane(scriptList);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);

        // placing and settings
        scriptScrollPane.setMinimumSize(new Dimension(40, 200));
        outputScrollPane.setMinimumSize(new Dimension(40, 200));
        outputArea.setEditable(false); // make output area for displaying only
        scriptPanel.add(scriptScrollPane);
        scriptPanel.add(outputScrollPane);

        ////  panel for buttons at bottom
        JPanel bottomButtons = new JPanel();
        JButton execute = new JButton("Execute");
        JButton showCode = new JButton("Show Code");

        bottomButtons.add(execute);
        bottomButtons.add(showCode);

        // listeners for bottom buttons
        execute.addActionListener(actionEvent -> {
            if (scriptList.getSelectedIndex() > 0) {
                executeButton(model.getElementAt(scriptList.getSelectedIndex())); // get selected script name
            }
        });
        showCode.addActionListener(actionEvent -> {
            if (scriptList.getSelectedIndex() > 0) {
                showCodeButton(model.getElementAt(scriptList.getSelectedIndex())); // get selected script name
            }
        });

        //// panel for all top buttons
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

        // initialisations of elements
        JButton connect = new JButton("Connect");
        JButton refresh = new JButton("Refresh");
        JButton settings = new JButton("Settings");
        JPanel topThreeButtons = new JPanel(); // panel for only top 3 buttons
        JButton openTerminal = new JButton("Open Remote Terminal");
        JRadioButton showOutput = new JRadioButton("Show Command Output");

        // listeners
        connect.addActionListener(actionEvent -> connectButton());
        refresh.addActionListener(actionEvent -> refreshButton());
        settings.addActionListener(actionEvent -> options());
        openTerminal.addActionListener(actionEvent -> openTerminal());
        showOutput.addActionListener(actionEvent -> showCommandOutput = !showCommandOutput); // toggle showCommandOutput when button is pressed

        // placing
        topThreeButtons.add(connect);
        topThreeButtons.add(refresh);
        topThreeButtons.add(settings);
        topThreeButtons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(topThreeButtons);
        openTerminal.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(openTerminal);
        showOutput.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(showOutput);

        buttonsPanel.setMaximumSize(new Dimension(400, 100));
        containerPanel.add(buttonsPanel);
        containerPanel.add(scriptPanel);
        bottomButtons.setMaximumSize(new Dimension(400, 100));
        containerPanel.add(bottomButtons);

        mainFrame.add(containerPanel, BorderLayout.CENTER);
        mainFrame.setVisible(true);
    }

    private void options() {
        // settings for the frame
        JFrame settingPage = new JFrame("SSH Script Runner Config");
        settingPage.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        settingPage.setSize(400, 200);
        settingPage.setLocationRelativeTo(null);
        settingPage.setResizable(false);

        // container panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // panel for the text boxes
        JPanel textBoxes = new JPanel();
        textBoxes.setBorder(new EmptyBorder(10, 10, 10, 10));
        textBoxes.setLayout(new GridLayout(3, 2, 2, 10));

        // text boxes within the text boxes panel
        JTextField hostField = new JTextField(host, 30);
        JTextField usernameField = new JTextField(username, 30);
        JTextField directoryField = new JTextField(directory, 30);

        // labels for the text boxes
        textBoxes.add(new JLabel("host"));
        textBoxes.add(hostField);
        textBoxes.add(new JLabel("username"));
        textBoxes.add(usernameField);
        textBoxes.add(new JLabel("script directory"));
        textBoxes.add(directoryField);

        // panel and button for updating info inputted within text boxes
        JPanel buttons = new JPanel();
        JButton enter = new JButton("Update");
        // listener to grab info from text boxes upon press
        enter.addActionListener(actionEvent -> {
            host = hostField.getText();
            username = usernameField.getText();
            directory = directoryField.getText();
            settingPage.dispose();
        });

        // button to reset info stored, add buttons to panel
        JButton reset = new JButton("Reset Information");
        reset.addActionListener(actionEvent -> {
            host = "";
            username = "";
            password = "";
            directory = "";
            hostField.setText("");
            usernameField.setText("");
            directoryField.setText("");
        });

        buttons.add(enter);
        buttons.add(reset);

        // adding panels tp main panel, then added to frame
        mainPanel.add(textBoxes);
        mainPanel.add(buttons);
        settingPage.add(mainPanel);
        settingPage.setVisible(true);
    }

    private void getPassword() {
        // clear password
        password = "";

        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter password:");
        JPasswordField pass = new JPasswordField(15);
        panel.add(label);
        panel.add(pass);
        String[] options = new String[]{"Cancel", "OK"};
        // dialogue box to get password, with OK and Cancel buttons
        int option = JOptionPane.showOptionDialog(null, panel, "password for " + username + "@" + host, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (option == 1) // OK
        {
            char[] input = pass.getPassword();
            if (input.length == 0) {
                // validate user input is not empty
                JOptionPane.showMessageDialog(null, "Password cannot be empty", "Warning", JOptionPane.WARNING_MESSAGE);
                getPassword();
            } else {
                // input valid, set as new password
                password = new String(input);
            }
        }
    }

    private int getInformation() {
        // check user information has been set (either via option pane or read from the user's file
        if (username.equals("") || host.equals("")) {
            JOptionPane.showMessageDialog(null, "Please insert information in the settings page before connecting", "Warning", JOptionPane.WARNING_MESSAGE);
            return 0;
        }

        // pop-up to get password from user
        getPassword();
        if (password.equals("")) { // user pressed cancel, do not update password
            return 0;
        }
        return 1; // information successfully retrieved
    }

    private void openTerminal() {
        // validate login information then get password
        if (getInformation() == 0) {
            return;
        }
        // launch a new terminal session
        new Terminal(username, password, host);
    }

    private void connectButton() {
        // validate login information then get password
        if (getInformation() == 0) {
            return;
        }
        // if directory is empty, revert to default ~/scripts
        if (directory.equals("")) {
            directory = "/home" + username + "/scripts";
            JOptionPane.showMessageDialog(null, "Script Directory not set. Defaulting to /home/" + username + "/scripts", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        // try to connect, if fail show error message on the output box
        try {
            connection.openConnection();
        } catch (JSchException e) {
            outputArea.setText(e.toString());
            return;
        }
        updateScriptList();
    }

    private void updateScriptList() {
        // get scripts, if no scripts found print error message
        ArrayList<String> scripts = connection.getScripts();
        if (scripts.isEmpty()) {
            model.addElement("No scripts found in directory");
            return;
        }
        // clear previous entries
        model.clear();
        for (String script : scripts) {
            model.addElement(script);
        }
    }

    private void refreshButton() {
        // check if a connection has been established via connect button
        if (connection.connected()) {
            updateScriptList();
            return;
        }
        JOptionPane.showMessageDialog(null, "Not connected to any server", "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void executeButton(String command) {
        AtomicReference<String> output = new AtomicReference<>("");
        Thread thread = new Thread(() -> output.set(connection.issueCommand("bash " + directory + "/" + command))); // send command in a new thread
        thread.start();
        if (!showCommandOutput) {
            // show command output is false so do not join thread to wait for output
            outputArea.setText(""); // a new command has been executed so clear the text area
            return;
        }
        try {
            thread.join(10000); // if the show output option is true then join thread and wait 10s
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (output.get().isEmpty()) { // waited 10s and thread still has not finished, return message saying output will not be waited on
            outputArea.setText("No output or process is still running");
        }
        outputArea.setText(output.get()); // waited 10s and thread has finished, can return actual output
    }

    private void showCodeButton(String script) {
        String command = "cat " + directory + "/" + script; // bash command to print contents of selected script
        // set output of command to the output area
        outputArea.setText(connection.issueCommand(command));
    }
}
