// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can.socket;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import com.gcdc.can.CanMessage;
import java.net.ConnectException;
import java.io.OutputStream;
import com.gcdc.can.CanMessageConsumer;
import java.net.Socket;

public class Task extends Thread
{
    Socket iface;
    private CanMessageConsumer cmc;
    private OutputStream rawTx;
    
    Task(final String s, final int n) throws Exception, ConnectException {
        this.cmc = null;
        (this.iface = new Socket(s, n)).setSendBufferSize(16);
        this.iface.setTcpNoDelay(true);
        this.iface.setTrafficClass(16);
        this.rawTx = this.iface.getOutputStream();
    }
    
    public void setCmc(final CanMessageConsumer cmc) {
        this.cmc = cmc;
    }
    
    private String toByteFmt(final byte b) {
        return String.format("%02X", b);
    }
    
    private CanMessage convertRaw(final byte[] array) {
        boolean b = false;
        final ByteBuffer wrap = ByteBuffer.wrap(array, 0, 4);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        final int int1 = wrap.getInt();
        if ((int1 & Integer.MIN_VALUE) != 0x0) {}
        if ((int1 & 0x40000000) != 0x0) {
            b = true;
        }
        if ((int1 & 0x20000000) != 0x0) {}
        final int n = int1 & 0x1FFFFFFF;
        final byte[] array2 = new byte[array[4] & 0xF];
        for (int i = 0; i < array2.length; ++i) {
            array2[i] = array[i + 8];
        }
        return new CanMessage(n, b ? 1 : 0, array2);
    }
    
    private byte[] formatTxMsg(final CanMessage canMessage) {
        final byte[] array = new byte[16];
        final ByteBuffer wrap = ByteBuffer.wrap(array, 0, 4);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        int id = canMessage.id;
        if (canMessage.rtr != 0) {
            id |= 0x40000000;
        }
        wrap.putInt(id);
        array[4] = (byte)canMessage.length;
        for (int i = 0; i < canMessage.length; ++i) {
            array[8 + i] = canMessage.data[i];
        }
        return array;
    }
    
    public void sendMsg(final CanMessage canMessage) throws IOException {
        final byte[] formatTxMsg = this.formatTxMsg(canMessage);
        synchronized (this) {
            this.rawTx.write(formatTxMsg);
            this.rawTx.flush();
        }
    }
    
    @Override
    public void run() {
        try {
            final InputStream inputStream = this.iface.getInputStream();
            final byte[] array = new byte[16];
            while (true) {
                inputStream.read(array);
                final CanMessage convertRaw = this.convertRaw(array);
                if (this.cmc != null) {
                    this.cmc.processMessage(convertRaw);
                }
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
