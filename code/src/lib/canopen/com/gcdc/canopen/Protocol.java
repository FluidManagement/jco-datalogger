// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.util.Iterator;
import java.io.IOException;
import java.util.ArrayList;
import com.gcdc.can.CanMessage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import com.gcdc.can.Driver;

class Protocol
{
    private Driver busDriver;
    protected boolean debug;
    protected boolean isEnabled;
    protected String name;
    static final int NMT = 0;
    static final int SYNC = 1;
    static final int TIME_STAMP = 2;
    static final int PDO1tx = 3;
    static final int PDO1rx = 4;
    static final int PDO2tx = 5;
    static final int PDO2rx = 6;
    static final int PDO3tx = 7;
    static final int PDO3rx = 8;
    static final int PDO4tx = 9;
    static final int PDO4rx = 10;
    static final int SDOtx = 11;
    static final int SDOrx = 12;
    static final int NODE_GUARD = 14;
    static final int LSS = 15;
    private List<AtomicInteger> cobIdList;
    private ObjectDictionary objDict;
    private List<CanOpenListener> messageListeners;
    
    void debugPrint(final String s) {
        if (this.debug) {
            System.out.println(";" + s);
        }
    }
    
    static int extractIndex(final CanMessage canMessage) {
        return (canMessage.data[2] << 8 & 0xFF00) | (canMessage.data[1] & 0xFF);
    }
    
    static int extractSubIndex(final CanMessage canMessage) {
        return canMessage.data[3] & 0xFF;
    }
    
    static String toIndexFmt(final int n) {
        return String.format("%04X", n).toUpperCase();
    }
    
    Protocol(final Driver busDriver, final boolean b, final String name, final ObjectDictionary objDict) {
        this.debug = false;
        this.isEnabled = false;
        if (b) {
            this.debug = true;
        }
        this.debugPrint("new Protocol " + name);
        this.busDriver = busDriver;
        this.cobIdList = new ArrayList<AtomicInteger>();
        this.name = name;
        this.objDict = objDict;
        this.messageListeners = new ArrayList<CanOpenListener>();
    }
    
    boolean start() throws IOException {
        if (this.isEnabled) {
            return false;
        }
        this.isEnabled = true;
        this.debugPrint("Protocol timers starting for " + this.name);
        return true;
    }
    
    boolean stop() {
        if (!this.isEnabled) {
            return false;
        }
        this.isEnabled = false;
        return true;
    }
    
    boolean processMessage(final CanMessage canMessage) {
        return this.isEnabled && this.isValidCobId(canMessage.id);
    }
    
    void sendMessage(final CanMessage canMessage) throws IOException {
        this.busDriver.sendMessage(canMessage);
    }
    
    void addCobId(final int n, final int n2) throws Exception {
        this.addCobId(this.objDict.getEntry(n), n2);
    }
    
    void addCobId(final OdEntry odEntry, final int n) throws COException {
        this.addCobId((AtomicInteger)odEntry.getSub(n).getIntReference());
    }
    
    void addCobId(final AtomicInteger atomicInteger) {
        this.cobIdList.add(atomicInteger);
    }
    
    SubEntry getSubEntry(final int n, final int n2) throws COException {
        return this.objDict.getSubEntry(n, n2);
    }
    
    boolean isValidCobId(final int n) {
        final Iterator<AtomicInteger> iterator = this.cobIdList.iterator();
        while (iterator.hasNext()) {
            if (n == iterator.next().intValue()) {
                return true;
            }
        }
        return false;
    }
    
    void addListener(final CanOpenListener canOpenListener) {
        this.messageListeners.add(canOpenListener);
    }
    
    void removeListener(final CanOpenListener canOpenListener) {
        this.messageListeners.remove(canOpenListener);
    }
    
    void notifyListeners(final CanMessage canMessage) {
        final Iterator<CanOpenListener> iterator = this.messageListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onMessage(canMessage);
        }
    }
}
