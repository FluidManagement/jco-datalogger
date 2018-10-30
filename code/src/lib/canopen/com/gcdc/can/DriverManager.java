// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can;

import com.gcdc.can.dummy.DummyDriver;
import com.gcdc.can.socket.SocketDriver;
import com.gcdc.can.datagram.DatagramDriver;

public class DriverManager
{
    public final int CAN_ERROR = -1;
    public final int CAN_SUCCESSFULL = 0;
    public final int CAN_TRANSFER_SUCCESS = 1;
    public final int CAN_TRANSFER_ERROR = 2;
    public final int CAN_NOT_SUPPORTED = 3;
    private ConfigFile cf;
    private Driver driver;
    private boolean loaded;
    private boolean DEBUG;
    
    public DriverManager(final ConfigFile cf) {
        this.loaded = false;
        this.cf = cf;
        this.DEBUG = new Boolean(cf.getEntry("driver.debug"));
        this.loadDriver(this.getDriverName());
    }
    
    public DriverManager(final String s, final String socketIP, final int socketPort, final boolean debug) {
        this.loaded = false;
        this.cf = null;
        this.DEBUG = debug;
        this.loaded = true;
        if (s.toLowerCase().startsWith("datagram")) {
            this.driver = new DatagramDriver(this.DEBUG);
        }
        else {
            this.driver = new SocketDriver(this.DEBUG);
        }
        this.driver.setSocketIP(socketIP);
        this.driver.setSocketPort(socketPort);
    }
    
    private void loadDriver(final String s) {
        this.loaded = true;
        if (s.equals("can_socket")) {
            (this.driver = new SocketDriver(this.DEBUG)).setSocketIP(this.cf.getEntry("driver.socket.ip"));
            this.driver.setSocketPort(Integer.parseInt(this.cf.getEntry("driver.socket.port")));
        }
        else if (s.equals("can_dummy")) {
            this.driver = new DummyDriver(this.cf, this.DEBUG);
        }
        else {
            System.out.println("Driver not found");
        }
    }
    
    public void unloadDriver() {
        if (!this.loaded) {
            return;
        }
        this.driver.stopTransfer();
        this.driver = null;
        System.gc();
    }
    
    public void reloadDriver() {
        if (!this.loaded) {
            return;
        }
        this.unloadDriver();
        this.loadDriver(this.getDriverName());
    }
    
    public Driver getDriver() {
        return this.driver;
    }
    
    String getDriverName() {
        return this.cf.getEntry("driver.name");
    }
}
