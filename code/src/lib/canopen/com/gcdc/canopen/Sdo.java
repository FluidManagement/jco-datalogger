// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import com.gcdc.can.CanMessage;
import java.io.IOException;
import com.gcdc.can.Driver;
import java.util.HashMap;

class Sdo extends Protocol
{
    static final int SDOABT_TOGGLE_NOT_ALTERNED = 84082688;
    static final int SDOABT_TIMED_OUT = 84148224;
    static final int SDOABT_OUT_OF_MEMORY = 84148229;
    static final int SDOABT_GENERAL_ERROR = 134217728;
    static final int SDOABT_LOCAL_CTRL_ERROR = 134217761;
    static final int OD_READ_NOT_ALLOWED = 100728833;
    static final int OD_WRITE_NOT_ALLOWED = 100728834;
    static final int OD_NO_SUCH_OBJECT = 100794368;
    static final int OD_NOT_MAPPABLE = 100925505;
    static final int OD_LENGTH_DATA_INVALID = 101122064;
    static final int OD_NO_SUCH_SUBINDEX = 101253137;
    static final int OD_VALUE_RANGE_EXCEEDED = 101253168;
    static final int OD_VALUE_TOO_LOW = 101253169;
    static final int OD_VALUE_TOO_HIGH = 101253170;
    HashMap<Integer, SdoSession> sessions;
    
    Sdo(final Driver driver, final boolean b, final ObjectDictionary objectDictionary) throws Exception {
        super(driver, b, "SDO", objectDictionary);
        this.sessions = null;
        this.addCobId(4608, 1);
        this.addCobId(4608, 2);
        this.debugPrint("new Sdo");
        this.sessions = new HashMap<Integer, SdoSession>();
    }
    
    void sendAbort(final int n, final int n2, final int n3) throws IOException {
        final byte[] array = new byte[8];
        this.debugPrint("Sending SDO abort " + n3);
        array[0] = -128;
        array[1] = (byte)(n & 0xFF);
        array[2] = (byte)(n >> 8 & 0xFF);
        array[3] = (byte)n2;
        array[4] = (byte)(n3 & 0xFF);
        array[5] = (byte)(n3 >> 8 & 0xFF);
        array[6] = (byte)(n3 >> 16 & 0xFF);
        array[7] = (byte)(n3 >> 24 & 0xFF);
        int int1;
        try {
            int1 = this.getSubEntry(4608, 2).getInt();
        }
        catch (Exception ex) {
            System.out.println(";Sdo.sendAbort(), error getting cobid " + ex);
            return;
        }
        this.send(int1, array);
    }
    
    void send(final int n, final byte[] array) throws IOException {
        this.sendMessage(new CanMessage(n, 0, array));
    }
    
    @Override
    boolean processMessage(final CanMessage canMessage) {
        int index = 0;
        int subIndex = 0;
        if (!super.processMessage(canMessage)) {
            return false;
        }
        if (canMessage.length != 8) {
            try {
                this.debugPrint("Error size SDO is " + canMessage.length);
                this.sendAbort(0, 0, 134217728);
            }
            catch (Exception ex) {
                System.out.println(";Sdo error when sending abort: " + ex);
            }
            return false;
        }
        try {
            SdoSession sdoSession = this.sessions.get(canMessage.id);
            if (sdoSession == null) {
                index = Protocol.extractIndex(canMessage);
                subIndex = Protocol.extractSubIndex(canMessage);
                sdoSession = new SdoSession(this, this.getSubEntry(4608, 2).getInt(), this.getSubEntry(index, subIndex));
                this.sessions.put(canMessage.id, sdoSession);
            }
            if (!sdoSession.processMessage(canMessage)) {
                this.sessions.remove(canMessage.id);
            }
        }
        catch (COException ex2) {
            System.out.println(ex2);
            try {
                this.sendAbort(index, subIndex, ex2.getErrorCode());
            }
            catch (IOException ex3) {
                System.out.println(";Sdo.processMessage: " + ex3);
            }
            return false;
        }
        catch (IOException ex4) {
            System.out.println(";Sdo.processMessage: " + ex4);
            return false;
        }
        this.notifyListeners(canMessage);
        return true;
    }
    
    public void run() {
        this.debugPrint("Sdo expired timer");
    }
}
