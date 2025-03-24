package com.mycompany.app.utils;

public class Msg {
    private String message = null;
    private long timestamp = 0;

    public Msg() {
        this("");
    } // Msg()

    public Msg(String newMsg) {
        message = newMsg;
        timestamp = System.currentTimeMillis();
    } // Msg constructor

    public String getMessage() {
        return message;
    } // getMessage

    public boolean isExpired(long timer) {
        if (timestamp + timer < System.currentTimeMillis()) {
            return true;
        } else {
            return false;
        }
    } // isExpired

}
