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

import javax.security.auth.Subject;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.dcache.vehicles.XrootdProtocolInfo;
import org.dcache.util.RedirectedTransfer;
import diskCacheV111.util.PnfsHandler;
import diskCacheV111.util.FsPath;
import diskCacheV111.vehicles.ProtocolInfo;

import dmg.cells.nucleus.CellPath;

public class XrootdTransfer extends RedirectedTransfer<InetSocketAddress>
{
    private UUID _uuid;
    private InetSocketAddress _doorAddress;
    private int _fileHandle;
    private boolean _uuidSupported;

    public XrootdTransfer(PnfsHandler pnfs, Subject subject, FsPath path) {
        super(pnfs, subject, path);
    }

    public synchronized void setFileHandle(int fileHandle) {
        _fileHandle = fileHandle;
    }

    public synchronized int getFileHandle() {
        return _fileHandle;
    }

    public synchronized void setUUID(UUID uuid) {
        _uuid = uuid;
    }

    public synchronized void setDoorAddress(InetSocketAddress doorAddress) {
        _doorAddress = doorAddress;
    }

    public synchronized void setUUIDSupported(boolean uuidSupported) {
        _uuidSupported = uuidSupported;
    }

    public boolean isUUIDSupported() {
        return _uuidSupported;
    }

    protected synchronized ProtocolInfo createProtocolInfo() {
        InetSocketAddress client = getClientAddress();
        XrootdProtocolInfo protocolInfo =
            new XrootdProtocolInfo(XrootdDoor.XROOTD_PROTOCOL_STRING,
                                   XrootdDoor.XROOTD_PROTOCOL_MAJOR_VERSION,
                                   XrootdDoor.XROOTD_PROTOCOL_MINOR_VERSION,
                                   client,
                                   new CellPath(getCellName(), getDomainName()),
                                   getPnfsId(),
                                   _fileHandle,
                                   _uuid,
                                   _doorAddress);
        protocolInfo.setPath(_path.toString());
        return protocolInfo;
    }

    @Override
    protected ProtocolInfo getProtocolInfoForPoolManager() {
        return createProtocolInfo();
    }

    @Override
    protected ProtocolInfo getProtocolInfoForPool() {
        return createProtocolInfo();
    }
}
