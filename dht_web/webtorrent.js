const WebTorrent = require('webtorrent')
const Rimraf = require("rimraf")
const client = new WebTorrent()
const fs = require("fs");
const path = require('path')
const dateFormat = require('dateformat');

const torrentId = process.argv[2];

const databaseDir = process.env.DATABASE_DIR || "torrents";
const webTorrentDir = process.env.WEBTORRENT_DIR || "/tmp/webtorrent"

if (!fs.existsSync(databaseDir)) {
  fs.mkdirSync(databaseDir)
}

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
  }

  torrent.destroy();

  Rimraf.sync(webTorrentDir + path.sep + torrent.infoHash);

  fs.appendFileSync(`${databaseDir}${path.sep}${dateFormat(new Date(), "yyyy_mm_dd_h:MM")}.txt`, JSON.stringify(obj), { flag: "a+" });

  process.exit();
});