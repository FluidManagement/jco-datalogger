// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class ObjectDictionary
{
    HashMap<Integer, OdEntry> hmap;
    
    ObjectDictionary() {
        this.hmap = null;
        this.hmap = new HashMap<Integer, OdEntry>();
    }
    
    static String toIndexFmt(final int n) {
        return String.format("%04X", n).toUpperCase();
    }
    
    public void addListener(final int n, final int n2, final CanOpenListener canOpenListener) throws Exception {
        this.getSubEntry(n, n2).addListener(canOpenListener);
    }
    
    public OdEntry getEntry(final int n) throws COException {
        final OdEntry odEntry = this.hmap.get(n);
        if (odEntry == null) {
            throw COException.noObject("Index: 0x" + toIndexFmt(n));
        }
        return odEntry;
    }
    
    public SubEntry getSubEntry(final int n, final int n2) throws COException {
        return this.getEntry(n).getSub(n2);
    }
    
    void insert(final OdEntry odEntry) {
        this.hmap.put(odEntry.index, odEntry);
    }
    
    public void insertRxpdo(final String s, final int n, int n2, final int n3, final int n4, final int n5) {
        if (n2 > 256) {
            n2 &= 0xFF;
            System.out.println(";Warning index > 0x100");
        }
        this.insert(OdEntry.pdoFactory(5120 + n2, "RPDO " + Integer.toString(n2 + 1), n, 255, 0, 0));
        final int[] array = new int[n4];
        final OdEntry odEntry = new OdEntry(n3, s);
        this.insert(odEntry);
        for (int i = 0; i < n4; ++i) {
            array[i] = (n3 << 16 | i << 8 | n5);
            odEntry.appendSub(new SubEntry(SubEntry.AccessType.RW, Integer.toString(i + 1), 0));
        }
        this.insert(OdEntry.pdoMappingFactoryArray(5632 + n2, "RPDO " + Integer.toString(n2 + 1) + " mapping parameter", array));
    }
    
    void dump() {
        for (final Map.Entry<Integer, OdEntry> entry : this.hmap.entrySet()) {
            System.out.print("key is: " + entry.getKey() + " & Value is: ");
            entry.getValue().dump();
        }
    }
    
    void reset(final int n, final int n2) {
        for (final Map.Entry<Integer, OdEntry> entry : this.hmap.entrySet()) {
            final int index = entry.getValue().index;
            if (index >= n && index <= n2) {
                entry.getValue().reset();
            }
        }
    }
}
