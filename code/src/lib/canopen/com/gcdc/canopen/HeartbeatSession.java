// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.io.IOException;
import com.gcdc.can.CanMessage;

class HeartbeatSession extends Thread
{
    Heartbeat heartbeat;
    OdEntry params;
    boolean running;
    
    HeartbeatSession(final Heartbeat heartbeat, final OdEntry params) {
        this.running = true;
        this.heartbeat = heartbeat;
        this.params = params;
        this.setName("HeartbeatSession");
    }
    
    int getInterval() throws Exception {
        if (this.params == null) {
            System.out.println("Error HeartbeatSession.getInterval params is null");
            return 5000;
        }
        final SubEntry sub = this.params.getSub(0);
        if (sub == null) {
            System.out.println("Error HeartbeatSession.getInterval returned null");
            return 5000;
        }
        return sub.getInt();
    }
    
    public void end() {
        this.running = false;
    }
    
    boolean processMessage(final CanMessage canMessage) throws Exception {
        return true;
    }
    
    @Override
    public void run() {
        int interval = 5000;
        while (this.running) {
            final byte[] array = { (byte)this.heartbeat.getNmtState() };
            try {
                interval = this.getInterval();
                this.heartbeat.send(1792, array);
            }
            catch (IOException ex) {
                System.out.println("IO ERRORs ending Heartbeat signal: " + ex);
                System.exit(-4);
            }
            catch (Exception ex2) {
                interval = 5000;
                System.out.println("ERROR sending Heartbeat signal: " + ex2);
            }
            try {
                Thread.sleep(interval);
            }
            catch (InterruptedException ex3) {}
        }
    }
}
