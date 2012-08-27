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

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.dcache.vehicles.XrootdProtocolInfo;
import org.dcache.pool.movers.AbstractNettyServer;
import org.dcache.util.PortRange;
import org.dcache.xrootd.core.XrootdDecoder;
import org.dcache.xrootd.core.XrootdEncoder;
import org.dcache.xrootd.core.XrootdHandshakeHandler;
import org.dcache.xrootd.plugins.ChannelHandlerFactory;
import org.dcache.xrootd.protocol.XrootdProtocol;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pool-netty server tailored to the requirements of the xrootd protocol.
 * @author tzangerl
 *
 */
public class XrootdPoolNettyServer
    extends AbstractNettyServer<XrootdProtocolInfo>
{
    private final static Logger _logger =
        LoggerFactory.getLogger(XrootdPoolNettyServer.class);

    private static final PortRange DEFAULT_PORTRANGE = new PortRange(20000, 25000);

    /**
     * Used to generate channel-idle events for the pool handler
     */
    private final Timer _timer = new HashedWheelTimer();

    private final long _clientIdleTimeout;

    private int _numberClientConnections;
    private List<ChannelHandlerFactory> _plugins;

    public XrootdPoolNettyServer(int threadPoolSize,
                                 int memoryPerConnection,
                                 int maxMemory,
                                 long clientIdleTimeout,
                                 List<ChannelHandlerFactory> plugins) {
        this(threadPoolSize,
             memoryPerConnection,
             maxMemory,
             clientIdleTimeout,
             plugins,
             -1);
    }

    public XrootdPoolNettyServer(int threadPoolSize,
                                 int memoryPerConnection,
                                 int maxMemory,
                                 long clientIdleTimeout,
                                 List<ChannelHandlerFactory> plugins,
                                 int socketThreads) {
        super(threadPoolSize,
              memoryPerConnection,
              maxMemory,
              socketThreads);
        _clientIdleTimeout = clientIdleTimeout;
        _plugins = plugins;

        String range = System.getProperty("org.globus.tcp.port.range");
        PortRange portRange =
            (range != null) ? PortRange.valueOf(range) : DEFAULT_PORTRANGE;
        setPortRange(portRange);
    }

    @Override
    protected ChannelPipelineFactory newPipelineFactory() {
        return new XrootdPoolPipelineFactory();
    }

    /**
     * Only shutdown the server if no client connection left.
     */
    @Override
    protected synchronized void conditionallyStopServer() throws IOException {
        if (_numberClientConnections == 0) {
            super.conditionallyStopServer();
        }
    }

    public synchronized void clientConnected()
    {
        _numberClientConnections++;
    }

    public synchronized void clientDisconnected() throws IOException {
        _numberClientConnections--;
        conditionallyStopServer();
    }

    private class XrootdPoolPipelineFactory implements ChannelPipelineFactory {
        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = pipeline();

            pipeline.addLast("encoder", new XrootdEncoder());
            pipeline.addLast("decoder", new XrootdDecoder());
            if (_logger.isDebugEnabled()) {
                pipeline.addLast("logger",
                                 new LoggingHandler(XrootdPoolNettyServer.class));
            }
            pipeline.addLast("handshake",
                             new XrootdHandshakeHandler(XrootdProtocol.DATA_SERVER));
            pipeline.addLast("executor", new ExecutionHandler(getDiskExecutor()));
            for (ChannelHandlerFactory plugin: _plugins) {
                pipeline.addLast("plugin:" + plugin.getName(),
                        plugin.createHandler());
            }
            pipeline.addLast("timeout", new IdleStateHandler(_timer,
                                                             0,
                                                             0,
                                                             _clientIdleTimeout,
                                                             TimeUnit.MILLISECONDS));
            pipeline.addLast("transfer",
                             new XrootdPoolRequestHandler(XrootdPoolNettyServer.this));
            return pipeline;
        }
    }
}
