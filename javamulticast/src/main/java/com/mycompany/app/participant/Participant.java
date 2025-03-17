package com.mycompany.app.participant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Participant {

    /*
     * Time of last message
     * One thread to send a message
     * One thread to recieve data and write to conf
     */
    public static void main(String args[]) throws IOException {
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

        String message = "Hello";
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
