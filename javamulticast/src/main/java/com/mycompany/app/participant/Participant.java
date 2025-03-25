package com.mycompany.app.participant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Participant {
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private LocalDateTime timeOfLastMessage;
    private static Scanner input = new Scanner(System.in);
    private static SocketChannel channel;
    private static boolean isConnected = false;
    private static String host = "localhost";
    private static int port = 8080;
    private static int id;

    
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
        connectToServer();
        Thread userInputThread = new Thread(Participant::handleUserCommands);
        userInputThread.start();
    }

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
                    if (parse.length < 2) {
                        System.out.println("Usage: register [portnumber]");
                        break;
                    }
                    String ip = "127.0.0.1";
                    id = 500;
                    int port = Integer.parseInt(parse[1]);
                    sendCommand("register " + id + " " + ip + " " + port);
                    break;
                case "deregister":
                    sendCommand("deregister " + getId());
                    break;
                case "disconnect":
                    sendCommand("disconnect " + getId());
                    closeConnection();
                    break;
                case "reconnect":
                    if (parse.length < 2) {
                        System.out.println("Usage: reconnect [portnumber]");
                        break;
                    }
                    int portNumber = Integer.parseInt(parse[1]);
                    // TODO: check thread-b is operational before sending reconnect
                    if (true) { // placeholder
                        LocalDateTime lastMessageTime = LocalDateTime.now();
                        reconnect();
                        sendCommand("reconnect " + getId() + " " + lastMessageTime + " " + host + " " + portNumber);
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
                    sendCommand("multicast " + getId() + " " + message);
                    break;
                default:
                    System.out.println("Unknown command.");
            }
        }
    }

    private static void sendCommand(String command) {
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

    private static int getId() {
        return id; 
    }

}
