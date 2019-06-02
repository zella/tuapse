package org.zella.tuapse.storage.impl;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.elasticsearch.common.collect.Tuple;
import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.es.IndexMeta;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.Torrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.storage.Index;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class LuceneIndex implements Index {

    private final Path dir;

    private final Analyzer analyzer = new SimpleAnalyzer();

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

//                Map<Integer, String> filesPath =
//                        doc.getFields().stream()
//                                .filter(f -> f.name().startsWith("f_"))
//                                .map(f -> Tuple.tuple(Integer.parseInt(StringUtils.substringAfter(f.name(), "f_")), f.stringValue()))
//                                .collect(Collectors.toMap(Tuple::v1, Tuple::v2));


                var sortedPaths = Arrays.asList(doc.getValues("file")).stream()
                        .sorted(Comparator.comparing(f -> f)).collect(Collectors.toList());

                Map<Integer, String> filesPath = new HashMap<>();
                for (int i = 0; i < sortedPaths.size(); i++) {
                    var path = sortedPaths.get(i);
                    filesPath.put(i, path);
                }

//                Map<Integer, String> filesPath = t.files.sort(Comparator.comparing(f -> f.path))
//                        .map(p -> {
//                                    var ind = Integer.parseInt(StringUtils.substringBefore(p, " "));
//                                    var path = StringUtils.substringAfter(p, " ");
//                                    return Tuple.tuple(ind, path);
//                                }
//                        )
//                        .collect(Collectors.toMap(Tuple::v1, Tuple::v2));


//                var expHighlighter = new UnifiedHighlighter(searcher, analyzer);
//                var test = expHighlighter.highlight("file", query, docs);
//                var test2 = expHighlighter.highlight("file", query, docs);
//
//                var fq = new FieldQuery(query, reader, true, false);
//                var fh = new FastVectorHighlighter(true, false);
//                var fff = fh.getBestFragments(fq,reader,hit.doc,"file", 7777, 7777);
                //Create token stream
//                TokenStream stream = TokenSources.getAnyTokenStream(reader, hit.doc, "file", analyzer);
//                TokenStream stream  =   TokenSources.getTokenStream( "file", null, what, analyzer, -1 );
//                //Get highlighted text fragments
//                String[] frags = highlighter.getBestFragments(analyzer, "file",  what, 8);

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

    @Override
    public Boolean isSpaceAllowed() {
        return null;
    }

    @Override
    public IndexMeta indexMeta() {
        return null;
    }
}
