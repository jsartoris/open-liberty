/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.concurrent.CountDownLatch;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;

/**
 *
 */
public class H2WriteQEntry {

    private static final TraceComponent tc = Tr.register(H2WriteQEntry.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public static enum WRITE_TYPE {
        NOT_SET, SYNC, ASYNC
    };

    WsByteBuffer buf = null;
    WsByteBuffer[] bufs = null;
    long minToWrite;
    int timeout;
    WRITE_TYPE wType = WRITE_TYPE.NOT_SET;
    int priority = 0;
    FrameTypes frameType = FrameTypes.UNKNOWN;
    int payloadLength = 0;

    // Latch that will signal when the write is seen as done at the TCP Channel layer.   A rule of the TCP Channel is that no two writes
    // can be outstanding at the same time (which would be really confusing to everyone if there were).
    CountDownLatch writeCompleteLatch = null;

    // when true, it means this request was put on the Queue, as such it is the thread that owns the queue, and not the write calling thread
    // that is processing this entry
    boolean servicedOnQ = true;

    H2TCPConnectionContext connectionContext = null;
    TCPWriteCompletedCallback callback = null;

    int streamID = 0;

    boolean forceQueue = false;

    public H2WriteQEntry(WsByteBuffer inBuf, WsByteBuffer[] inBufs, long inMin, int inTimeout, WRITE_TYPE inType, FrameTypes fType, int inPayloadLength, int inStreamID) {

        //  For a Sync write entry, the following are not use:  callback, forceQueue, connectionContext.
        this(inBuf, inBufs, inMin, null, false, inTimeout, null, inType, fType, inPayloadLength, inStreamID);

    }

    public H2WriteQEntry(WsByteBuffer inBuf, WsByteBuffer[] inBufs, long inMin, TCPWriteCompletedCallback inCallback,
                         boolean inForceQueue, int inTimeout, H2TCPConnectionContext inConnectionContext, WRITE_TYPE inType,
                         FrameTypes fType, int inPayloadLength, int inStreamID) {
        buf = inBuf;
        bufs = inBufs;
        minToWrite = inMin;
        timeout = inTimeout;
        wType = inType;
        callback = inCallback;
        forceQueue = inForceQueue;
        connectionContext = inConnectionContext;
        frameType = fType;
        payloadLength = inPayloadLength;
        streamID = inStreamID;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "H2WriteQEntry constructor for entry: " + this.hashCode());
        }

    }

    public void armWriteCompleteLatch() {
        writeCompleteLatch = new CountDownLatch(1);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeCompleteLatch armed: " + writeCompleteLatch.hashCode() + " on entry: " + this.hashCode());
        }
    }

    public void waitWriteCompleteLatch() {
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeCompleteLatch await: " + writeCompleteLatch.hashCode() + " on entry: " + this.hashCode());
            }
            writeCompleteLatch.await();
        } catch (InterruptedException e) {

        }
    }

    public void hitWriteCompleteLatch() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeCompleteLatch hit countDown: " + writeCompleteLatch.hashCode() + " on entry: " + this.hashCode());
        }
        writeCompleteLatch.countDown();
    }

    public boolean getServicedOnQ() {
        return servicedOnQ;
    }

    public void setServicedOnQ(boolean x) {
        servicedOnQ = x;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int x) {
        priority = x;
    }

    // Only do get methods on parameters that are only set at the time of construction

    public WsByteBuffer getBuffer() {
        return buf;
    }

    public WsByteBuffer[] getBuffers() {
        return bufs;
    }

    public long getMinToWrite() {
        return minToWrite;
    }

    public WRITE_TYPE getWriteType() {
        return wType;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean getForceQueue() {
        return forceQueue;
    }

    public TCPWriteCompletedCallback getCallback() {
        return callback;
    }

    public H2TCPConnectionContext getConnectionContext() {
        return connectionContext;
    }

    public int getStreamID() {
        return streamID;
    }

    public FrameTypes getFrameType() {
        return frameType;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

}
