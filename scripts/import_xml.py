import sys
import requests
import json

file = sys.argv[1]
urlEval = sys.argv[2]
urlImport = sys.argv[2]

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
            r1 = requests.post(urlEval, json=buf)
            torrentsData = r1.json()
            print("Evaluated: " + len(torrentsData))
            r2 = requests.post(urlImport, json=torrentsData)
            imported = r2.json(())
            buf.clear()
            if len(imported) > 0:
                with open('last', 'w') as f:
                    f.write(imported[-1])


with open(file) as infile:
    for line in infile:
        process_line(line)
