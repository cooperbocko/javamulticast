package com.mycompany.app.participant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Participant {
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private static LocalDateTime timeOfLastMessage;
    private static Scanner input = new Scanner(System.in);
    private static SocketChannel channel;
    private static boolean isConnected = false;
    private static int id;
    private static String messageLogFile;
    private static String host;
    private static int port;
    
    public static void main(String args[]) throws IOException {
        initializeParticipant(args);
        connectToServer();
        Thread userInputThread = new Thread(Participant::handleUserCommands);
        userInputThread.start();
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

            if (command.equalsIgnoreCase("quit")) {
                closeConnection();
                break;
            }

            switch (parse[0].toLowerCase()) {
                case "register":
                    // TODO: check that thread B is operational
                    sendCommand("register " + id + " " + host + " " + port);
                    timeOfLastMessage = LocalDateTime.now();
                    break;
                case "deregister":
                    // TODO: check thread-B relinquished port before sending the deregister
                    sendCommand("deregister " + id);
                    break;
                case "disconnect":
                    // TODO: check thread-B relinquished port and is dormant before disconnecting
                    sendCommand("disconnect " + id);            
                    break;
                case "reconnect":
                    // TODO: check thread-b is operational before sending reconnect
                    reconnect();
                    sendCommand("reconnect " + id + " " + timeOfLastMessage.toString());
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
        System.out.println("Attempting to read response from channel..."); // gets here 

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
        System.out.println("Received message length: " + len);

        if (len > MAX_MESSAGE_LENGTH) {
            System.out.println("Too Long");
            return -1;
        }

        rbuf = ByteBuffer.allocate(len);
        System.out.println("Reading message of length: " + len);

        error = readFull(channel, rbuf, len);
        if (error <= 0) {
            System.out.println("Read Error");
            return -1;
        }

        rbuf.flip();
        String response = new String(rbuf.array(), 0, rbuf.limit());
        System.out.println("Received response: " + response);
        return 1;
    }

    public static int readFull(SocketChannel channel, ByteBuffer rbuf, int size) throws IOException {
        System.out.println("Starting to read full data of size: " + size);
        while (size > 0) {
            int bytesRead = channel.read(rbuf);
            System.out.println("Bytes read in this iteration: " + bytesRead);

            if (bytesRead <= 0) {
                if (bytesRead == 0) {
                    System.out.println("Warning: No bytes read, possibly the channel is not ready.");
                } else {
                    System.out.println("Error: End of stream or read error occurred.");
                }
                return bytesRead; //error or eof
            }
            size -= bytesRead;
            System.out.println("Remaining size to read: " + size);

        }
        System.out.println("Successfully read full data.");
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
