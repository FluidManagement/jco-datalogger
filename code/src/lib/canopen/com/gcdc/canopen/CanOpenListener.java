// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

import com.gcdc.can.CanMessage;

public interface CanOpenListener
{
    void onObjDictChange(final SubEntry p0);
    
    void onMessage(final CanMessage p0);
    
    void onEvent(final CanOpen p0);
}
