const WebTorrent = require('webtorrent');
const Rimraf = require("rimraf");
const Os = require('os');
const path = require('path');

const client = new WebTorrent();

const dir = process.env.WEBTOR_SPIDER_DIR || "/tmp/webtorrent_spider/";

const torrentId = process.argv[2];

client.add(torrentId, {path: dir}, function (torrent) {

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

    process.stdout.write("[torrent]" + JSON.stringify(obj) + Os.EOL, () => process.exit());

});



