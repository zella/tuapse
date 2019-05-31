package org.zella.tuapse.storage.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.elasticsearch.common.collect.Tuple;
import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.es.IndexMeta;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.Torrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.storage.Index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LuceneIndex implements Index {

    private final Analyzer analyzer = new SimpleAnalyzer();

    static class FileMeta {
        public long i;
        public long length;

        FileMeta(long i, long length) {
            this.i = i;
            this.length = length;
        }

        public FileMeta() {
        }
    }

    @Override
    public synchronized String insertTorrent(Torrent t) {
        try {
            //TODO seems like IndexWriter thread safe, but cannot share single directory
            Directory storageDir = FSDirectory.open((Paths.get("TODO")));//TODO env
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            IndexWriter writter = new IndexWriter(storageDir, indexWriterConfig);

            var doc = new Document();
            doc.add(new StringField("infoHash", t.name, Field.Store.YES));
            doc.add(new TextField("name", t.name, Field.Store.YES));
            t.files.forEach(f -> doc.add(new TextField("file", f.index + " " + f.path, Field.Store.YES)));
            doc.add(new StringField("files",
                    Json.mapper.writeValueAsString(
                            t.files.stream().map(f -> new FileMeta(f.index, f.length)).collect(Collectors.toList())
                    ), Field.Store.YES));

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
            Directory storageDir = FSDirectory.open((Paths.get("TODO")));//TODO env
            var dir = ((FSDirectory) storageDir).getDirectory();
            Files.createDirectories(dir);
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            IndexWriter writter = new IndexWriter(storageDir, indexWriterConfig);
            writter.commit();
            writter.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //TODO
    private String[] getHighlightedField(Query query, Analyzer analyzer, String fieldName, String fieldValue) throws IOException, InvalidTokenOffsetsException {
        Formatter formatter = new SimpleHTMLFormatter();
        QueryScorer queryScorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, queryScorer);
        highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, Integer.MAX_VALUE));
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
        return highlighter.getBestFragments(this.analyzer, fieldName, fieldValue, Integer.MAX_VALUE);
    }

    @Override
    public synchronized List<FoundTorrent> search(String what) {
        try {
            //TODO cant share directory!
            Directory storageDir = FSDirectory.open((Paths.get("TODO")));
            IndexReader reader = DirectoryReader.open(storageDir);
            IndexSearcher searcher = new IndexSearcher(reader);

            var both = new BooleanQuery.Builder();
            both.add(new BooleanClause(new TermQuery(new Term("file", what.toLowerCase())), BooleanClause.Occur.SHOULD));
            both.add(new BooleanClause(new TermQuery(new Term("name", what.toLowerCase())), BooleanClause.Occur.SHOULD));

            TopDocs docs = searcher.search(both.build(), 10);//TODO env
            var result = new ArrayList<FoundTorrent>();
            for (ScoreDoc hit : docs.scoreDocs) {
                var doc = searcher.doc(hit.doc);
                var infoHash = doc.get("infoHash");
                var name = doc.get("name");

                Map<Integer, String> filesPath = Arrays.asList(doc.getValues("file")).stream()
                        .map(p -> {
                                    var ind = Integer.parseInt(StringUtils.substringBefore(p, " "));
                                    var path = StringUtils.substringAfter(p, " ");
                                    return Tuple.tuple(ind, path);
                                }
                        )
                        .collect(Collectors.toMap(Tuple::v1, Tuple::v2));

                List<FileMeta> filesMeta = Json.mapper.readValue(doc.get("files"), new TypeReference<List<FileMeta>>() {
                });

                var files = filesMeta.stream().map(m -> TFile.create(Math.toIntExact(m.i), filesPath.get(m.i), m.length))
                        .collect(Collectors.toList());

                var torrent = Torrent.create(name, infoHash, files);

                result.add(FoundTorrent.create(torrent, List.of("TODO"), hit.score));

            }

            return result;


        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
