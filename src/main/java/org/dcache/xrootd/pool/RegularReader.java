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
import java.nio.ByteBuffer;

import org.dcache.pool.repository.RepositoryChannel;
import org.dcache.xrootd.protocol.messages.GenericReadRequestMessage;
import org.dcache.xrootd.protocol.messages.ReadRequest;
import org.dcache.xrootd.protocol.messages.ReadResponse;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

/**
 * Reader for kXR_read requests.
 */
public class RegularReader implements Reader
{
    private final ReadRequest _request;
    private long _position;
    private int _length;
    private FileDescriptor _descriptor;

    public RegularReader(ReadRequest request, FileDescriptor descriptor)
    {
        _request = request;
        _position = _request.getReadOffset();
        _length = _request.bytesToRead();
        _descriptor = descriptor;
    }

    @Override
    public GenericReadRequestMessage getRequest()
    {
        return _request;
    }

    /**
     * Returns the next response message for this read
     * request. Returns null if all data has been read.
     */
    @Override
    public ReadResponse read(int maxFrameSize)
        throws IOException
    {
        if (_length == 0) {
            return null;
        }

        RepositoryChannel channel = _descriptor.getChannel();

        int length = Math.min(_length, maxFrameSize);
        RegularReadResponse response = new RegularReadResponse(_request);
        length = response.write(channel, _position, length);
        _position += length;
        _length -= length;
        response.setIncomplete(_length != 0);

        return response;
    }

    /**
     * Specialized response for regular read requests.
     *
     * In contrast to ReadResponse, RegularReadResponse supports position
     * independent read from a RepositoryChannel.
     */
    private static class RegularReadResponse extends ReadResponse
    {
        public RegularReadResponse(ReadRequest request) {
            super(request, 0);
        }

        public int write(RepositoryChannel channel, long srcIndex, int length)
                throws IOException
        {
            int remaining = length;
            byte[] chunkArray = new byte[length];
            ByteBuffer chunk = ByteBuffer.wrap(chunkArray);

            while (remaining > 0) {
                /* use position independent thread safe call */
                int bytes = channel.read(chunk, srcIndex);
                if (bytes < 0) {
                    break;
                }

                srcIndex += bytes;
                remaining -= bytes;
            }

            _buffer = wrappedBuffer(_buffer,
                    wrappedBuffer(chunkArray, 0, length - remaining));

            return length - remaining;
        }
    }
}

