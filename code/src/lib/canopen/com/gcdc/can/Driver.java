// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can;

import java.io.IOException;

public interface Driver
{
    int startTransfer() throws Exception;
    
    void stopTransfer();
    
    void sendMessage(final CanMessage p0) throws IOException;
    
    CanMessage getMessage();
    
    void setMessageConsumer(final CanMessageConsumer p0);
    
    int setFilter(final int p0);
    
    int[] getFilter();
    
    int deleteFilter(final int p0);
    
    int setCanSpeed(final int p0);
    
    int getCanSpeed();
    
    int setSocketIP(final String p0);
    
    String getSocketIP();
    
    int setSocketPort(final int p0);
    
    int getSocketPort();
}
