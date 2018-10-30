// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import com.gcdc.can.Driver;
import java.io.IOException;
import com.gcdc.can.CanMessage;

class Sync extends Protocol
{
    private Pdo pdo;
    
    private void sendSync() throws IOException, Exception {
        this.sendMessage(new CanMessage(this.getSubEntry(4101, 1).getInt(), 0, new byte[0]));
    }
    
    Sync(final Driver driver, final boolean b, final Pdo pdo, final ObjectDictionary objectDictionary) {
        super(driver, b, "SYNC", objectDictionary);
        this.pdo = null;
        this.debugPrint("new Sync");
        this.pdo = pdo;
    }
    
    @Override
    boolean processMessage(final CanMessage canMessage) {
        if (!super.processMessage(canMessage) && canMessage.id != 128) {
            return false;
        }
        final boolean sendSyncEvents = this.pdo.sendSyncEvents();
        this.notifyListeners(canMessage);
        return sendSyncEvents;
    }
    
    @Override
    boolean start() throws IOException {
        if (super.start()) {
            this.debugPrint("sync starting");
            return true;
        }
        return false;
    }
}
