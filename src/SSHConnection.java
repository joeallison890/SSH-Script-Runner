import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class SSHConnection {

    Session session;
    ChannelExec channel;

    public boolean connected() {
        return session != null; // if session is null then no connection has been established yet
    }

    public void openConnection() throws JSchException {
        JSch jsch = new JSch();

        session = jsch.getSession(Application.username, Application.host, 22);
        session.setConfig("StrictHostKeyChecking", "no"); // SSH tends to send ECDSA fingerprints, JSch likes SHA_RSA. To save the user having to convert their known_hosts file key checking is disabled
        session.setPassword(Application.password);

        session.connect(30000); // try to connect, 30s timeout
    }

    public String issueCommand(String command) {
        String output;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command); // set command to be executed
            channel.setInputStream(null); // input stream not needed for exec type channel

            InputStream in = channel.getInputStream();

            channel.connect();
            output = getChannelOutput(channel, in); // get output from exec channel
            channel.disconnect(); // disconnect from channel
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // get output from the JSch channel
    private String getChannelOutput(Channel channel, InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder strBuilder = new StringBuilder();

        String line = "";
        while (true) {
            while (in.available() > 0) {
                int i = in.read(buffer, 0, 1024);
                if (i < 0) {
                    break;
                }
                strBuilder.append(new String(buffer, 0, i));
            }

            if (line.contains("logout")) {
                break;
            }
            if (channel.isClosed()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return strBuilder.toString();
    }

    public ArrayList<String> getScripts() {
        String output = "";
        try {
            // bash command ls to get list of files in directory, filter to only show .sh files
            output = issueCommand("ls " + Application.directory + " | grep .sh");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (output == null || output.isEmpty()) {
            // command failed or the directory does not exist/cannot be accessed, return error message
            ArrayList<String> noDir = new ArrayList<>();
            noDir.add("Cannot access directory");
            return noDir;
        }
        return new ArrayList<>(Arrays.asList(Objects.requireNonNull(output).split("\n")));
    }
}
