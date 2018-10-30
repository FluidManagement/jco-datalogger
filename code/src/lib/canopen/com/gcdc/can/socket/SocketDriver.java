// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can.socket;

import java.io.IOException;
import com.gcdc.can.CanMessage;
import com.gcdc.can.CanMessageConsumer;
import com.gcdc.can.Driver;

public class SocketDriver implements Driver
{
    private String ipAddr;
    private int port;
    Task backgroundTask;
    CanMessageConsumer cmc;
    boolean debug;
    
    public SocketDriver(final boolean debug) {
        this.backgroundTask = null;
        this.debug = debug;
        if (this.debug) {
            System.out.println("Socket Driver Loaded");
        }
    }
    
    @Override
    public int startTransfer() throws Exception {
        if (this.debug) {
            System.out.println("opening thread with \"" + this.ipAddr + ":" + this.port + "\"");
        }
        (this.backgroundTask = new Task(this.ipAddr, this.port)).setCmc(this.cmc);
        this.backgroundTask.start();
        return 0;
    }
    
    @Override
    public void stopTransfer() {
    }
    
    @Override
    public void sendMessage(final CanMessage canMessage) throws IOException {
        this.backgroundTask.sendMsg(canMessage);
    }
    
    @Override
    public CanMessage getMessage() {
        return null;
    }
    
    @Override
    public int setCanSpeed(final int n) {
        return 0;
    }
    
    @Override
    public int getCanSpeed() {
        return 0;
    }
    
    @Override
    public int setFilter(final int n) {
        return 0;
    }
    
    @Override
    public int[] getFilter() {
        return null;
    }
    
    @Override
    public int deleteFilter(final int n) {
        return 0;
    }
    
    @Override
    public int setSocketIP(final String s) {
        this.ipAddr = new String(s);
        return 0;
    }
    
    @Override
    public String getSocketIP() {
        return this.ipAddr;
    }
    
    @Override
    public int setSocketPort(final int port) {
        return this.port = port;
    }
    
    @Override
    public int getSocketPort() {
        return this.port;
    }
    
    @Override
    public void setMessageConsumer(final CanMessageConsumer cmc) {
        this.cmc = cmc;
        if (this.backgroundTask != null) {
            this.backgroundTask.setCmc(this.cmc);
        }
    }
}
