// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can.datagram;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import com.gcdc.can.CanMessage;
import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.net.InetAddress;
import java.net.DatagramPacket;
import com.gcdc.can.CanMessageConsumer;
import java.net.DatagramSocket;

public class Task extends Thread
{
    DatagramSocket socket;
    private CanMessageConsumer cmc;
    private byte[] inBuff;
    DatagramPacket inPacket;
    InetAddress address;
    int port;
    private final CountDownLatch latch;
    private final CountDownLatch bufferIsFlushed;
    private boolean rxEnabled;
    private static final int CAN_ERR_CRTL_RX_OVERFLOW = 1;
    private static final int CAN_ERR_BUSERROR = 128;
    
    Task(final String s, final int port) throws Exception, ConnectException {
        this.cmc = null;
        this.latch = new CountDownLatch(1);
        this.bufferIsFlushed = new CountDownLatch(1);
        this.rxEnabled = false;
        this.port = port;
        this.address = InetAddress.getByName(s);
        this.setName("datagramsocketTask");
        (this.socket = new DatagramSocket()).setSoTimeout(500);
        this.inBuff = new byte[256];
        this.inPacket = new DatagramPacket(this.inBuff, this.inBuff.length, this.address, port);
        this.start();
    }
    
    public void enableRx() {
        try {
            this.bufferIsFlushed.await();
            this.rxEnabled = true;
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
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
    
    private byte[] formatErrMsg(final int n, final int n2) {
        final byte[] array = new byte[16];
        final ByteBuffer wrap = ByteBuffer.wrap(array, 0, 4);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        wrap.putInt(n | 0x20000000);
        array[4] = 0;
        array[9] = (byte)n2;
        return array;
    }
    
    public void sendMsg(final CanMessage canMessage) throws IOException {
        final byte[] formatTxMsg = this.formatTxMsg(canMessage);
        final DatagramPacket datagramPacket = new DatagramPacket(formatTxMsg, formatTxMsg.length, this.address, this.port);
        try {
            this.latch.await();
            this.socket.send(datagramPacket);
        }
        catch (InterruptedException ex) {
            System.out.println(ex);
        }
    }
    
    public void close() {
        if (this.socket != null) {
            synchronized (this.socket) {
                this.socket.notify();
            }
            this.socket.close();
        }
        this.socket = null;
        System.gc();
    }
    
    @Override
    public void run() {
        try {
            this.socket.connect(this.address, this.port);
            if (this.socket.isConnected()) {
                final byte[] formatErrMsg = this.formatErrMsg(128, 1);
                this.socket.send(new DatagramPacket(formatErrMsg, formatErrMsg.length, this.address, this.port));
            }
            else {
                System.out.println("not connected");
            }
            Long n = null;
            while (true) {
                this.latch.countDown();
                try {
                    if (this.bufferIsFlushed.getCount() != 0L) {
                        this.socket.receive(this.inPacket);
                        final long nanoTime = System.nanoTime();
                        if (n == null) {
                            n = nanoTime;
                        }
                        if (nanoTime - n > 10000000L) {
                            this.bufferIsFlushed.countDown();
                        }
                        else {
                            n = nanoTime;
                        }
                    }
                    else {
                        this.socket.receive(this.inPacket);
                    }
                }
                catch (SocketTimeoutException ex3) {
                    this.bufferIsFlushed.countDown();
                    this.socket.setSoTimeout(0);
                    continue;
                }
                if (this.rxEnabled && this.bufferIsFlushed.getCount() == 0L) {
                    final CanMessage convertRaw = this.convertRaw(this.inPacket.getData());
                    if (this.cmc != null) {
                        this.cmc.processMessage(convertRaw);
                    }
                    else {
                        convertRaw.dump();
                    }
                }
            }
        }
        catch (SocketException ex) {
            System.out.println("Task.run() " + ex);
        }
        catch (IOException ex2) {
            System.out.println("Task.run() " + ex2);
            ex2.printStackTrace();
        }
    }
}
