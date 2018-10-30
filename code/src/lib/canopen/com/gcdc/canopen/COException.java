// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

public class COException extends Exception
{
    static final int SDOABT_TOGGLE_NOT_ALTERNED = 84082688;
    static final int SDOABT_TIMED_OUT = 84148224;
    static final int SDOABT_OUT_OF_MEMORY = 84148229;
    static final int OD_READ_NOT_ALLOWED = 100728833;
    static final int OD_WRITE_NOT_ALLOWED = 100728834;
    static final int OD_NO_SUCH_OBJECT = 100794368;
    static final int OD_NOT_MAPPABLE = 100925505;
    static final int OD_LENGTH_DATA_INVALID = 101122064;
    static final int OD_NO_SUCH_SUBINDEX = 101253137;
    static final int OD_VALUE_RANGE_EXCEEDED = 101253168;
    static final int OD_VALUE_TOO_LOW = 101253169;
    static final int OD_VALUE_TOO_HIGH = 101253170;
    static final int SDOABT_GENERAL_ERROR = 134217728;
    static final int SDOABT_LOCAL_CTRL_ERROR = 134217761;
    int errorCode;
    
    COException(final String s, final int errorCode) {
        super(s);
        this.errorCode = errorCode;
    }
    
    COException(final String s, final Throwable t, final int errorCode) {
        super(s, t);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return this.errorCode;
    }
    
    public static COException noSubindex(final String s) {
        return new COException("Subindex out of range: " + s, 101253137);
    }
    
    public static COException noObject(final String s) {
        return new COException("No Such Object: " + s, 100794368);
    }
    
    public static COException invalidLength(final String s) {
        return new COException("Invalid Length: " + s, 101122064);
    }
    
    public static COException noToggle(final String s) {
        return new COException("Toggle not alternated: " + s, 84082688);
    }
    
    public static COException notMappable(final String s) {
        return new COException("Not mappable: " + s, 100925505);
    }
    
    public static COException isReadOnly(final String s) {
        return new COException("Object is Read Only: " + s, 100728834);
    }
}
