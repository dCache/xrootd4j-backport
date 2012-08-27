/**
 * Copyright (C) 2012 dCache.org <support@dcache.org>
 *
 * This file is part of xrootd4j-backport.
 *
 * xrootd4j-backport is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * xrootd4j-backport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with xrootd4j-backport.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.dcache.pool.movers;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.SyncFailedException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.dcache.pool.repository.Allocator;
import org.dcache.pool.repository.RepositoryChannel;

import diskCacheV111.vehicles.ProtocolInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for RepositoryChannel adding features used by movers.
 */
public class MoverChannel<T extends ProtocolInfo> implements RepositoryChannel
{
    private static final Logger _logSpaceAllocation =
        LoggerFactory.getLogger("logger.dev.org.dcache.poolspacemonitor." +
                                MoverChannel.class.getName());

    /**
     * The minimum number of bytes to increment the space
     * allocation.
     */
    private static final long SPACE_INC = 50 * (1 << 20);

    /**
     * Inner channel to which most operations are delegated.
     */
    private final RepositoryChannel _channel;

    /**
     * The IoMode of the mover that created this MoverChannel.
     */
    private final IoMode _mode;

    /**
     * Space allocator provided by the pool.
     */
    private final Allocator _allocator;

    /**
     * Timestamp of when the transfer started.
     */
    private final long _transferStarted =
        System.currentTimeMillis();

    /**
     * Timestamp of when the last block was transferred.
     */
    private final AtomicLong _lastTransferred =
        new AtomicLong(_transferStarted);

    /**
     * True if the transfer any data.
     */
    private final AtomicBoolean _wasChanged =
        new AtomicBoolean(false);

    /**
     * The number of bytes transferred.
     */
    private final AtomicLong _bytesTransferred =
        new AtomicLong(0);

    /**
     * ProtocolInfo associated with the transfer.
     */
    private final T _protocolInfo;

    /**
     * The number of bytes reserved in the space allocator. Only
     * accessed while the monitor lock is held.
     */
    private long _reserved;

    public MoverChannel(IoMode mode, T protocolInfo, RepositoryChannel channel,
                        Allocator allocator)
    {
        _mode = mode;
        _protocolInfo = protocolInfo;
        _channel = channel;
        _allocator = allocator;
    }

    @Override
    public long position() throws IOException
    {
        return _channel.position();
    }

    @Override
    public synchronized MoverChannel position(long position)
        throws IOException
    {
        _channel.position(position);
        return this;
    }

    @Override
    public long size() throws IOException
    {
        return _channel.size();
    }

    @Override
    public void sync() throws SyncFailedException, IOException
    {
        _channel.sync();
    }

    @Override
    public synchronized MoverChannel truncate(long size) throws IOException
    {
        try {
            _wasChanged.set(true);
            _channel.truncate(size);
            return this;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public void close() throws IOException
    {
        _lastTransferred.set(System.currentTimeMillis());
        _channel.close();
    }

    @Override
    public boolean isOpen()
    {
        return _channel.isOpen();
    }

    @Override
    public synchronized int read(ByteBuffer dst) throws IOException
    {
        try {
            int bytes = _channel.read(dst);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public synchronized int read(ByteBuffer buffer, long position) throws IOException {
        try {
            int bytes = _channel.read(buffer, position);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public synchronized long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        try {
            long bytes = _channel.read(dsts, offset, length);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public synchronized long read(ByteBuffer[] dsts) throws IOException {
        try {
            long bytes = _channel.read(dsts);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public synchronized int write(ByteBuffer src) throws IOException {
        try {
            preallocate(position() + src.remaining());
            int bytes = _channel.write(src);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public int write(ByteBuffer buffer, long position) throws IOException {
        try {
            preallocate(position + buffer.remaining());
            _wasChanged.set(true);
            int bytes = _channel.write(buffer, position);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public synchronized long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        try {
            long remaining = 0;
            for (int i = offset; i < offset + length; i++) {
                remaining += srcs[i].remaining();
            }
            preallocate(position() + remaining);

            _wasChanged.set(true);
            long bytes = _channel.write(srcs, offset, length);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public synchronized long write(ByteBuffer[] srcs) throws IOException {
        try {
            long remaining = 0;
            for (ByteBuffer src: srcs) {
                remaining += src.remaining();
            }
            preallocate(position() + remaining);

            _wasChanged.set(true);
            long bytes = _channel.write(srcs);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        try {
            long bytes = _channel.transferTo(position, count, target);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        try {
            preallocate(position + count);
            _wasChanged.set(true);
            long bytes = _channel.transferFrom(src, position, count);
            _bytesTransferred.getAndAdd(bytes);
            return bytes;
        } finally {
            _lastTransferred.set(System.currentTimeMillis());
        }
    }

    public IoMode getIoMode() {
        return _mode;
    }

    public T getProtocolInfo() {
        return _protocolInfo;
    }

    public long getBytesTransferred() {
        return _bytesTransferred.get();
    }

    public long getTransferTime() {
        return (_channel.isOpen()
                ? System.currentTimeMillis()
                : getLastTransferred()) - _transferStarted;
    }

    public long getLastTransferred() {
        return _lastTransferred.get();
    }

    public boolean wasChanged() {
        return _wasChanged.get();
    }

    private synchronized void preallocate(long pos)
        throws IOException
    {
        try {
            checkArgument(pos >= 0);

            if (pos > _reserved) {
                long delta = Math.max(pos - _reserved, SPACE_INC);
                _logSpaceAllocation.debug("ALLOC: " + delta);
                _allocator.allocate(delta);
                _reserved += delta;
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.getMessage());
        } catch (IllegalStateException e) {
            throw new ClosedChannelException();
        }
    }
}
