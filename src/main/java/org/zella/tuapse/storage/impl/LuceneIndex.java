package org.zella.tuapse.storage.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.index.Highlight;
import org.zella.tuapse.model.index.IndexMeta;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.providers.Utils;
import org.zella.tuapse.search.SearchMode;
import org.zella.tuapse.storage.AbstractIndex;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LuceneIndex extends AbstractIndex {

    private static final String FIELD_INFO_HASH = "infoHash";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_FILE = "file";
    private static final String FIELD_FILES = "files";

    public LuceneIndex(Path dir) {
        this.dir = dir;
    }

    @Override
    protected IndexMeta indexMetaInternal() throws IOException {
        var size = Utils.size(dir);
        Directory storageDir = FSDirectory.open(dir);
        IndexReader reader = DirectoryReader.open(storageDir);
        var count = reader.maxDoc();
        return new IndexMeta(size, count);
    }


    private final Path dir;

    private final Analyzer analyzer = new SimpleAnalyzer();


    private String insertTorrentNoCommit(IndexWriter writter, StorableTorrent t) throws IOException {
        var doc = new Document();

        doc.add(new StringField(FIELD_INFO_HASH, t.infoHash, Field.Store.YES));
        doc.add(new TextField(FIELD_NAME, t.name, Field.Store.YES));

        for (TFile f : t.files) {
            doc.add(new TextField(FIELD_FILE, f.path, Field.Store.YES));
        }

        //sort by path for evaluation in query
        var files = new ArrayList<>(t.files);
        files.sort(Comparator.comparing(f -> f.path));

        var metas = new ArrayList<FileMeta>();
        for (int i = 0; i < files.size(); i++) {
            var f = files.get(i);
            metas.add(new FileMeta(i, f.index, f.length));
        }

        doc.add(new StoredField(FIELD_FILES, Json.mapper.writeValueAsString(metas)));

        writter.updateDocument(new Term(FIELD_INFO_HASH, t.infoHash), doc);
        return t.infoHash;
    }

    @Override
    public synchronized String insertTorrent(StorableTorrent t) {
        try {
            //TODO seems like IndexWriter thread safe, but cannot share single directory
            Directory storageDir = FSDirectory.open(dir);
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            IndexWriter writter = new IndexWriter(storageDir, indexWriterConfig);

            insertTorrentNoCommit(writter, t);

            writter.flush();
            writter.close();
            return t.infoHash;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public void insertTorrents(List<StorableTorrent> torrents) {
        try {
            //TODO seems like IndexWriter thread safe, but cannot share single directory
            Directory storageDir = FSDirectory.open(dir);
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            IndexWriter writter = new IndexWriter(storageDir, indexWriterConfig);

            for (StorableTorrent t : torrents) {
                insertTorrentNoCommit(writter, t);
            }

            writter.flush();
            writter.close();
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
    public synchronized List<FoundTorrent<StorableTorrent>> search(String what, SearchMode mode, int page, int pageSize) {
        if (page < 1)
            page = 1;
        try {
            Directory storageDir = FSDirectory.open(dir);
            IndexReader reader = DirectoryReader.open(storageDir);
            IndexSearcher searcher = new IndexSearcher(reader);

            final Query query;

            switch (mode) {
                case NAMES:
                    var parserName_ = new QueryParser(FIELD_NAME, analyzer);
                    query = parserName_.parse(what.toLowerCase());
                    break;
                case FILES:
                    var parserFile_ = new QueryParser(FIELD_FILE, analyzer);
                    query = parserFile_.parse(what.toLowerCase());
                    break;
                case FILES_AND_NAMES:
                    var parserName = new QueryParser(FIELD_NAME, analyzer);
                    var parserFile = new QueryParser(FIELD_FILE, analyzer);
                    var both = new BooleanQuery.Builder();
                    both.add(new BooleanClause(parserName.parse(what.toLowerCase()), BooleanClause.Occur.SHOULD));
                    both.add(new BooleanClause(parserFile.parse(what.toLowerCase()), BooleanClause.Occur.SHOULD));
                    query = both.build();
                    break;
                default:
                    query = null;
            }

            Objects.requireNonNull(query);
            //TODO what is it
            TopScoreDocCollector collector = TopScoreDocCollector.create(1000, Integer.MAX_VALUE);
            int startIndex = (page - 1) * pageSize;

            searcher.search(query, collector);

            TopDocs docs = collector.topDocs(startIndex, pageSize);

            var result = new ArrayList<FoundTorrent<StorableTorrent>>();
            for (ScoreDoc hit : docs.scoreDocs) {
                var doc = searcher.doc(hit.doc);
                var infoHash = doc.get(FIELD_INFO_HASH).toLowerCase();
                var name = doc.get(FIELD_NAME);

                var sortedPaths = Arrays.stream(doc.getValues(FIELD_FILE))
                        .sorted(Comparator.comparing(f -> f)).collect(Collectors.toList());

                //<Sorting position, Path>
                Map<Integer, String> filesPath = new HashMap<>();
                for (int i = 0; i < sortedPaths.size(); i++) {
                    var path = sortedPaths.get(i);
                    filesPath.put(i, path);
                }

                var highlightsByPath = new HashMap<String, String>();
                filesPath.forEach((i, path) -> {
                    var highlight = getHighlightedField(query, FIELD_FILE, path);
                    if (highlight != null)
                        //change it if change Formatter
                        highlightsByPath.put(highlight.replace("<B>", "").replace("</B>", ""), highlight);
                });


                List<FileMeta> filesMeta = Json.mapper.readValue(doc.get(FIELD_FILES), new TypeReference<List<FileMeta>>() {
                });

                var files = filesMeta.stream().map(m -> TFile.create(Math.toIntExact(m.n), filesPath.get(m.i), m.l))
                        .collect(Collectors.toList());

                var highlights = new ArrayList<Highlight>();
                filesMeta.forEach(m -> {
                    String path = filesPath.get(m.i);
                    var highlight = highlightsByPath.get(path);
                    if (highlight != null)
                        highlights.add(Highlight.create(m.n, highlight, m.l));
                });

                var torrent = StorableTorrent.create(name, infoHash, files);


                result.add(FoundTorrent.create(torrent, highlights.stream().limit(highlightsLimit).collect(Collectors.toList()), hit.score));

            }

            return result;


        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    static class FileMeta {
        //sorting position
        public int i;
        //index in torrent
        public int n;
        public long l;

        public FileMeta(int sortingPosition, int index, long length) {
            this.i = sortingPosition;
            this.n = index;
            this.l = length;
        }

        public FileMeta() {
        }
    }

}
