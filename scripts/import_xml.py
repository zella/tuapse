import sys
import requests
import json

file = sys.argv[1]
url = sys.argv[2]

buf = []

skipped = 0
allow_process = False

with open('last', 'w+') as f:
    try:
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
        hash = after_hash[:40]
        global last
        global allow_process
        global skipped

        if (allow_process == False):
            if (hash == last):
                allow_process = True
                print('Skipped ' + str(skipped))
            skipped += 1
            return

        print(hash)
        buf.append(hash)
        if (len(buf) == 8):
            r = requests.post(url, json=buf)
            print(r.text)
            buf.clear()

        with open('last', 'w') as f:
            f.write(hash)


with open(file) as infile:
    for line in infile:
        process_line(line)
