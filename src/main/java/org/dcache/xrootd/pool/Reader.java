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

import org.dcache.xrootd.protocol.messages.GenericReadRequestMessage;
import org.dcache.xrootd.protocol.messages.ReadResponse;


/**
 * Encapsulates a read request. To avoid that we deplete memory space,
 * we only read as much data as we can write to the socket without
 * buffering. Hence a single read request may be broken into smaller
 * blocks internally. Each block is returned as an incomplete xrootd
 * response (with an "ok so far" response code).
 */
public interface Reader
{
    ReadResponse read(int maxFrameSize)
        throws IOException;
    GenericReadRequestMessage getRequest();
}
