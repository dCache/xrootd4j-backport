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

import org.dcache.auth.LoginReply;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import static org.jboss.netty.channel.Channels.*;

public class LoginEvent implements ChannelEvent
{
    private final Channel _channel;
    private LoginReply _loginReply;

    public LoginEvent(Channel channel, LoginReply loginReply)
    {
        _channel = channel;
        _loginReply = loginReply;
    }

    public LoginReply getLoginReply()
    {
        return _loginReply;
    }

    @Override
    public Channel getChannel()
    {
        return _channel;
    }

    @Override
    public ChannelFuture getFuture()
    {
        return succeededFuture(getChannel());
    }
}