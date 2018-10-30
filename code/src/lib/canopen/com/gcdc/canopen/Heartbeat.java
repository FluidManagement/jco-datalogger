// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.util.Iterator;
import com.gcdc.can.CanMessage;
import java.io.IOException;
import java.util.ArrayList;
import com.gcdc.can.Driver;
import java.util.List;

class Heartbeat extends Protocol
{
    private List<HeartbeatSession> rheartbeats;
    private CanOpen canOpen;
    private OdEntry odComm;
    private HeartbeatSession txHeartbeat;
    
    Heartbeat(final Driver driver, final boolean b, final ObjectDictionary objectDictionary, final CanOpen canOpen) throws Exception {
        super(driver, b, "HEARTBEAT", objectDictionary);
        this.rheartbeats = new ArrayList<HeartbeatSession>();
        this.canOpen = canOpen;
        this.odComm = objectDictionary.getEntry(4119);
        this.debugPrint("new Heartbeat");
    }
    
    int getNmtState() {
        return this.canOpen.getNmtState();
    }
    
    @Override
    boolean start() throws IOException {
        super.start();
        if (this.txHeartbeat != null) {
            return false;
        }
        if (this.odComm == null) {
            System.out.println(";ERROR odComm is null");
        }
        (this.txHeartbeat = new HeartbeatSession(this, this.odComm)).start();
        return true;
    }
    
    @Override
    boolean stop() {
        super.stop();
        return this.stopHbSession();
    }
    
    boolean stopHbSession() {
        if (this.txHeartbeat != null) {
            this.txHeartbeat.end();
            this.txHeartbeat.interrupt();
            this.txHeartbeat = null;
            System.gc();
        }
        return true;
    }
    
    void appendRxHeartbeat(final HeartbeatSession heartbeatSession) {
        this.rheartbeats.add(heartbeatSession);
    }
    
    void send(final int n, final byte[] array) throws IOException, Exception {
        this.sendMessage(new CanMessage(n | this.canOpen.getNodeId(), 0, array));
    }
    
    @Override
    boolean processMessage(final CanMessage canMessage) {
        if (!super.processMessage(canMessage)) {
            return false;
        }
        this.debugPrint("Heartbeat.processMessage()");
        if (this.debug) {
            canMessage.dump();
        }
        final Iterator<HeartbeatSession> iterator = this.rheartbeats.iterator();
        while (iterator.hasNext()) {
            try {
                if (iterator.next().processMessage(canMessage)) {
                    this.notifyListeners(canMessage);
                    return true;
                }
                continue;
            }
            catch (Exception ex) {
                System.out.println(";ERROR: Heartbeat.processMessage() threw " + ex);
            }
        }
        System.out.println(";Warning: heartbeat cobId not found");
        return false;
    }
}
