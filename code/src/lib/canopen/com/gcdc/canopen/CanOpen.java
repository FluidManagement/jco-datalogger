// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.util.concurrent.atomic.AtomicInteger;
import com.gcdc.can.CanMessage;
import java.util.Iterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.gcdc.can.Driver;
import com.gcdc.can.CanMessageConsumer;

public class CanOpen extends Thread implements CanMessageConsumer
{
    private Driver busDriver;
    private boolean debug;
    private Nmt nmt;
    private Sdo sdo;
    private Pdo pdo;
    private Protocol sync;
    private Protocol time;
    private Protocol emcy;
    private Protocol heartbeat;
    private Protocol lss;
    private OperationalState canOpenState;
    private int nodeId;
    ObjectDictionary objdict;
    List<CanOpenListener> eventListeners;
    
    private void debugPrint(final String s) {
        if (this.debug) {
            System.out.println(";" + s);
        }
    }
    
    @Override
    public void run() {
        try {
            try {
                this.startTasks();
                synchronized (this) {
                    this.wait();
                }
            }
            catch (InterruptedException ex) {
                System.out.println("interrupted: " + ex);
                ex.printStackTrace();
                this.toRebootState();
            }
        }
        catch (Exception ex2) {
            System.out.println(ex2);
        }
    }
    
    public CanOpen(final Driver busDriver, final ObjectDictionary objdict, final int nodeId, final boolean debug) {
        this.debug = false;
        this.nmt = null;
        this.sdo = null;
        this.pdo = null;
        this.sync = null;
        this.time = null;
        this.emcy = null;
        this.heartbeat = null;
        this.lss = null;
        this.canOpenState = OperationalState.UNKNOWN;
        this.objdict = null;
        this.debug = debug;
        this.debugPrint("CanOpen Init");
        if (objdict != null) {
            this.objdict = objdict;
        }
        this.eventListeners = new ArrayList<CanOpenListener>();
        this.setNodeId(nodeId);
        this.busDriver = busDriver;
        this.nmt = new Nmt(this.busDriver, false, this, this.objdict);
        this.toInitializationState();
        this.setName("CanOpen Thread");
    }
    
    public void startTasks() throws Exception {
        try {
            this.nmt.start();
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
        this.busDriver.setMessageConsumer(this);
        this.busDriver.startTransfer();
        this.toPreoperationalState();
    }
    
    public ObjectDictionary getObjDict() {
        return this.objdict;
    }
    
    public void addEventListener(final CanOpenListener canOpenListener) {
        this.eventListeners.add(canOpenListener);
    }
    
    public void removeEventListener(final CanOpenListener canOpenListener) {
        this.eventListeners.remove(canOpenListener);
    }
    
    private void notifyListeners() {
        final Iterator<CanOpenListener> iterator = this.eventListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onEvent(this);
        }
    }
    
    public void addSyncListener(final CanOpenListener canOpenListener) {
        this.sync.addListener(canOpenListener);
    }
    
    public void removeSyncListener(final CanOpenListener canOpenListener) {
        this.sync.removeListener(canOpenListener);
    }
    
    public void addRxPdoIndex(final int n) throws COException {
        this.pdo.appendRxPdoFromIndex(n);
    }
    
    @Override
    public void processMessage(final CanMessage canMessage) {
        int n = 0;
        try {
            if (canMessage == null) {
                this.debugPrint("null message recevied");
                return;
            }
            n = canMessage.id >> 7;
            switch (n) {
                case 1: {
                    if (canMessage.id == 128 && this.sync != null) {
                        this.sync.processMessage(canMessage);
                        break;
                    }
                    this.debugPrint("pmEMCY");
                    if (this.emcy != null && this.emcy.processMessage(canMessage)) {
                        this.emcy.notifyListeners(canMessage);
                        break;
                    }
                    break;
                }
                case 2: {
                    this.debugPrint("pmTIME_STAMP");
                    if (this.time.processMessage(canMessage) && this.time != null) {
                        this.time.notifyListeners(canMessage);
                        break;
                    }
                    break;
                }
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10: {
                    if (this.pdo != null) {
                        this.pdo.processMessage(canMessage);
                        break;
                    }
                    break;
                }
                case 11:
                case 12: {
                    this.debugPrint("pmSDO");
                    if (this.sdo != null) {
                        this.sdo.processMessage(canMessage);
                        break;
                    }
                    System.out.println(";sdo instance is null");
                    break;
                }
                case 14: {
                    if (this.heartbeat != null) {
                        this.heartbeat.processMessage(canMessage);
                        break;
                    }
                    break;
                }
                case 0: {
                    this.debugPrint("pmNMT");
                    if (this.nmt != null) {
                        this.nmt.processMessage(canMessage);
                        break;
                    }
                    break;
                }
                case 15: {
                    this.debugPrint("pmLSS");
                    if (this.lss.processMessage(canMessage) && this.lss != null) {
                        this.lss.notifyListeners(canMessage);
                        break;
                    }
                    break;
                }
                default: {
                    this.debugPrint("pmUNKNOWN canopen message type");
                    canMessage.dump();
                    break;
                }
            }
        }
        catch (Exception ex) {
            System.out.println("exception: " + ex);
            System.out.println("msg type: " + n);
            canMessage.dump();
            ex.printStackTrace();
            System.exit(-1);
        }
    }
    
    public boolean isInitState() {
        return this.canOpenState == OperationalState.INIT;
    }
    
    public boolean isPreOpState() {
        return this.canOpenState == OperationalState.PREOPERATIONAL;
    }
    
    public boolean isStoppedState() {
        return this.canOpenState == OperationalState.STOPPED;
    }
    
    public boolean isOperationalState() {
        return this.canOpenState == OperationalState.OPERATIONAL;
    }
    
    public boolean isResetCommState() {
        return this.canOpenState == OperationalState.RESET_COMM;
    }
    
    public boolean isResetNodeState() {
        return this.canOpenState == OperationalState.RESET_NODE;
    }
    
    int getNmtState() {
        switch (this.canOpenState) {
            case INIT: {
                return 0;
            }
            case PREOPERATIONAL: {
                return 127;
            }
            case STOPPED: {
                return 4;
            }
            case OPERATIONAL: {
                return 5;
            }
            case RESET_NODE:
            case RESET_COMM: {
                return 113;
            }
            default: {
                return 255;
            }
        }
    }
    
    public String getStateString() {
        switch (this.canOpenState) {
            case INIT: {
                return "Init";
            }
            case PREOPERATIONAL: {
                return "Preoperational";
            }
            case STOPPED: {
                return "Stopped";
            }
            case OPERATIONAL: {
                return "Operational";
            }
            case RESET_NODE: {
                return "Reset Node";
            }
            case RESET_COMM: {
                return "Reset Communications";
            }
            default: {
                return "Unknown";
            }
        }
    }
    
    void toRebootState() {
        this.debugPrint("toRebootState");
        if (this.sdo != null) {
            this.sdo.stop();
            this.sdo = null;
        }
        if (this.pdo != null) {
            this.pdo.stop();
            this.pdo = null;
        }
        if (this.sync != null) {
            this.sync.stop();
            this.sync = null;
        }
        if (this.time != null) {
            this.time.stop();
            this.time = null;
        }
        if (this.emcy != null) {
            this.emcy.stop();
            this.emcy = null;
        }
        if (this.heartbeat != null) {
            this.heartbeat.stop();
            this.heartbeat = null;
        }
        if (this.lss != null) {
            this.lss.stop();
            this.lss = null;
        }
        System.gc();
    }
    
    void toInitializationState() {
        try {
            this.sdo = new Sdo(this.busDriver, this.debug, this.objdict);
            this.pdo = new Pdo(this.busDriver, this.debug, this.objdict);
            this.heartbeat = new Heartbeat(this.busDriver, this.debug, this.objdict, this);
            this.sync = new Sync(this.busDriver, this.debug, this.pdo, this.objdict);
        }
        catch (Exception ex) {
            System.out.println(";" + ex);
            ex.printStackTrace();
            System.exit(0);
        }
        this.time = new Protocol(this.busDriver, this.debug, "TIME_STAMP", this.objdict);
        this.emcy = new Protocol(this.busDriver, this.debug, "EMCY", this.objdict);
        this.lss = new Protocol(this.busDriver, this.debug, "LSS", this.objdict);
        this.heartbeat.addCobId(new AtomicInteger(1792));
        this.canOpenState = OperationalState.INIT;
        this.notifyListeners();
    }
    
    void toPreoperationalState() {
        this.debugPrint("toPreop");
        try {
            this.sdo.start();
            this.emcy.start();
            this.sync.start();
            this.pdo.stop();
            this.lss.start();
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
        this.canOpenState = OperationalState.PREOPERATIONAL;
        this.notifyListeners();
        try {
            this.nmt.sendBootUp();
        }
        catch (Exception ex2) {
            System.out.println(ex2);
        }
    }
    
    void toOperationalState() {
        this.debugPrint("toOperational");
        try {
            this.sdo.start();
            this.emcy.start();
            this.sync.start();
            this.heartbeat.start();
            this.pdo.start();
            this.lss.stop();
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
        this.canOpenState = OperationalState.OPERATIONAL;
        this.notifyListeners();
    }
    
    void toStoppedState() {
        this.debugPrint("toStopped");
        try {
            this.sdo.stop();
            this.emcy.stop();
            this.sync.stop();
            this.heartbeat.start();
            this.pdo.stop();
            this.lss.start();
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
        this.canOpenState = OperationalState.STOPPED;
        this.notifyListeners();
    }
    
    void toResetNodeState() {
        this.debugPrint("toResetNodeState");
        this.sdo.stop();
        this.emcy.stop();
        this.sync.stop();
        this.heartbeat.stop();
        this.pdo.stop();
        this.lss.stop();
        this.canOpenState = OperationalState.RESET_NODE;
        this.notifyListeners();
        this.toPreoperationalState();
    }
    
    void toResetCommState() {
        this.debugPrint("toResetCommState");
        this.sdo.stop();
        this.emcy.stop();
        this.sync.stop();
        this.heartbeat.stop();
        this.pdo.stop();
        this.lss.stop();
        this.canOpenState = OperationalState.RESET_NODE;
        this.objdict.reset(0, 8191);
        this.notifyListeners();
        this.toPreoperationalState();
    }
    
    private void setPdoCobId(final int n, final int n2) throws Exception {
        final OdEntry entry = this.objdict.getEntry(n);
        entry.getSub(0).getInt();
        final SubEntry sub = entry.getSub(1);
        sub.getInt();
        sub.set(n2);
    }
    
    void setNodeId(final int nodeId) {
        if (nodeId <= 0 || nodeId > 127) {
            System.out.println(";Invalid NodeID " + nodeId);
            return;
        }
        try {
            final OdEntry entry = this.objdict.getEntry(4608);
            entry.getSub(0).getInt();
            final SubEntry sub = entry.getSub(1);
            sub.getInt();
            sub.setIgnorePermissions(0x600 | nodeId);
            final SubEntry sub2 = entry.getSub(2);
            sub2.getInt();
            sub2.setIgnorePermissions(0x580 | nodeId);
            this.setPdoCobId(6144, 0x180 | nodeId);
            this.setPdoCobId(6145, 0x280 | nodeId);
            this.setPdoCobId(6146, 0x380 | nodeId);
            this.setPdoCobId(6147, 0x480 | nodeId);
            this.nodeId = nodeId;
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }
    
    int getNodeId() {
        return this.nodeId;
    }
    
    enum OperationalState
    {
        INIT, 
        PREOPERATIONAL, 
        STOPPED, 
        OPERATIONAL, 
        RESET_NODE, 
        RESET_COMM, 
        UNKNOWN;
    }
}
