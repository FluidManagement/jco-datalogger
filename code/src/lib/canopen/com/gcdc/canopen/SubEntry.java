// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

public class SubEntry
{
    AccessType accessType;
    byte byte1;
    byte resetByte1;
    List<CanOpenListener> listeners;
    DataType dataType;
    int size;
    Object pObject;
    Object pResetObj;
    String pname;
    Boolean pdoMapping;
    
    SubEntry(final AccessType accessType, final DataType dataType, final int size, final String s) {
        this.pdoMapping = false;
        this.accessType = accessType;
        this.dataType = dataType;
        this.size = size;
        this.pname = s.trim();
        this.listeners = new ArrayList<CanOpenListener>();
    }
    
    SubEntry(final AccessType accessType, final String s, final String s2) {
        this(accessType, DataType.visible_string, s2.length(), s);
        this.pObject = new String(s2);
        this.pResetObj = new String(s2);
    }
    
    SubEntry(final AccessType accessType, final String s, final int n) {
        this(accessType, DataType.uint32, 4, s);
        this.pObject = new AtomicInteger(n);
        this.pResetObj = new AtomicInteger(n);
    }
    
    SubEntry(final AccessType accessType, final String s, final short n) {
        this(accessType, DataType.uint16, 2, s);
        this.pObject = new AtomicInteger(n);
        this.pResetObj = new AtomicInteger(n);
    }
    
    SubEntry(final AccessType accessType, final String s, final byte byte1) {
        this(accessType, DataType.uint8, 1, s);
        this.byte1 = byte1;
        this.pObject = null;
    }
    
    SubEntry(final AccessType accessType, final String s, final boolean b) {
        this(accessType, DataType.int8, 1, s);
        this.pObject = new Boolean(b);
        this.pResetObj = new Boolean(b);
    }
    
    void reset() {
        if (this.pResetObj != null) {
            if (this.dataType == DataType.uint16 || this.dataType == DataType.uint32 || this.dataType == DataType.int8) {
                this.pObject = new AtomicInteger(((AtomicInteger)this.pResetObj).get());
            }
            else if (this.dataType == DataType.visible_string) {
                this.pObject = new String((String)this.pResetObj);
            }
            else {
                System.out.println("SubEntry.reset() type not supported");
            }
        }
        else if (this.dataType == DataType.uint8) {
            this.byte1 = this.resetByte1;
        }
        else {
            System.out.println("SubEntry.reset() type not supported");
        }
    }
    
    void dump() {
        System.out.println(" " + this.accessType + "  " + this.dataType + "  \tsize:" + this.size);
    }
    
    public void addListener(final CanOpenListener canOpenListener) {
        this.listeners.add(canOpenListener);
    }
    
    public void removeListener(final CanOpenListener canOpenListener) {
        this.listeners.remove(canOpenListener);
    }
    
    private void notifyListeners() {
        final Iterator<CanOpenListener> iterator = this.listeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onObjDictChange(this);
        }
    }
    
    String toIniString(final int n, final int n2) {
        final StringBuilder sb = new StringBuilder("[");
        sb.append(String.format("%04Xsub%d]\r\n", n, n2));
        sb.append("ParameterName=");
        sb.append(this.pname);
        sb.append("\r\n");
        sb.append("ObjectType=0x7\r\n");
        sb.append(this.toIniString());
        return sb.toString();
    }
    
    String getStringValue() {
        if (this.dataType == DataType.uint8) {
            return String.format("%02x", this.byte1);
        }
        if (this.dataType == DataType.uint32) {
            return String.format("%08x", ((AtomicInteger)this.pObject).intValue());
        }
        if (this.dataType == DataType.uint16) {
            return String.format("%04x", ((AtomicInteger)this.pObject).intValue() & 0xFFFF);
        }
        return "Unable to cast datatype to int";
    }
    
    String toIniString() {
        final StringBuilder sb = new StringBuilder("DataType=");
        sb.append(String.format("0x%04X", this.dataType.value));
        sb.append("\r\nAccessType=");
        sb.append(new String("" + this.accessType).toLowerCase());
        sb.append("\r\nDefaultValue=0x");
        sb.append(this.getStringValue());
        if (this.pdoMapping) {
            sb.append("\r\nPDOMapping=0x1\r\n");
        }
        else {
            sb.append("\r\nPDOMapping=0x0\r\n");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return this.toIniString();
    }
    
    ByteBuffer getByteBuffer() throws COException {
        final ByteBuffer allocate = ByteBuffer.allocate(this.size);
        allocate.order(ByteOrder.LITTLE_ENDIAN);
        if (this.dataType == DataType.uint8) {
            allocate.put(this.byte1);
        }
        else if (this.dataType == DataType.uint32) {
            allocate.putInt(((AtomicInteger)this.pObject).intValue());
        }
        else if (this.dataType == DataType.uint16) {
            allocate.putShort((short)(((AtomicInteger)this.pObject).intValue() & 0xFFFF));
        }
        else {
            if (this.dataType != DataType.visible_string) {
                throw COException.notMappable("FIXME unimplimented cast to ByteBuffer");
            }
            final String s = (String)this.pObject;
            allocate.order(ByteOrder.BIG_ENDIAN);
            allocate.put(s.getBytes());
        }
        return allocate;
    }
    
    public int getInt() throws COException {
        if (this.dataType == DataType.uint8) {
            return this.byte1;
        }
        if (this.dataType == DataType.uint32) {
            return ((AtomicInteger)this.pObject).intValue();
        }
        if (this.dataType == DataType.uint16) {
            return ((AtomicInteger)this.pObject).intValue() & 0xFFFF;
        }
        throw COException.invalidLength(String.format("dataType: 0x%x", this.dataType));
    }
    
    Object getIntReference() {
        return this.pObject;
    }
    
    void set(final int n) throws COException {
        if (this.accessType == AccessType.CONST || this.accessType == AccessType.RO) {
            throw COException.isReadOnly("Value cannot be written, Read only or Const");
        }
        if (this.dataType == DataType.uint8) {
            this.byte1 = (byte)n;
        }
        else if (this.dataType == DataType.uint32 || this.dataType == DataType.int32) {
            ((AtomicInteger)this.pObject).set(n);
        }
        else {
            if (this.dataType != DataType.uint16) {
                throw COException.notMappable("unable to cast datatype from int is " + this.dataType);
            }
            ((AtomicInteger)this.pObject).set(n & 0xFFFF);
        }
        this.notifyListeners();
    }
    
    void setIgnorePermissions(final int n) throws COException {
        if (this.dataType == DataType.uint8) {
            this.byte1 = (byte)n;
        }
        else if (this.dataType == DataType.uint32 || this.dataType == DataType.int32) {
            ((AtomicInteger)this.pObject).set(n);
        }
        else {
            if (this.dataType != DataType.uint16) {
                throw COException.notMappable("unable to cast datatype from int is " + this.dataType);
            }
            ((AtomicInteger)this.pObject).set(n & 0xFFFF);
        }
        this.notifyListeners();
    }
    
    void put(final ByteBuffer byteBuffer) throws COException {
        if (this.accessType == AccessType.CONST || this.accessType == AccessType.RO) {
            throw COException.isReadOnly("Value cannot be written, Read only or Const");
        }
        if (this.dataType == DataType.uint8) {
            this.byte1 = byteBuffer.get(0);
        }
        else if (this.dataType == DataType.uint32 || this.dataType == DataType.int32) {
            ((AtomicInteger)this.pObject).set(byteBuffer.getInt(0));
        }
        else {
            if (this.dataType != DataType.uint16) {
                throw COException.notMappable("unable to cast datatype from ButBuffer to  " + this.dataType);
            }
            ((AtomicInteger)this.pObject).set(byteBuffer.getInt(0) & 0xFFFF);
        }
        this.notifyListeners();
    }
    
    enum DataType
    {
        bit(1), 
        int8(2), 
        int16(3), 
        int32(4), 
        uint8(5), 
        uint16(6), 
        uint32(7), 
        real32(8), 
        visible_string(9), 
        octet_string(10), 
        unicode_string(11), 
        time_of_day(12), 
        time_difference(13), 
        domain(15), 
        int24(16), 
        real64(17), 
        int40(18), 
        int48(19), 
        int56(20), 
        int64(21), 
        uint24(22), 
        pdo_mapping(33), 
        sdo_parameter(34), 
        identity(35);
        
        private int value;
        
        private DataType(final int value) {
            this.value = value;
        }
    }
    
    enum AccessType
    {
        RW(0), 
        WO(1), 
        RO(2), 
        CONST(3);
        
        private int value;
        
        private AccessType(final int value) {
            this.value = value;
        }
    }
}
