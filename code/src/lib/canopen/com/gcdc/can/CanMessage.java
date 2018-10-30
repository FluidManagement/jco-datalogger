// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can;

public class CanMessage
{
    public int id;
    public byte[] data;
    public int length;
    public int rtr;
    
    public CanMessage(final int id, final int rtr, final byte[] array) {
        this.data = new byte[8];
        this.id = id;
        this.length = array.length;
        this.rtr = rtr;
        for (int i = 0; i < this.length; ++i) {
            this.data[i] = array[i];
        }
    }
    
    public CanMessage() {
        this.data = new byte[8];
        this.id = 0;
        this.length = 0;
        this.rtr = 0;
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i] = 0;
        }
    }
    
    private String toIdFmt(final int n) {
        return String.format("%03X", n).toUpperCase();
    }
    
    private String toByteFmt(final byte b) {
        return String.format("%02X", b);
    }
    
    @Override
    public int hashCode() {
        int n = 31 * (31 * (31 * 1 + this.id) + this.length) + this.rtr;
        for (int i = 0; i < this.length; ++i) {
            n = 31 * n + this.data[i];
        }
        return n;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof CanMessage)) {
            return false;
        }
        final CanMessage canMessage = (CanMessage)o;
        if (this.id == canMessage.id) {
            if (this.rtr != canMessage.rtr) {
                return false;
            }
            if (this.length == canMessage.length) {
                for (int i = 0; i < this.length; ++i) {
                    if (this.data[i] != canMessage.data[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    public void dump() {
        String string = new String();
        for (int i = 0; i < this.length; ++i) {
            string = string + " " + this.toByteFmt(this.data[i]);
        }
        System.out.println(this.toIdFmt(this.id) + "   [" + this.length + "] " + string);
    }
}
