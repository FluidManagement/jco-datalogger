// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import com.gcdc.can.CanMessage;
import java.util.Iterator;
import java.util.ArrayList;
import com.gcdc.can.Driver;
import java.util.List;

class Pdo extends Protocol
{
    private List<PdoSession> rpdos;
    private List<PdoSession> tpdos;
    private ObjectDictionary od1;
    
    Pdo(final Driver driver, final boolean b, final ObjectDictionary od1) throws Exception {
        super(driver, b, "PDO", od1);
        this.rpdos = new ArrayList<PdoSession>();
        this.tpdos = new ArrayList<PdoSession>();
        this.od1 = od1;
        this.debugPrint("creating Rx Pdo's from Od");
        for (int i = 0; i < 4; ++i) {
            this.appendRxPdoFromIndex(i);
        }
        this.debugPrint("creating Tx Pdo's from Od");
        for (int j = 0; j < 4; ++j) {
            this.appendTxPdo(new PdoSession(this, od1.getEntry(6144 + j), od1.getEntry(6656 + j)));
        }
        this.debugPrint("new Pdo");
    }
    
    void appendRxPdo(final PdoSession pdoSession) {
        this.rpdos.add(pdoSession);
    }
    
    void appendRxPdoFromIndex(final int n) throws COException {
        this.appendRxPdo(new PdoSession(this, this.od1.getEntry(5120 + n), this.od1.getEntry(5632 + n)));
    }
    
    void appendTxPdo(final PdoSession pdoSession) {
        this.tpdos.add(pdoSession);
    }
    
    boolean sendSyncEvents() {
        boolean b = false;
        if (!this.isEnabled) {
            return b;
        }
        final Iterator<PdoSession> iterator = this.tpdos.iterator();
        while (iterator.hasNext()) {
            try {
                if (!iterator.next().syncEvent()) {
                    continue;
                }
                b = true;
            }
            catch (Exception ex) {
                System.out.println(";ERROR: PdoSession.syncEvent() threw " + ex);
            }
        }
        return b;
    }
    
    @Override
    boolean processMessage(final CanMessage canMessage) {
        if (!super.processMessage(canMessage)) {
            return false;
        }
        Iterator<PdoSession> iterator;
        if (canMessage.rtr == 0) {
            iterator = this.rpdos.iterator();
        }
        else {
            iterator = this.tpdos.iterator();
        }
        while (iterator.hasNext()) {
            try {
                if (iterator.next().processMessage(canMessage)) {
                    this.notifyListeners(canMessage);
                    return true;
                }
                continue;
            }
            catch (Exception ex) {
                System.out.println(";ERROR: Pdo.processMessage() threw " + ex);
                canMessage.dump();
            }
        }
        System.out.print(";Warning: pdo could not be processed: ");
        canMessage.dump();
        return false;
    }
    
    public void run() {
        this.debugPrint("Pdo expired timer");
    }
}
