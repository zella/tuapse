package org.zella.tuapse.search.filter.impl;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.zella.tuapse.search.filter.BaseLuceneFilter;
import org.zella.tuapse.model.torrent.TFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Ищет файлы по полным путям файлов торрента. Т.е полный музыкальный альбом не вытащит
 */
public class FilesOnlyLuceneFilter extends BaseLuceneFilter {

    private final String text;
    private final Optional<List<String>> ext;
    private final int n;

    private final Analyzer analyzer = new StandardAnalyzer();


    private static final String DELIMTER_SPACE = " ";

    public FilesOnlyLuceneFilter(String text, Optional<List<String>> ext, int n) {
        this.text = text;
        this.ext = ext;
        this.n = n;

    }

    @Override
    protected Document searchableDocument(TFile file) {
        var paths = file.path.split("/");
        var name = paths[paths.length - 1];
        var nameNoExt = FilenameUtils.removeExtension(name);
        var ext = FilenameUtils.getExtension(name).toLowerCase();

        var parentsList = Arrays.asList(Arrays.copyOf(paths, paths.length - 1));
        var parents = String.join(DELIMTER_SPACE, parentsList);

        var pathsNoExtList = new ArrayList<>(parentsList);
        pathsNoExtList.add(nameNoExt);

        var pathsNoExt = String.join(DELIMTER_SPACE, pathsNoExtList);

        Document document = new Document();
        document.add(new TextField("name", name, Field.Store.NO));
        document.add(new TextField("nameNoExt", nameNoExt, Field.Store.NO));
        document.add(new StringField("ext", ext, Field.Store.NO));
        document.add(new TextField("parents", parents, Field.Store.NO));
        document.add(new TextField("pathsNoExt", pathsNoExt, Field.Store.NO));
        return document;
    }

    @Override
    protected Query query(Analyzer analyzer) {
        QueryParser parser = new QueryParser("pathsNoExt", analyzer);
        Query textQuery;
        try {
            textQuery = parser.parse(text);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if (!ext.isPresent()) {
            return textQuery;
        } else {
            var extQueryB = new BooleanQuery.Builder();
            ext.get().forEach(e -> extQueryB.add(new BooleanClause(new TermQuery(new Term("ext", e.toLowerCase())), BooleanClause.Occur.SHOULD)));
            var extQuery = extQueryB.build();

            return new BooleanQuery.Builder()
                    .add(new BooleanClause(textQuery, BooleanClause.Occur.MUST))
                    .add(new BooleanClause(extQuery, BooleanClause.Occur.FILTER))
                    .build();
        }
    }

    @Override
    protected int count() {
        return n;
    }

    @Override
    protected Analyzer analyzer() {
        return analyzer;
    }

}
