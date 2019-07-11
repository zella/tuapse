package org.zella.tuapse.search.filter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.filter.FilteredTFile;
import org.zella.tuapse.model.filter.TFileWithMeta;
import org.zella.tuapse.model.torrent.TFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseLuceneFilter {

    private static final Logger logger = LoggerFactory.getLogger(BaseLuceneFilter.class);

    protected abstract Document searchableDocument(TFile file);

    protected abstract Query query(Analyzer indexAnalyzer);

    protected abstract int count();

    protected abstract Analyzer analyzer();

    public List<FilteredTFile> selectFiles(List<TFileWithMeta> files) {
        logger.trace("Start filtering");
        Map<String, TFileWithMeta> byHashIndex = files.stream().collect(Collectors.toMap(t -> t.hash + "_" + String.valueOf(t.file.index), t -> t));
        Directory memoryDir = null;
        try {
            memoryDir = new ByteBuffersDirectory();

            Analyzer analyzer = analyzer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            IndexWriter writter = new IndexWriter(memoryDir, indexWriterConfig);
            files.forEach(file -> {
                Document document = searchableDocument(file.file);
                document.add(new StringField("hash_index", file.hash + "_" + String.valueOf(file.file.index), Field.Store.YES));
                try {
                    writter.addDocument(document);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            writter.commit();
            writter.close();

            IndexReader reader = DirectoryReader.open(memoryDir);
            IndexSearcher searcher = new IndexSearcher(reader);

            TopDocs docs = searcher.search(query(analyzer), count());
            ScoreDoc[] hits = docs.scoreDocs;

            logger.trace("Found " + hits.length);

            List<FilteredTFile> out = new ArrayList<>();

            for (ScoreDoc hit : hits) {
                var hashIndex = (searcher.doc(hit.doc).get("hash_index"));
                out.add(new FilteredTFile(byHashIndex.get(hashIndex), hit.score));
            }
            logger.trace("End filtering");
            return postFilter(out);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                memoryDir.close();
            } catch (IOException e) {
                logger.error("Can't close memory index", e);
            }
        }
    }

    protected abstract List<FilteredTFile> postFilter(List<FilteredTFile> files);
}
