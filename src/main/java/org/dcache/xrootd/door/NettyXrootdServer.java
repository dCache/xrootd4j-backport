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
package org.dcache.xrootd.door;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;

import diskCacheV111.util.FsPath;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelFactory;
import static org.jboss.netty.channel.Channels.*;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import org.dcache.xrootd.core.XrootdEncoder;
import org.dcache.xrootd.core.XrootdDecoder;
import org.dcache.xrootd.core.XrootdHandshakeHandler;
import org.dcache.xrootd.plugins.ChannelHandlerFactory;
import org.dcache.xrootd.protocol.XrootdProtocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty based xrootd redirector. Could possibly be replaced by pure
 * spring configuration once we move to Netty 3.1.
 */
public class NettyXrootdServer
{
    private static final Logger _log =
        LoggerFactory.getLogger(NettyXrootdServer.class);

    private int _port;
    private int _backlog;
    private Executor _requestExecutor;
    private XrootdDoor _door;
    private ChannelFactory _channelFactory;
    private ConnectionTracker _connectionTracker;
    private List<ChannelHandlerFactory> _channelHandlerFactories;
    private FsPath _rootPath;

    /**
     * Switch Netty to slf4j for logging.
     */
    static
    {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    public int getPort()
    {
        return _port;
    }

    @Required
    public void setPort(int port)
    {
        _port = port;
    }

    public int getBacklog()
    {
        return _backlog;
    }

    @Required
    public void setBacklog(int backlog)
    {
        _backlog = backlog;
    }

    @Required
    public void setRequestExecutor(Executor executor)
    {
        _requestExecutor = executor;
    }

    @Required
    public void setChannelFactory(ChannelFactory channelFactory)
    {
        _channelFactory = channelFactory;
    }

    @Required
    public void setConnectionTracker(ConnectionTracker connectionTracker)
    {
        _connectionTracker = connectionTracker;
    }

    @Required
    public void setDoor(XrootdDoor door)
    {
        _door = door;
    }

    @Required
    public void setChannelHandlerFactories(
            List<ChannelHandlerFactory> channelHandlerFactories)
    {
        _channelHandlerFactories = channelHandlerFactories;
    }

    public String getRootPath()
    {
        return (_rootPath == null) ? null : _rootPath.toString();
    }

    /**
     * Sets the root path of the name space exported by this xrootd door.
     */
    @Required
    public void setRootPath(String s)
    {
        _rootPath = new FsPath(s);
    }

    public void init()
    {
        ServerBootstrap bootstrap = new ServerBootstrap(_channelFactory);
        bootstrap.setOption("backlog", _backlog);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline()
                {
                    ChannelPipeline pipeline = pipeline();
                    pipeline.addLast("tracker", _connectionTracker);
                    pipeline.addLast("encoder", new XrootdEncoder());
                    pipeline.addLast("decoder", new XrootdDecoder());
                    if (_log.isDebugEnabled()) {
                        pipeline.addLast("logger", new LoggingHandler(NettyXrootdServer.class));
                    }
                    pipeline.addLast("handshake", new XrootdHandshakeHandler(XrootdProtocol.LOAD_BALANCER));
                    pipeline.addLast("executor", new ExecutionHandler(_requestExecutor));
                    for (ChannelHandlerFactory factory: _channelHandlerFactories) {
                        pipeline.addLast("plugin:" + factory.getName(), factory.createHandler());
                    }
                    pipeline.addLast("redirector", new XrootdRedirectHandler(_door, _rootPath));
                    return pipeline;
                }
            });

        bootstrap.bind(new InetSocketAddress(_port));
    }
}