package org.zella.tuapse.storage.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.elasticsearch.client.Request;
import org.elasticsearch.common.collect.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.es.IndexMeta;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.Torrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.providers.Utils;
import org.zella.tuapse.storage.Index;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LuceneIndex implements Index {


    private static final Logger logger = LoggerFactory.getLogger(LuceneIndex.class);


    public LuceneIndex(Path dir) {
        this.dir = dir;
    }

    static class FileMeta {
        public int i;
        public int index;
        public long length;

        public FileMeta(int i, int index, long length) {
            this.i = i;
            this.index = index;
            this.length = length;
        }

        public FileMeta() {
        }
    }

    private static final String KEY_META = "KEY_META";

    private final Path dir;

    private final Analyzer analyzer = new SimpleAnalyzer();


    private final LoadingCache<String, IndexMeta> indexMetaCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public IndexMeta load(String key) throws Exception {
                    logger.info("Request index meta...");
                    return LuceneIndex.this.indexMetaInternal();
                }
            });


    @Override
    public synchronized String insertTorrent(Torrent t) {
        try {
            //TODO seems like IndexWriter thread safe, but cannot share single directory
            Directory storageDir = FSDirectory.open(dir);
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            IndexWriter writter = new IndexWriter(storageDir, indexWriterConfig);

            var doc = new Document();
            doc.add(new StringField("infoHash", t.infoHash, Field.Store.YES));
            doc.add(new TextField("name", t.name, Field.Store.YES));

            for (TFile f : t.files) {
                doc.add(new TextField("file", f.path, Field.Store.YES));
            }

            //sort by path for evaluation in query
            var files = new ArrayList<>(t.files);
            files.sort(Comparator.comparing(f -> f.path));

            var metas = new ArrayList<FileMeta>();
            for (int i = 0; i < files.size(); i++) {
                var f = files.get(i);
                metas.add(new FileMeta(i, f.index, f.length));
            }

            doc.add(new StringField("files", Json.mapper.writeValueAsString(metas), Field.Store.YES));

            writter.addDocument(doc);
            writter.commit();
            writter.close();
            return t.infoHash;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public void createIndexIfNotExist() {
        try {
            Files.createDirectories(dir);
            Directory storageDir = FSDirectory.open(dir);

            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            IndexWriter writter = new IndexWriter(storageDir, indexWriterConfig);
            writter.commit();
            writter.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Nullable
    private String getHighlightedField(Query query, String fieldName, String fieldValue) {
        Formatter formatter = new SimpleHTMLFormatter();
        QueryScorer queryScorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, queryScorer);
        highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, Integer.MAX_VALUE));
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
        try {
            return highlighter.getBestFragment(this.analyzer, fieldName, fieldValue);
        } catch (IOException | InvalidTokenOffsetsException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized List<FoundTorrent> search(String what) {
        try {
            //TODO cant share directory!
            Directory storageDir = FSDirectory.open(dir);
            IndexReader reader = DirectoryReader.open(storageDir);
            IndexSearcher searcher = new IndexSearcher(reader);

            var parserName = new QueryParser("name", analyzer);
            var parserFile = new QueryParser("file", analyzer);

            var both = new BooleanQuery.Builder();
            both.add(new BooleanClause(parserName.parse(what.toLowerCase()), BooleanClause.Occur.SHOULD));
            both.add(new BooleanClause(parserFile.parse(what.toLowerCase()), BooleanClause.Occur.SHOULD));

            var query = both.build();

            TopDocs docs = searcher.search(both.build(), 10);//TODO env
            var result = new ArrayList<FoundTorrent>();
            for (ScoreDoc hit : docs.scoreDocs) {
                var doc = searcher.doc(hit.doc);
                var infoHash = doc.get("infoHash");
                var name = doc.get("name");

                var sortedPaths = Arrays.asList(doc.getValues("file")).stream()
                        .sorted(Comparator.comparing(f -> f)).collect(Collectors.toList());

                Map<Integer, String> filesPath = new HashMap<>();
                for (int i = 0; i < sortedPaths.size(); i++) {
                    var path = sortedPaths.get(i);
                    filesPath.put(i, path);
                }

                var highlights = new ArrayList<String>();
                filesPath.forEach((i, path) -> {
                    var highlight = getHighlightedField(query, "file", path);
                    if (highlight != null)
                        highlights.add(highlight);
                });

                List<FileMeta> filesMeta = Json.mapper.readValue(doc.get("files"), new TypeReference<List<FileMeta>>() {
                });

                var files = filesMeta.stream().map(m -> TFile.create(Math.toIntExact(m.index), filesPath.get(m.i), m.length))
                        .collect(Collectors.toList());

                var torrent = Torrent.create(name, infoHash, files);

                result.add(FoundTorrent.create(torrent, highlights, hit.score));

            }

            return result;


        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean isSpaceAllowed() {
        var sizeGb = indexMetaCache.getUnchecked(KEY_META).indexSize / 1024d / 1024d / 1024d;
        logger.info("Index gb: " + new DecimalFormat("#.######").format(sizeGb));
        return (sizeGb < MaxIndexSizeGb);
    }

    @Override
    public IndexMeta indexMeta() {
        return indexMetaCache.getUnchecked(KEY_META);
    }

    private synchronized IndexMeta indexMetaInternal() throws IOException {
        var size = Utils.size(dir);
        Directory storageDir = FSDirectory.open(dir);
        IndexReader reader = DirectoryReader.open(storageDir);
        var count = reader.maxDoc();
        return new IndexMeta(size, count);
    }

}
