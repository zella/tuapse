import sys
import requests
import json

file = sys.argv[1]
urlImport = sys.argv[2]

torrents_buff = []

def send_buff():
    r = requests.post(urlImport, json=torrents_buff)
    torrents_buff.clear()
    imported = r.json()
    for x in imported:
        print(x)

def process_line(line):
    l = line.rstrip()
    torrent = json.loads(l)
    torrents_buff.append(torrent)
    if len(torrents_buff) == 64:
        send_buff()


with open(file) as infile:
    for line in infile:
        process_line(line)
    send_buff()
    print('Done')
