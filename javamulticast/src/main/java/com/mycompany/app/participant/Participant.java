package com.mycompany.app.participant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Participant {

    private int participantId;
    private String messageLogFile;
    private String coordinatorIp;
    private int coordinatorPort; 

    public Participant(String configFile) throws IOException {
        parseConfigFile(configFile);
    }

    private void parseConfigFile(String configFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            this.participantId = Integer.parseInt(reader.readLine().trim());
            this.messageLogFile = reader.readLine().trim();
            String[] coordinatorInfo = reader.readLine().trim().split(" ");
            this.coordinatorIp = coordinatorInfo[0];
            this.coordinatorPort = Integer.parseInt(coordinatorInfo[1]);
        }
        System.out.println("Participant ID: " + participantId);
        System.out.println("Message Log File: " + messageLogFile);
        System.out.println("Coordinator IP: " + coordinatorIp);
        System.out.println("Coordinator Port: " + coordinatorPort);
    }

    /*
     * Time of last message
     * One thread to send a message
     * One thread to recieve data and write to conf
     */
    public static void main(String args[]) throws IOException {
        Participant p = new Participant(args[0]); 
        
        SocketChannel channel = null;

        channel = SocketChannel.open();

        InetSocketAddress server = new InetSocketAddress("localhost", 8080);
        channel.connect(server);

        // Wait until the connection is established
        while (!channel.finishConnect()) {
            // You could perform other tasks here while waiting for the connection to finish
            // For example, a small sleep to avoid busy-waiting
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Connected to server");

        String message = "Register 1 127.0.0.1 3000 ";
        ByteBuffer buf = ByteBuffer.wrap(message.getBytes());

        while (buf.hasRemaining()) {
            channel.write(buf);
        }

        buf.clear();
        int bytesRead = channel.read(buf);
        if (bytesRead > 0) {
            buf.flip();
            byte[] resp = new byte[buf.remaining()];
            buf.get(resp);
            System.out.println(new String(resp));
        }

        channel.close();

    }
    
}
