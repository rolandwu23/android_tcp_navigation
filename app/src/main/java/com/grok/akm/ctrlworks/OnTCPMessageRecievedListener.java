package com.grok.akm.ctrlworks;

public interface OnTCPMessageRecievedListener {
    public void onTCPMessageRecieved(String message);
    public void onConnect(boolean connect);
}
