// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import com.gcdc.can.Driver;
import java.io.IOException;
import com.gcdc.can.CanMessage;

class Nmt extends Protocol
{
    private CanOpen canOpen;
    static final int GO_OPERATIONAL = 1;
    static final int GO_STOP = 2;
    static final int GO_PREOPERATIONAL = 128;
    static final int GO_RESET_NODE = 129;
    static final int GO_RESET_COMM = 130;
    
    void sendBootUp() throws IOException, Exception {
        this.sendMessage(new CanMessage(0x700 | this.canOpen.getNodeId(), 0, new byte[] { 0 }));
    }
    
    Nmt(final Driver driver, final boolean b, final CanOpen canOpen, final ObjectDictionary objectDictionary) {
        super(driver, b, "NMT", objectDictionary);
        this.canOpen = null;
        this.debugPrint("new Nmt");
        this.canOpen = canOpen;
    }
    
    @Override
    boolean processMessage(final CanMessage canMessage) {
        if (!super.processMessage(canMessage) && canMessage.id != 0) {
            return false;
        }
        this.debugPrint("Nmt.processMessage()");
        if (this.debug) {
            canMessage.dump();
        }
        final int n = 0xFF & canMessage.data[1];
        if (n != this.canOpen.getNodeId() && n != 0) {
            return false;
        }
        final int n2 = 0xFF & canMessage.data[0];
        this.debugPrint("cmd :" + n2 + " (0x" + Protocol.toIndexFmt(n2) + ")");
        switch (n2) {
            case 1: {
                this.debugPrint("Start");
                this.canOpen.toOperationalState();
                break;
            }
            case 2: {
                this.debugPrint("Stop");
                this.canOpen.toStoppedState();
                break;
            }
            case 128: {
                this.debugPrint("Preop");
                this.canOpen.toPreoperationalState();
                break;
            }
            case 129: {
                this.debugPrint("reset node");
                this.canOpen.toResetNodeState();
                break;
            }
            case 130: {
                this.debugPrint("reset comm");
                this.canOpen.toResetCommState();
                break;
            }
            default: {
                System.out.println("ERROR unknown cmd 0x" + Protocol.toIndexFmt(n2));
                return false;
            }
        }
        this.notifyListeners(canMessage);
        return true;
    }
    
    @Override
    boolean start() throws IOException {
        return super.start();
    }
}
