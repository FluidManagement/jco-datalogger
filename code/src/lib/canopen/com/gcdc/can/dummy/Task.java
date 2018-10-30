// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can.dummy;

import java.util.GregorianCalendar;
import com.gcdc.can.CanMessage;
import java.util.Random;
import java.util.TimerTask;

public class Task extends TimerTask
{
    private int nr_frame;
    private int highID;
    Random rand;
    
    Task(final int highID) {
        this.nr_frame = 0;
        this.rand = new Random(2048L);
        this.highID = highID;
        this.nr_frame = 0;
    }
    
    @Override
    public void run() {
        final CanMessage canMessage = new CanMessage();
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        canMessage.id = this.rand.nextInt(this.highID);
        for (byte b = 0; b < 8; ++b) {
            canMessage.data[b] = b;
        }
    }
    
    private String lenCheck(final int n) {
        if (n <= 9) {
            return "0" + n;
        }
        return "" + n;
    }
}
