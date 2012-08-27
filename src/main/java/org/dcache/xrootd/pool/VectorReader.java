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

import java.nio.ByteBuffer;
import java.util.List;

import java.io.IOException;

import org.dcache.pool.repository.RepositoryChannel;
import org.dcache.xrootd.protocol.messages.GenericReadRequestMessage;
import org.dcache.xrootd.protocol.messages.ReadResponse;
import org.dcache.xrootd.protocol.messages.GenericReadRequestMessage.EmbeddedReadRequest;
import org.dcache.xrootd.protocol.messages.ReadVRequest;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

/**
 * Reader for kXR_readv requests.
 */
public class VectorReader implements Reader
{
    private final ReadVRequest _request;
    private final List<FileDescriptor> _descriptors;
    private final EmbeddedReadRequest[] _requests;

    private int _index;

    public VectorReader(ReadVRequest request,
                        List<FileDescriptor> descriptors)
    {
        _request = request;
        _descriptors = descriptors;
        _requests = request.getReadRequestList();
        _index = 0;
    }

    @Override
    public GenericReadRequestMessage getRequest()
    {
        return _request;
    }

    private int getLengthOfRequest(EmbeddedReadRequest request)
            throws IOException
    {
        FileDescriptor descriptor = _descriptors.get(request.getFileHandle());
        RepositoryChannel channel = descriptor.getChannel();
        return (int) Math.min(request.BytesToRead(), channel.size() - request.getOffset());
    }

    private int getChunksInNextFrame(int maxFrameSize) throws IOException
    {
        long length = 0;
        int count = 0;
        for (int i = _index; i < _requests.length && length < maxFrameSize; i++) {
            length += ReadResponse.READ_LIST_HEADER_SIZE;
            length += getLengthOfRequest(_requests[i]);
            count++;
        }
        if (length > maxFrameSize) {
            count--;
        }
        if (count == 0) {
            throw new IllegalStateException("Maximum chunk size exceeded");
        }
        return count;
    }

    private ChannelBuffer readBlock(EmbeddedReadRequest request)
        throws IOException
    {
        FileDescriptor descriptor = _descriptors.get(request.getFileHandle());
        RepositoryChannel channel = descriptor.getChannel();

        long position = request.getOffset();
        int remaining = request.BytesToRead();
        byte[] chunkArray = new byte[remaining];
        ByteBuffer chunk = ByteBuffer.wrap(chunkArray);

        while (remaining > 0) {
            /* use position independent thread safe call */
            int bytes = channel.read(chunk, position);
            if (bytes < 0) {
                break;
            }

            position += bytes;
            remaining -= bytes;
        }

        return wrappedBuffer(chunkArray, 0, chunkArray.length - remaining);
    }

    /**
     * Returns the next response message for this read
     * request. Returns null if all data has been read.
     */
    @Override
    public ReadResponse read(int maxFrameSize)
        throws IOException
    {
        if (_index == _requests.length) {
            return null;
        }

        int count = getChunksInNextFrame(maxFrameSize);
        ChannelBuffer[] chunks = new ChannelBuffer[_requests.length];
        for (int i = _index; i < _index + count; i++) {
            chunks[i] = readBlock(_requests[i]);
        }

        VectorReadResponse response = new VectorReadResponse(_request);
        response.write(_requests, chunks, _index, count);
        response.setIncomplete(_index + count < _requests.length);
        _index += count;

        return response;
    }

    /**
     * Specialized response for vector read requests.
     *
     * In contrast to ReadResponse, VectorReadResponse supports zero-copy
     * appending the response data.
     */
    public static class VectorReadResponse extends ReadResponse
    {
        public VectorReadResponse(ReadVRequest request) {
            super(request, 0);
        }

        private ChannelBuffer getReadListHeader(EmbeddedReadRequest request, int actualLength)
        {
            ChannelBuffer buffer = ChannelBuffers.buffer(16);
            buffer.writeInt(request.getFileHandle());
            buffer.writeInt(actualLength);
            buffer.writeLong(request.getOffset());
            return buffer;
        }

        public void write(EmbeddedReadRequest[] requests,
                          ChannelBuffer[] buffers,
                          int offset, int length)
        {
            ChannelBuffer[] reply = new ChannelBuffer[2 * length + 1];
            reply[0] = _buffer;
            for (int i = 0; i < length; i++) {
                reply[2 * i + 1] = getReadListHeader(requests[offset + i], buffers[offset + i].readableBytes());
                reply[2 * i + 2] = buffers[offset + i];
            }
            _buffer = wrappedBuffer(reply);
        }
    }
}
