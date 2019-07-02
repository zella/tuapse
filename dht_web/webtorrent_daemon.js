const Readline = require('readline');
const WebTorrent = require('webtorrent');
const Rimraf = require("rimraf");
const Os = require('os');
const path = require('path');

const client = new WebTorrent();

const dir = process.env.WEBTOR_SPIDER_DIR || "/tmp/webtorrent_spider/";

const rl = Readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: false
});

rl.on('line', function (hash) {
    client.add(hash, {path: dir}, function (torrent) {

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

        torrent.destroy();

        Rimraf.sync(path.join(dir, torrent.infoHash));

        process.stdout.write("[torrent]" + JSON.stringify(obj) + Os.EOL);

    });
});





