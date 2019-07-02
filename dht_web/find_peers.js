const DHT = require('bittorrent-dht');
const Readline = require('readline');
const Os = require('os');

const rl = Readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: false
});

const dht = new DHT();

rl.on('line', function (hash) {
    // find peers for the given torrent info hash
    dht.lookup(parsed.infoHash)
});

dht.listen( function () {
   //do nothing
    console.log('Dht listening ...')
});

dht.on('peer', function (peer, infoHash, from) {

    const obj = {
        "infoHash": infoHash.toString("hex"),
        "peer": peer,
        "from": from
    };


    process.stdout.write("[foundPeer]" + JSON.stringify(obj) + Os.EOL);

    // console.log('found potential peer ' + peer.host + ':' + peer.port + ' through ' + from.address + ':' + from.port)
});

// dht.lookup("9501161d97259b33a95d768d049ffc20591f1b0c");
