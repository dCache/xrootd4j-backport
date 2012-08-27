xrootd4j-backport
=================

A dCache plugin backporting the latest xrootd4j door and xrootd4j
mover to dCache 1.9.12 and dCache 2.2. 

By using this plugin you get access to the same xrootd implementation
that is expected to be part of dCache 2.4. xrootd4j authentication,
xrootd4j authorization and xrootd4j channel handlers plugins can be
combined with xrootd4j-backport.

The xrootd4j-backport plugins includes the GSI authentication and the
Alice token authorization plugins.

To install the plugin, unpack the tarball in
/usr/local/share/dcache/plugins/ followed by restarting dCache:

    mkdir -p /usr/local/share/dcache/plugins/
    cd /usr/local/share/dcache/plugins/
    tar xzf /tmp/xrootd4j-backport-2.4.0.tar.gz 
    dcache restart

The plugin should be installed in both the xrootd door and on all
pools that serve xrootd data.

Note that due to the overlap between the xrootd and HTTP
implementations in dCache, this plugin also provides a slightly
updated version of the HTTP mover.