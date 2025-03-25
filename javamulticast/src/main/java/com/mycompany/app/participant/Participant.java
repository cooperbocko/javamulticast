package com.mycompany.app.participant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Participant {
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private LocalDateTime timeOfLastMessage;
    private static Scanner input = new Scanner(System.in);
    private static SocketChannel channel;
    private static boolean isConnected = false;
    private static int id;
    private static String messageLogFile;
    private static String host;
    private static int port;
    
    public static void main(String args[]) throws IOException {
        /*  SocketChannel channel = null;

        channel = SocketChannel.open();
        InetSocketAddress server = new InetSocketAddress("localhost", 8080);
        channel.connect(server);
        while (!channel.finishConnect()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Connected to server"); 

        String message = "";
        ByteBuffer buf = ByteBuffer.allocate(MAX_MESSAGE_LENGTH);
        int bytesRead;
        while (message != "quit") {
            message = input.nextLine();
            buf = ByteBuffer.wrap(message.getBytes());
            while (buf.hasRemaining()) {
                channel.write(buf);
            }
            buf.clear();
            readResonse(channel);
        }
        channel.close(); */
        // System.out.println("1");

        initializeParticipant(args);

        connectToServer();
        Thread userInputThread = new Thread(Participant::handleUserCommands);
        userInputThread.start();

        // Thread messageReceiverThread = new Thread(Participant::receiveMulticastMessages);
        // messageReceiverThread.start();
    }

    /**
     * Reads participant configuration file and initialize variables 
     * 
     * @param args Command-line arguments
     */
    public static void initializeParticipant (String[] args) {
        if (args.length < 1) {
            System.err.println("Requires <config-file>");
            System.exit(1);
        }

        String configFile = args[0];

        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String idLine = br.readLine();
            id = Integer.parseInt(idLine.trim());

            String logFileLine = br.readLine();
            messageLogFile = logFileLine.trim();

            String coordinatorLine = br.readLine();
            String[] coordinatorInfo = coordinatorLine.trim().split(" ");
            host = coordinatorInfo[0];
            port = Integer.parseInt(coordinatorInfo[1]);
        } catch (IOException e) {
            System.err.println("Error reading configuration file: " + e.getMessage());
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in configuration file: " + e.getMessage());
            System.exit(1);
        }
    } // initializeParticipant

    private static void connectToServer() {
        try {
            channel = SocketChannel.open();
            InetSocketAddress server = new InetSocketAddress(host, port);
            channel.connect(server);
            while(!channel.finishConnect()) {
                Thread.sleep(10);
            }
            isConnected = true;
            System.out.println("Connected to server");
        } catch (IOException | InterruptedException e) {
            System.out.println("Failed to connect to server");
        }
    }

    private static void handleUserCommands() {
        while (true) {
            System.out.print("Enter command: ");
            String command = input.nextLine().trim();
            String[] parse = command.split(" ");
            int portNumber;

            if (command.equalsIgnoreCase("quit")) {
                closeConnection();
                break;
            }

            switch (parse[0].toLowerCase()) {
                case "register":
                    if (parse.length < 2) {
                        System.out.println("Usage: register [portnumber]");
                        break;
                    }
                    portNumber = Integer.parseInt(parse[1]);
                    // TODO: check that thread B is operational
                    sendCommand("register " + id + " " + host + " " + portNumber);
                    break;
                case "deregister":
                    // TODO: check thread-B relinquished port before sending the deregister
                    sendCommand("deregister " + id);
                    break;
                case "disconnect":
                    // TODO: check thread-B relinquished port and is dormant before disconnecting
                    sendCommand("disconnect " + id);
                    System.out.println("Sent command");
                    try {
                        ByteBuffer responseBuffer = ByteBuffer.allocate(MAX_MESSAGE_LENGTH);
                        int bytesRead = channel.read(responseBuffer);
                
                        if (bytesRead > 0) {
                            System.out.println("In if bytesRead > 0");
                            responseBuffer.flip(); // Prepare buffer for reading
                            byte[] responseData = new byte[bytesRead];
                            responseBuffer.get(responseData);
                            String response = new String(responseData).trim();
                            System.out.println("See response: " + response);

                            // Check if the acknowledgment is what we expect
                            if ("Disconnected!".equals(response)) {
                                System.out.println("Disconnected! Disconnected from server.");
                                closeConnection();
                                System.out.println("Should have enter command prompt here");
                            } else {
                                System.out.println("Error: Unexpected response: " + response);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Error: Failed to receive acknowledgment: " + e.getMessage());
                    }                
                    break;
                case "reconnect":
                    if (parse.length < 2) {
                        System.out.println("Usage: reconnect [portnumber]");
                        break;
                    }
                    portNumber = Integer.parseInt(parse[1]);
                    // TODO: check thread-b is operational before sending reconnect
                    if (true) { // placeholder
                        // TODO: retrieve actual last message timestamp
                        LocalDateTime lastMessageTime = LocalDateTime.now();
                        reconnect();
                        sendCommand("reconnect " + id + " " + lastMessageTime + " " + host + " " + portNumber);
                    }
                    // reconnect();
                    // sendCommand("reconnect " + parse[1]);
                    break;
                case "multicast":
                    if (parse.length < 2) {
                        System.out.println("Usage: msend [message]");
                        break;
                    }

                    StringBuilder messageBuilder = new StringBuilder();
                    for (int i = 1; i < parse.length; i++) {
                        messageBuilder.append(parse[i]);
                        if (i < parse.length - 1) {
                            messageBuilder.append(" ");
                        }
                    }
                    String message = messageBuilder.toString();
                    sendCommand("multicast " + id + " " + message);
                    // TODO: unblock after receiving ACK 
                    break;
                default:
                    System.out.println("Unknown command.");
            }
        }
    }

    private static void sendCommand (String command) {
        if (!isConnected) {
            System.out.println("Not connected to the server.");
            return;
        }
        try {
            ByteBuffer buf = ByteBuffer.wrap(command.getBytes());
            while (buf.hasRemaining()) {
                channel.write(buf);
            }
            buf.clear();
            readResponse(channel);
        } catch (IOException e) {
            System.out.println("Error sending command: " + e.getMessage());
        }
    }

    private static void reconnect() {
        closeConnection();
        System.out.println("Reconnecting...");
        connectToServer();
    }

    private static void closeConnection() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
                System.out.println("Disconnected from server.");
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
        isConnected = false;
    }

    private static int readResponse(SocketChannel channel) throws IOException {
        ByteBuffer rbuf = ByteBuffer.allocate(4);
        int error = readFull(channel, rbuf, 4);
        if (error <= 0) {
            if (error == 0) {
                System.out.println("EOF");
            } else {
                System.out.println("Read Error");
            }
            return -1;
        }

        rbuf.flip();
        int len = rbuf.getInt();
        if (len > MAX_MESSAGE_LENGTH) {
            System.out.println("Too Long");
            return -1;
        }

        rbuf = ByteBuffer.allocate(len);
        error = readFull(channel, rbuf, len);
        if (error <= 0) {
            System.out.println("Read Error");
            return -1;
        }

        rbuf.flip();
        String response = new String(rbuf.array(), 0, rbuf.limit());
        System.out.println(response);
        return 1;
    }

    private static int readFull(SocketChannel channel, ByteBuffer rbuf, int size) throws IOException {
        while (size > 0) {
            int bytesRead = channel.read(rbuf);
            if (bytesRead <= 0) {
                return bytesRead; //error or eof
            }
            size -= bytesRead;
        }
        return 1;
    }

    /**
     * Thread-B: listen for multicast messages and log them
     * Goal: listens for incoming messages from coordinator 
     * via multicast socket, writes to log file 
     */
    private static void receiveMulticastMessages() {
        try {
            // TODO: open multicast socket on the port, join group
            while (true) {
                // TODO: check if participant is online. if offline, terminate or pause thread
                Thread.sleep(5000); // simulated waiting
                System.out.println("5000 ms passed.");
                // TODO: read actual multicast message from socket 
                String simulatedMessage = "Received message at " + LocalDateTime.now();
                System.out.println("Simulated message: " + simulatedMessage);
                System.out.println("Attempting to log message.");
                logMessage(simulatedMessage);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Logs messages to a file in order
     * Messages stored with timestamps in structured format
     * 
     * @param message Multicast message to be logged
     */
    private static void logMessage (String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(messageLogFile, true))) {
            String formattedMessage = String.format("[%s] %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                message);
            out.println(formattedMessage);
            System.out.println("Logged message.");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }


}
