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

import org.dcache.pool.movers.MoverChannel;
import org.dcache.vehicles.XrootdProtocolInfo;
import org.dcache.xrootd.protocol.messages.ReadRequest;
import org.dcache.xrootd.protocol.messages.WriteRequest;
import org.dcache.xrootd.protocol.messages.SyncRequest;

/**
 * Encapsulates an open file for reading in the xrootd data server.
 */
public class ReadDescriptor implements FileDescriptor
{
    /**
     * Update mover meta-information
     */
    private MoverChannel<XrootdProtocolInfo> _channel;

    public ReadDescriptor(MoverChannel<XrootdProtocolInfo> channel)
    {
        _channel = channel;
    }

    @Override
    public Reader read(ReadRequest msg)
    {
        return new RegularReader(msg, this);
    }

    @Override
    public void sync(SyncRequest msg)
    {
        /* As this is a read only file, there is no reason to sync
         * anything.
         */
    }

    @Override
    public void write(WriteRequest msg)
        throws IOException
    {
        throw new IOException("File is read only");
    }

    @Override
    public MoverChannel<XrootdProtocolInfo> getChannel()
    {
        return _channel;
    }
}

