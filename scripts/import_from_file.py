import sys
import requests
import json

file = sys.argv[1]
urlImport = sys.argv[2]


def process_line(line):
    l = line.rstrip()
    torrent = json.loads(l)
    arr = [torrent]
    # jsonArr = json.dumps(arr)
    # jsonn = json.loads(jsonArr)
    r = requests.post(urlImport, json=arr)
    imported = r.json()
    print(imported)


with open(file) as infile:
    for line in infile:
        process_line(line)
