// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can.dummy;

import com.gcdc.can.CanMessage;
import java.util.TimerTask;
import java.util.Timer;
import com.gcdc.can.ConfigFile;
import com.gcdc.can.CanMessageConsumer;
import com.gcdc.can.Driver;

public class DummyDriver implements Driver
{
    private CanMessageConsumer cmc;
    private ConfigFile cf;
    private Timer timer;
    
    public DummyDriver(final ConfigFile cf, final boolean b) {
        this.cf = cf;
        if (b) {
            System.out.println("Dummy Driver Loaded");
        }
    }
    
    @Override
    public int startTransfer() {
        final int intValue = new Integer(this.cf.getEntry("driver.dummy.intervall"));
        (this.timer = new Timer()).schedule(new Task(new Integer(this.cf.getEntry("driver.dummy.highID"))), intValue, intValue);
        return 0;
    }
    
    @Override
    public void stopTransfer() {
        this.timer.cancel();
    }
    
    @Override
    public void sendMessage(final CanMessage canMessage) {
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
        return 0;
    }
    
    @Override
    public String getSocketIP() {
        return "0.0.0.0";
    }
    
    @Override
    public int setSocketPort(final int n) {
        return 0;
    }
    
    @Override
    public int getSocketPort() {
        return 33333;
    }
    
    @Override
    public void setMessageConsumer(final CanMessageConsumer cmc) {
        this.cmc = cmc;
    }
}
