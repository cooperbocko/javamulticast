package com.mycompany.app.utils;

import java.nio.channels.SocketChannel;

public class Connection {
    public int id;
    public SocketChannel channel;
    public String readIp;
    public int readPort;
    public boolean isOnline;
    public char[] incoming;
    public char[] outgoing;

}
