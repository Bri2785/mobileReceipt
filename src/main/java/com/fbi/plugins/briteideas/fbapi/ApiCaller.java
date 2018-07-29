// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.fbapi;

import com.fbi.plugins.briteideas.exception.FishbowlException;
import com.fbi.fbo.message.response.ResponseBase;
import com.fbi.fbo.message.request.RequestBase;
import com.fbi.fbo.impl.ApiCallType;

@FunctionalInterface
public interface ApiCaller
{
    ResponseBase call(final ApiCallType p0, final RequestBase p1) throws FishbowlException;
}
