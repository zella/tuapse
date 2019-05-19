'use strict'

const spider = new (require('dhtspider'))

spider.on('ensureHash', (hash, addr) => console.log(`[hash]${hash.toLowerCase()}`))

spider.listen(parseInt(process.env.SPIDER_PORT) || 6339)
