const Readline = require('readline');
const WebTorrent = require('webtorrent');
const Rimraf = require("rimraf");
const Os = require('os');
const path = require('path');

const client = new WebTorrent();

const dir = process.env.WEBTOR_SPIDER_DIR || "/tmp/webtorrent_spider/";

client.on('error', function (err) {
    process.exit(-1)
});

const rl = Readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: false
});


function makeStdout(torrent) {
    const obj = {
        "infoHash": torrent.infoHash,
        "numPeers": torrent.numPeers,
        "name": torrent.name,
        "files": torrent.files.map(function (f, index) {
            return {
                "index": index,
                "length": f.length,
                "path": f.path
            };
        })
    };

    return "[torrent]" + JSON.stringify(obj) + Os.EOL
}


function removeTorrent(hash) {
    try {
        client.remove(hash)
    } catch (e) {
    }
}

function addTorrent(hash) {
    client.add(hash, {path: dir}, function (torrent) {
        torrent.deselect(0, torrent.pieces.length - 1, false);

        const stdout = makeStdout(torrent);

        Rimraf.sync(path.join(dir, torrent.infoHash));

        process.stdout.write(stdout);
    });
    setTimeout(function () {
        try {
            client.remove(hash, function callback(err) {
            })
        }
        catch (e) {
        }
    }, 300000); //5 min
}

rl.on('line', function (hash) {
    removeTorrent(hash);
    addTorrent(hash);
});





