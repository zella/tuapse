const WebTorrent = require('webtorrent');
const Rimraf = require("rimraf");
const client = new WebTorrent();
const Os = require('os');

const torrentId = process.argv[2];

client.add(torrentId, function (torrent) {

    const obj = {
        "infoHash": torrent.infoHash,
        "name": torrent.name,
        "files": torrent.files.map(function (f) {
            return {
                "name": f.name,
                "length": f.length,
                "path": f.path
            };
        })
    };

    torrent.destroy();

    Rimraf.sync("/tmp/webtorrent/" + torrent.infoHash);

    process.stdout.write("[torrent]" + JSON.stringify(obj) + Os.EOL, () => process.exit());

});



