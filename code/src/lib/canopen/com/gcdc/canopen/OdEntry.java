// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.util.ListIterator;
import java.util.LinkedList;

public class OdEntry
{
    int index;
    LinkedList<SubEntry> subIndexList;
    String pname;
    
    OdEntry(final int n, final String s, final SubEntry subEntry) {
        this(n, s);
        this.appendSub(subEntry);
    }
    
    OdEntry(final int index, final String s) {
        this.index = index;
        this.subIndexList = new LinkedList<SubEntry>();
        this.pname = s.trim();
    }
    
    void appendSub(final SubEntry subEntry) {
        this.subIndexList.add(subEntry);
    }
    
    int size() {
        return this.subIndexList.size();
    }
    
    public String getName() {
        return this.pname;
    }
    
    public SubEntry getSub(final int n) throws COException {
        try {
            return this.subIndexList.get(n);
        }
        catch (IndexOutOfBoundsException ex) {
            throw COException.noSubindex(ex.toString());
        }
    }
    
    private String toIndexFmt() {
        return String.format("%04X", this.index).toUpperCase();
    }
    
    private String toSizeFmt() {
        return String.format("%X", this.size()).toUpperCase();
    }
    
    void dump() {
        System.out.println("Index: 0x" + this.toIndexFmt() + "   Num sub :" + this.size());
        final ListIterator<SubEntry> listIterator = this.subIndexList.listIterator(0);
        while (listIterator.hasNext()) {
            listIterator.next().dump();
        }
    }
    
    String toIniString() {
        final StringBuilder sb = new StringBuilder("[");
        sb.append(this.toIndexFmt());
        sb.append("]\r\n");
        if (this.size() > 1) {
            sb.append("SubNumber=0x");
            sb.append(this.toSizeFmt());
            sb.append("\r\n");
        }
        sb.append("ParameterName=");
        sb.append(this.pname);
        if (this.size() > 1) {
            sb.append("\r\nObjectType=0x9\r\n");
        }
        else {
            sb.append("\r\nObjectType=0x7\r\n");
        }
        if (this.size() == 1) {
            sb.append(this.subIndexList.getFirst().toIniString());
        }
        else {
            int n = 0;
            final ListIterator<SubEntry> listIterator = this.subIndexList.listIterator(0);
            while (listIterator.hasNext()) {
                sb.append("\r\n");
                sb.append(listIterator.next().toIniString(this.index, n++));
            }
        }
        return sb.toString();
    }
    
    void reset() {
        final ListIterator<SubEntry> listIterator = this.subIndexList.listIterator(0);
        while (listIterator.hasNext()) {
            listIterator.next().reset();
        }
    }
    
    static OdEntry pdoFactory(final int n, final String s, final int n2, final int n3, final int n4, final int n5) {
        final OdEntry odEntry = new OdEntry(n, s);
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.CONST, "largest subindex supported", (byte)5));
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, "COB-Id used", n2));
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, "Transmission Type", (byte)n3));
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, "Inhibit Time", (short)n4));
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, "Unused", (byte)0));
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, "Event Timer", (short)n5));
        return odEntry;
    }
    
    static OdEntry pdoMappingFactory(final int n, final String s, final int... array) {
        final OdEntry odEntry = new OdEntry(n, s);
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.CONST, "number of mapped objects", (byte)array.length));
        for (int i = 0; i < array.length; ++i) {
            odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, "PDO mapping " + i + 1 + " app. object", array[i]));
        }
        return odEntry;
    }
    
    static OdEntry pdoMappingFactoryArray(final int n, final String s, final int[] array) {
        final OdEntry odEntry = new OdEntry(n, s);
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.CONST, "number of mapped objects", (byte)array.length));
        for (int i = 0; i < array.length; ++i) {
            odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, "PDO mapping " + i + 1 + " app. object", array[i]));
        }
        return odEntry;
    }
}
