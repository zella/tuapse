'use strict';

const Room = require('ipfs-pubsub-room');
const IPFS = require('ipfs');
const Readline = require('readline');

var room;

const rl = Readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: false
});

//TODO zlib compression.
//TODO sign messages

rl.on('line', function (line) {
    if (!room) {
        console.error("[Err]Room not initialized. Exiting...");
        process.exit(-1);
    }

    if (line === "[getPeers]") {
        console.log("[Peers]" + JSON.stringify(room.getPeers()));
    }
    if (line.startsWith("[searchAsk]")) {
        const msg = JSON.parse(line.substr("[searchAsk]".length));
        const peer = msg["peerId"];
        //TODO search
        room.sendTo(peer, "TODO")
    }
    if (line.startsWith("[searchAnswer]")) {
        const msg = JSON.parse(line.substr("[searchAsk]".length));
        const peer = msg["peerId"];
        //TODO search
        room.sendTo(peer, "TODO")
    }
});

const ipfs = new IPFS({
    EXPERIMENTAL: {
        pubsub: true
    },
    config: {
        Addresses: {
            Swarm: [
                '/dns4/ws-star.discovery.libp2p.io/tcp/443/wss/p2p-websocket-star'
            ]
        }
    }
});


// IPFS node is ready, so we can start using ipfs-pubsub-room
ipfs.on('ready', () => {

    ipfs.id((err, info) => console.log("[MyPeer]" + info.id));

    room = Room(ipfs, process.env.IPFS_ROOM);

    room.on('peer joined', (peer) => {
        console.log('[PeerJoined]' + peer)
    });

    room.on('peer left', (peer) => {
        console.log('[PeerLeft]' + peer)
    });

    // now started to listen to room
    room.on('subscribed', () => {
        console.log('[Connected]')
    });

    room.on('message', (message) => {
        const peerId = message.from;
        const data = message.data.toString();
        //TODO
        console.log()
    })
});
