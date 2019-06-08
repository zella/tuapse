import sys
import requests
import time

file = sys.argv[1]
url = sys.argv[2]


def process_line(line):
    index = line.find('hash="')
    if index != -1:
        after_hash = line[index + 6:]
        hash = after_hash[:40]
        print(hash)
        r = requests.post(url, json=[hash])
        print(r.text)
        time.sleep(0.1)


with open(file) as infile:
    for line in infile:
        process_line(line)
