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
package org.dcache.xrootd2.pool;

import java.io.RandomAccessFile;

import diskCacheV111.util.PnfsId;
import diskCacheV111.vehicles.ProtocolInfo;
import diskCacheV111.vehicles.StorageInfo;
import org.dcache.pool.movers.IoMode;
import org.dcache.pool.repository.Allocator;
import org.dcache.pool.repository.FileRepositoryChannel;

import dmg.cells.nucleus.CellEndpoint;

/**
 * Xrootd mover for 1.9.12.
 */
public class XrootdProtocol_3 extends org.dcache.xrootd.pool.XrootdProtocol_3
{
    public XrootdProtocol_3(CellEndpoint endpoint) throws Exception
    {
        super(endpoint);
    }

    public void runIO(RandomAccessFile file,
                      ProtocolInfo protocol,
                      StorageInfo storage,
                      PnfsId pnfsId,
                      Allocator allocator,
                      IoMode access)
        throws Exception
    {
        runIO(new FileRepositoryChannel(file), protocol,
              storage, pnfsId, allocator, access);
    }
}
