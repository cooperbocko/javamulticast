package com.mycompany.app.utils;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class ParticipantMessageListenUtils {

    public static void CheckForMessages(SocketChannel channel, ByteBuffer buf) {
        buf.clear();
        try {
            int bytesRead = channel.read(buf);
            if (bytesRead > 0) {
                buf.flip();
                byte[] message = new byte[buf.remaining()];
                buf.get(message);
                System.out.println(new String(message));
            } else if (bytesRead == -1) {
                System.out.println("Read Error");
            } //if
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } //try
    } //ListenForMessages
    
} //ParticipantMessageListenUtils
