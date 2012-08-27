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

import java.io.PrintWriter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jboss.netty.channel.Channels.*;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.Channel;

import org.dcache.cells.CellCommandListener;
import org.dcache.cells.CellInfoProvider;
import dmg.util.Args;
import dmg.cells.nucleus.CellInfo;

/**
 * Channel handler that keeps track of connected channels. Provides
 * administrative commands for listing and killing connections.
 */
@Sharable
public class ConnectionTracker
    extends SimpleChannelHandler
    implements CellCommandListener,
               CellInfoProvider
{
    private Map<Integer, Channel> _channels = new ConcurrentHashMap<Integer, Channel>();
    private AtomicInteger _counter = new AtomicInteger();

    public ConnectionTracker()
    {
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx,
                                 ChannelStateEvent e)
        throws Exception
    {
        super.channelConnected(ctx, e);
        Channel channel = e.getChannel();
        _channels.put(channel.getId(), channel);
        _counter.getAndIncrement();
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
                                    ChannelStateEvent e)
        throws Exception
    {
        _channels.remove(e.getChannel().getId());
        super.channelDisconnected(ctx, e);
    }

    @Override
    public CellInfo getCellInfo(CellInfo info)
    {
        return info;
    }

    @Override
    public void getInfo(PrintWriter pw)
    {
        pw.println(String.format("Active : %d", _channels.size()));
        pw.println(String.format("Created: %d", _counter.get()));
    }

    public String ac_connections(Args args)
    {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Integer,Channel> e: _channels.entrySet()) {
            Channel c = e.getValue();
            s.append(e.getKey()).append(" ").append(c.getRemoteAddress())
                    .append("\n");
        }
        return s.toString();
    }

    public String ac_kill_$_1(Args args)
    {
        int id = Integer.parseInt(args.argv(1));
        Channel channel = _channels.get(id);
        if (channel == null) {
            return "No such connection";
        }

        close(channel);

        return "";
    }
}