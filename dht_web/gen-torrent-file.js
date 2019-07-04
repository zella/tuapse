const GenTorrentFile = require('parse-torrent');
const WebTorrent = require('webtorrent');
const Rimraf = require("rimraf");
const path = require('path');

const client = new WebTorrent();

const dir = process.env.WEBTOR_GEN_DIR || "/tmp/webtorrent_gen/";

const torrentId = process.argv[2];

client.add(torrentId, {path: dir}, function (torrent) {

    torrent.deselect(0, torrent.pieces.length - 1, false);

    Rimraf.sync(path.join(dir, torrent.infoHash));

    const buf = GenTorrentFile.toTorrentFile(torrent);

    torrent.destroy();

    process.stdout.write("[torrentFile]" + buf.toString('base64'), () => process.exit());

});
