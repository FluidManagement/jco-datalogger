// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.nio.ByteOrder;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.gcdc.can.CanMessage;

class SdoSession
{
    SubEntry subEntry;
    int txCobId;
    boolean inProgress;
    Sdo sdo;
    CanMessage msg;
    int index;
    int subIndex;
    ByteBuffer bbSeg;
    int toggle;
    
    SdoSession(final Sdo sdo, final int txCobId, final SubEntry subEntry) {
        this.sdo = sdo;
        this.subEntry = subEntry;
        this.txCobId = txCobId;
        this.inProgress = false;
        this.toggle = 0;
    }
    
    private int extractN2() {
        return this.msg.data[0] >> 2 & 0x3;
    }
    
    private int extractCommandSpecifier() {
        return (0xFF & this.msg.data[0]) >> 5;
    }
    
    private int extractN3() {
        return this.msg.data[0] >> 1 & 0x7;
    }
    
    private int extractExpidited() {
        return this.msg.data[0] >> 1 & 0x1;
    }
    
    private int extractSizeInd() {
        return this.msg.data[0] & 0x1;
    }
    
    private int extractEndTrans() {
        return this.msg.data[0] & 0x1;
    }
    
    private int extractToggle() {
        return this.msg.data[0] >> 4 & 0x1;
    }
    
    private boolean segmentDownloadRequest() throws COException, IOException {
        if (this.toggle != this.extractToggle()) {
            throw COException.noToggle("toggle: " + this.extractToggle());
        }
        this.bbSeg.put(this.msg.data, 1, 7 - this.extractN3());
        final byte[] array = new byte[8];
        array[0] = (byte)(0x20 | this.toggle << 4);
        for (int i = 1; i < 8; ++i) {
            array[i] = 0;
        }
        System.out.println(";SDO. Send response to download request defined at index 0x120");
        this.sdo.send(this.txCobId, array);
        if (this.toggle == 0) {
            this.toggle = 1;
        }
        else {
            this.toggle = 0;
        }
        return false;
    }
    
    private boolean downloadRequest() throws COException, IOException {
        this.index = Protocol.extractIndex(this.msg);
        this.subIndex = Protocol.extractSubIndex(this.msg);
        this.inProgress = true;
        if (this.extractExpidited() != 0) {
            final int n = 4 - this.extractN2();
            final ByteBuffer allocate = ByteBuffer.allocate(4);
            allocate.put(this.msg.data, 4, n);
            allocate.order(ByteOrder.LITTLE_ENDIAN);
            try {
                this.subEntry.put(allocate);
            }
            catch (Exception ex) {
                System.out.println(";" + this.getClass() + "downloadRequest() error : Unable to copy the data in the object dictionary " + ex);
                try {
                    this.sdo.sendAbort(this.index, this.subIndex, 134217728);
                }
                catch (Exception ex2) {
                    System.out.println(";" + this.getClass() + ".downloadRequest()  error: Unable to send abort message " + ex2);
                }
            }
        }
        else if (this.extractSizeInd() != 0) {
            final ByteBuffer wrap = ByteBuffer.wrap(this.msg.data, 4, 4);
            wrap.order(ByteOrder.LITTLE_ENDIAN);
            this.bbSeg = ByteBuffer.allocate(wrap.getInt(0));
        }
        final byte[] array = { 96, (byte)(this.index & 0xFF), (byte)(this.index >> 8 & 0xFF), (byte)this.subIndex, 0, 0, 0, 0 };
        for (int i = 4; i < 8; ++i) {
            array[i] = 0;
        }
        this.sdo.send(this.txCobId, array);
        return false;
    }
    
    private boolean segmentUploadRequest() throws COException, IOException {
        if (this.toggle != this.extractToggle()) {
            System.out.println(";SDO error : Toggle error : " + this.extractToggle());
            this.sdo.sendAbort(this.index, this.subIndex, 84082688);
            return false;
        }
        final int remaining = this.bbSeg.remaining();
        final byte[] array = new byte[8];
        array[0] = (byte)(this.toggle << 4);
        if (this.toggle == 0) {
            this.toggle = 1;
        }
        else {
            this.toggle = 0;
        }
        if (remaining > 7) {
            this.bbSeg.get(array, 1, 7);
            this.sdo.send(this.txCobId, array);
            return true;
        }
        final byte[] array2 = array;
        final int n = 0;
        array2[n] |= (byte)(0x1 | 7 - remaining << 1);
        this.bbSeg.get(array, 1, remaining);
        for (int i = remaining + 1; i < 8; ++i) {
            array[i] = 0;
        }
        this.sdo.send(this.txCobId, array);
        return false;
    }
    
    private boolean uploadRequest() throws COException, IOException {
        this.index = Protocol.extractIndex(this.msg);
        this.subIndex = Protocol.extractSubIndex(this.msg);
        if (this.inProgress) {
            System.out.println(";" + this.getClass() + ".uploadRequest()  sdo in progress");
            this.sdo.sendAbort(this.index, this.subIndex, 134217761);
        }
        this.inProgress = true;
        (this.bbSeg = this.subEntry.getByteBuffer()).position(0);
        final int remaining = this.bbSeg.remaining();
        if (remaining > 4) {
            this.sdo.send(this.txCobId, new byte[] { 65, (byte)(this.index & 0xFF), (byte)(this.index >> 8 & 0xFF), (byte)this.subIndex, (byte)remaining, (byte)(remaining >> 8), (byte)(remaining >> 16), (byte)(remaining >> 24) });
            return true;
        }
        final byte[] array = this.bbSeg.array();
        final byte[] array2 = { (byte)(0x40 | 4 - remaining << 2 | 0x3), (byte)(this.index & 0xFF), (byte)(this.index >> 8 & 0xFF), (byte)this.subIndex, 0, 0, 0, 0 };
        for (int i = 0; i < 4; ++i) {
            if (i < array.length) {
                array2[4 + i] = array[i];
            }
            else {
                array2[4 + i] = 0;
            }
        }
        this.sdo.send(this.txCobId, array2);
        return false;
    }
    
    boolean processMessage(final CanMessage msg) throws COException, IOException {
        this.msg = msg;
        final int commandSpecifier = this.extractCommandSpecifier();
        switch (commandSpecifier) {
            case 0: {
                return this.segmentDownloadRequest();
            }
            case 1: {
                if (this.inProgress) {
                    System.out.println(";" + this.getClass() + "processMessage(), SDO error : Transmission already started.");
                    this.sdo.sendAbort(this.index, this.subIndex, 134217761);
                    return false;
                }
                return this.downloadRequest();
            }
            case 2: {
                return this.uploadRequest();
            }
            case 3: {
                return this.segmentUploadRequest();
            }
            case 4: {
                System.out.println(";Recieved abort code: " + commandSpecifier + "  Index: 0x" + String.format("%04X", Protocol.extractIndex(msg)).toUpperCase() + "  sub: 0x" + String.format("%02X", Protocol.extractSubIndex(msg)).toUpperCase());
                msg.dump();
                return false;
            }
            default: {
                System.out.println(";Unimplemented SDO command specifier");
                return false;
            }
        }
    }
}
