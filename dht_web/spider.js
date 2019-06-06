'use strict';

const Spider = require('dhtspider');

const bootstraps = [{
    address: 'router.bittorrent.com',
    port: 6881
}, {
    address: 'dht.transmissionbt.com',
    port: 6881
},{
    address: 'router.utorrent.com',
    port: 6881
},{
    address: 'router.bitcomet.com',
    port: 6881
},{
    address: 'dht.aelitis.com',
    port: 6881
},{
    address: 'router.bittorrent.com',
    port: 6881
}];

const spider = new Spider({'bootstraps': bootstraps});

spider.on('ensureHash', (hash, addr) => console.log(`[hash]${hash.toLowerCase()}`));

spider.listen(parseInt(process.env.SPIDER_PORT) || 6339);
