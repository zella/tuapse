const GenTorrentFile = require('parse-torrent');
const WebTorrent = require('webtorrent');
const Rimraf = require("rimraf");
const client = new WebTorrent();

const torrentId = process.argv[2];

client.add(torrentId, function (torrent) {

    torrent.destroy();
    //TODO another dir
    Rimraf.sync("/tmp/webtorrent/" + torrent.infoHash);

    const buf = GenTorrentFile.toTorrentFile(torrent);

    process.stdout.write("[torrentFile]" + buf.toString('base64'), () => process.exit());

});
