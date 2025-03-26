package com.mycompany.app.participant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Participant {
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private LocalDateTime timeOfLastMessage;
    private static Scanner input = new Scanner(System.in);

    
    public static void main(String args[]) throws IOException {
        
        boolean msgHandler = false;

        SocketChannel channel = null;

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
            if (!msgHandler) {
                new AwaitMessage(channel);
                msgHandler = true;
            }
            //readResonse(channel);
        }
        //channel.close();
    }

    private static int readResonse(SocketChannel channel) throws IOException {
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
    
}

class AwaitMessage implements Runnable {

    private final SocketChannel serverChannel;
    private static final int MAX_MESSAGE_LENGTH = 4096;

    public AwaitMessage(SocketChannel channel) {
        serverChannel = channel;
        run();
    }

    @Override
        public void run() {
            while (true) {
            try {
            readResonse(serverChannel);
            } catch (Exception e){
                System.out.println(e);
            }
        }
        }

        private static int readResonse(SocketChannel channel) throws IOException {
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

}