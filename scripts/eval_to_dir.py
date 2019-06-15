#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys
import json
import requests

file = sys.argv[1]
urlEval = sys.argv[2]
outFile = sys.argv[3]

buf = []

skipped = 0
allow_process = False

try:
    with open('last', 'r') as f:
        last = f.read()
except:
    last = ''

print('Last processed: ' + last)

if (last == ''):
    allow_process = True
else:
    allow_process = False

print('Allow process ' + str(allow_process))


def process_line(line):
    index = line.find('hash="')

    if index != -1:
        after_hash = line[index + 6:]
        hash = after_hash[:40].lower()
        global last
        global allow_process
        global skipped

        if (allow_process == False):
            if (hash == last):
                allow_process = True
                print('Skipped ' + str(skipped + 1))
            skipped += 1
            return

        print(hash)
        buf.append(hash)
        if (len(buf) == 32):
            # eval torrent from dht
            r1 = requests.post(urlEval, json=buf)
            torrentsData = r1.json()  # json array of full torrents
            print("Evaluated: " + str(len(torrentsData)))
            print(torrentsData)
            with open(outFile, 'a+') as out:
                for t in torrentsData:
                    out.write(str(json.dumps(t, ensure_ascii=False)) + os.linesep)

            buf.clear()
            if len(torrentsData) > 0:
                with open('last', 'w') as f:
                    f.write(torrentsData[-1]['infoHash'].lower())


with open(file) as infile:
    for line in infile:
        process_line(line)
