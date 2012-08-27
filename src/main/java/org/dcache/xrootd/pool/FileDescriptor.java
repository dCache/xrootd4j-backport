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
package org.dcache.xrootd.pool;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.dcache.pool.movers.MoverChannel;
import org.dcache.vehicles.XrootdProtocolInfo;
import org.dcache.xrootd.protocol.messages.ReadRequest;
import org.dcache.xrootd.protocol.messages.SyncRequest;
import org.dcache.xrootd.protocol.messages.WriteRequest;

/**
 * Encapsulates an open file in the xrootd data server.
 */
public interface FileDescriptor
{
    /**
     * Returns a reader object for a given read request. The reader
     * provides read access to the file and can generate response
     * objects for the request.
     */
    Reader read(ReadRequest msg);

    /**
     * Forces unwritten data to disk.
     *
     * @throws ClosedChannelException if the descriptor is closed.
     * @throws IOException if the operation failed.
     */
    void sync(SyncRequest msg)
        throws IOException;

    /**
     * Writes data to the file.
     *
     * @throws ClosedChannelException if the descriptor is closed.
     * @throws IOException if the operation failed.
     */
    void write(WriteRequest msg)
        throws IOException;

    /**
     * Returns the FileChannel of this descriptor.
     */
    MoverChannel<XrootdProtocolInfo> getChannel();
}
