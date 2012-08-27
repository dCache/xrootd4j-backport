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
package org.dcache.http;

import static org.jboss.netty.channel.Channels.pipeline;

import java.util.concurrent.TimeUnit;

import diskCacheV111.vehicles.HttpProtocolInfo;
import org.dcache.pool.movers.AbstractNettyServer;
import org.dcache.util.PortRange;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used for encapsulating the netty HTTP server that serves client
 * connections to the mover.
 *
 * @author tzangerl
 *
 */
public class HttpPoolNettyServer
    extends AbstractNettyServer<HttpProtocolInfo>
{
    private final static Logger _logger =
        LoggerFactory.getLogger(HttpPoolNettyServer.class);

    private final static PortRange DEFAULT_PORTRANGE =
        new PortRange(20000, 25000);

    private final Timer _timer = new HashedWheelTimer();

    private final long _clientIdleTimeout;

    private final int _chunkSize;

    public HttpPoolNettyServer(int threadPoolSize,
                               int memoryPerConnection,
                               int maxMemory,
                               int chunkSize,
                               long clientIdleTimeout) {
        this(threadPoolSize,
             memoryPerConnection,
             maxMemory,
             chunkSize,
             clientIdleTimeout,
             -1);
    }

    public HttpPoolNettyServer(int threadPoolSize,
                               int memoryPerConnection,
                               int maxMemory,
                               int chunkSize,
                               long clientIdleTimeout,
                               int socketThreads) {
        super(threadPoolSize,
              memoryPerConnection,
              maxMemory,
              socketThreads);

        _clientIdleTimeout = clientIdleTimeout;
        _chunkSize = chunkSize;

        String range = System.getProperty("org.globus.tcp.port.range");
        PortRange portRange =
            (range != null) ? PortRange.valueOf(range) : DEFAULT_PORTRANGE;
        setPortRange(portRange);
    }

    @Override
    protected ChannelPipelineFactory newPipelineFactory() {
        return new HttpPoolPipelineFactory();
    }

    /**
     * Factory that creates new server handler.
     *
     * The pipeline can handle HTTP compression and chunked transfers.
     *
     * @author tzangerl
     *
     */
    class HttpPoolPipelineFactory implements ChannelPipelineFactory {

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = pipeline();

            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("encoder", new HttpResponseEncoder());

            if (_logger.isDebugEnabled()) {
                pipeline.addLast("logger",
                                 new LoggingHandler(HttpProtocol_2.class));
            }
            pipeline.addLast("executor",
                             new ExecutionHandler(getDiskExecutor()));
            pipeline.addLast("idle-state-handler",
                             new IdleStateHandler(_timer,
                                                  0,
                                                  0,
                                                  _clientIdleTimeout,
                                                  TimeUnit.MILLISECONDS));
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            pipeline.addLast("transfer", new HttpPoolRequestHandler(HttpPoolNettyServer.this, _chunkSize));

            return pipeline;
        }
    }
}
