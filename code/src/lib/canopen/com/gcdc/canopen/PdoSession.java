// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.BitSet;
import com.gcdc.can.CanMessage;
import java.util.concurrent.atomic.AtomicInteger;

class PdoSession
{
    AtomicInteger cobId;
    Pdo pdo;
    OdEntry params;
    OdEntry mapping;
    static final int TRANS_SYNC_ACYCLIC = 0;
    static final int TRANS_SYNC_MIN = 1;
    static final int TRANS_SYNC_MAX = 240;
    static final int TRANS_RTR_SYNC = 252;
    static final int TRANS_RTR = 253;
    static final int TRANS_EVENT_SPECIFIC = 254;
    static final int TRANS_EVENT_PROFILE = 255;
    private int transSyncCount;
    private CanMessage lastMsg;
    static final int[] mask;
    
    PdoSession(final Pdo pdo, final OdEntry params, final OdEntry mapping) throws COException {
        this.transSyncCount = 0;
        this.lastMsg = null;
        this.pdo = pdo;
        this.params = params;
        this.mapping = mapping;
        this.cobId = (AtomicInteger)params.getSub(1).getIntReference();
        this.debugPrint("pdo cobId: " + String.format("%04X", this.cobId.get()).toUpperCase());
        pdo.addCobId(this.cobId);
    }
    
    void debugPrint(final String s) {
    }
    
    static long getLeBitSeqAsInt64(final byte[] array, final int n, final int n2) {
        return BitSet.valueOf(array).get(n, n + n2).toLongArray()[0];
    }
    
    static int getLeBitSeqAsUInt32(final byte[] array, final int n, final int n2) {
        int n3 = 0;
        int n4 = 0;
        final int n5 = 0xFF & array[n / 8];
        if (n2 > 8) {
            n4 = (0xFF & array[1 + n / 8]);
        }
        if (n2 > 16) {
            n3 = (0xFF & array[2 + n / 8]);
        }
        int n6;
        if (n2 > 24) {
            n6 = ((0xFF & (0xFF & array[3 + n / 8]) >> 32 - n2) << 24 | n3 << 16 | n4 << 8 | n5);
        }
        else if (n2 > 16) {
            n6 = (n3 << 16 | n4 << 8 | n5);
        }
        else if (n2 > 8) {
            n6 = (n4 << 8 | n5);
        }
        else {
            n6 = (0xFF & n5);
        }
        return n6 >> n % 8 & PdoSession.mask[n2];
    }
    
    static int getLeBitSeqAsInt32(final byte[] array, final int n, final int n2) {
        return getLeBitSeqAsUInt32(array, n, n2) << 32 - n2 >> 32 - n2;
    }
    
    boolean processMessage(final CanMessage canMessage) throws Exception {
        if (canMessage.id != this.cobId.intValue()) {
            return false;
        }
        this.debugPrint("PdoSession cobId match " + String.format("%04X", canMessage.id).toUpperCase());
        final int int1 = this.mapping.getSub(0).getInt();
        int n = 0;
        final ByteBuffer allocate = ByteBuffer.allocate(8);
        allocate.put(canMessage.data, 0, canMessage.length);
        allocate.order(ByteOrder.LITTLE_ENDIAN);
        allocate.position(0);
        this.debugPrint("PdoSession.processMessage() numMaps: " + Integer.toString(int1));
        for (int i = 1; i <= int1; ++i) {
            final int int2 = this.mapping.getSub(i).getInt();
            final int n2 = 0xFFFF & int2 >> 16;
            final int n3 = 0xFF & int2 >> 8;
            final int n4 = 0xFF & int2;
            this.debugPrint("PdoSession.processMessage() index: 0x" + String.format("%04x ", n2).toUpperCase() + ", subindex: 0x" + String.format("%02x", n3).toUpperCase() + " bits: " + n4);
            final SubEntry subEntry = this.pdo.getSubEntry(n2, n3);
            if (n4 == 8) {
                subEntry.set(allocate.get());
            }
            else if (n4 == 16) {
                final short short1 = allocate.getShort();
                this.debugPrint("PdoSession.processMessage() setting Object Dict entry to 0x" + String.format("%04x", short1).toUpperCase());
                subEntry.set(short1);
            }
            else if (n4 == 32) {
                subEntry.set(allocate.getInt());
            }
            else {
                subEntry.set(getLeBitSeqAsInt32(canMessage.data, n, n4));
            }
            n += n4;
            if (n > 64) {
                break;
            }
        }
        return true;
    }
    
    private static long mask(final int n) {
        return (n == 64) ? -1L : ((1L << n) - 1L);
    }
    
    private CanMessage buildMessage() throws Exception {
        final ByteBuffer allocate = ByteBuffer.allocate(8);
        allocate.clear();
        final int int1 = this.mapping.getSub(0).getInt();
        int n = 0;
        for (int i = 1; i <= int1; ++i) {
            final int int2 = this.mapping.getSub(i).getInt();
            final int n2 = 0xFFFF & int2 >> 16;
            final int n3 = 0xFF & int2 >> 8;
            final int n4 = 0xFF & int2;
            final int int3 = this.pdo.getSubEntry(n2, n3).getInt();
            mask(n4);
            if (n4 == 8) {
                allocate.put((byte)int3);
            }
            else if (n4 == 16) {
                allocate.putShort((short)int3);
            }
            else if (n4 == 32) {
                allocate.putInt(int3);
            }
            else {
                System.out.println(";Error: PdoSession.buildPdo() " + n4 + " not supported, Fixme!");
            }
            n += n4;
            if (n > 64) {
                break;
            }
        }
        allocate.order(ByteOrder.LITTLE_ENDIAN);
        int n5 = n / 8;
        if (n % 8 != 0) {
            ++n5;
        }
        if (n5 > 8) {
            n5 = 8;
            System.out.println(";Error mapping results in msg overflow, truncating");
        }
        final byte[] array = new byte[n5];
        allocate.position(0);
        allocate.get(array, 0, n5);
        return new CanMessage(this.cobId.intValue(), 0, array);
    }
    
    boolean syncEvent() throws Exception {
        if ((this.cobId.intValue() & Integer.MIN_VALUE) == Integer.MIN_VALUE) {
            return false;
        }
        final int int1 = this.params.getSub(2).getInt();
        if (int1 >= 1 && int1 <= 240) {
            ++this.transSyncCount;
            if (this.transSyncCount >= int1) {
                this.transSyncCount = 0;
                this.lastMsg = this.buildMessage();
                this.pdo.sendMessage(this.lastMsg);
            }
        }
        else if (int1 == 252) {
            this.lastMsg = this.buildMessage();
        }
        else if (int1 == 0) {
            final CanMessage buildMessage = this.buildMessage();
            if (!buildMessage.equals(this.lastMsg)) {
                this.lastMsg = buildMessage;
                this.pdo.sendMessage(this.lastMsg);
            }
        }
        return true;
    }
    
    static {
        mask = new int[] { 0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, Integer.MAX_VALUE, -1 };
    }
}
