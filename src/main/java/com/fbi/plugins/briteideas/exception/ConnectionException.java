// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.exception;

import java.io.IOException;

public class ConnectionException extends IOException
{
    public ConnectionException() {
    }
    
    public ConnectionException(final String message) {
        super(message);
    }
    
    public ConnectionException(final Throwable throwable) {
        super(throwable);
    }
}
