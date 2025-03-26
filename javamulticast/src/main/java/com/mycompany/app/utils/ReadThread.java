package com.mycompany.app.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.mycompany.app.participant.Participant;

public class ReadThread implements Runnable {
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private int port;
    private ServerSocketChannel channel;
    private SocketChannel coordinator;
    private String fileName;

    public ReadThread(int port, String fileName) {
        this.port = port;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try {
            //open channel
            channel = ServerSocketChannel.open();
            channel.bind(new java.net.InetSocketAddress(port));

            while (true) {
                //waits until a new connection and reads the response and then closes the connection
                coordinator = channel.accept();
                read(coordinator);
                coordinator.close();
            }

        } catch (Exception e) {

        } finally {
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception e) {

                }
            }
        }
    }

    private int read(SocketChannel channel) throws IOException {
        System.out.println("Attempting to read response from channel..."); // gets here 

        ByteBuffer rbuf = ByteBuffer.allocate(4);
        int error = Participant.readFull(channel, rbuf, 4);
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

        error = Participant.readFull(channel, rbuf, len);
        if (error <= 0) {
            System.out.println("Read Error");
            return -1;
        }

        rbuf.flip();
        String response = new String(rbuf.array(), 0, rbuf.limit());
        System.out.println("Received response: " + response);

        //write to file
        FileWriter writer = new FileWriter(fileName);
        writer.write(response);
        writer.close();
        return 1;
    }
    
}


