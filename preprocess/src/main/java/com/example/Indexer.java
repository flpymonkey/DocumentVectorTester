package com.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

// There exists a class in Luceane called document, this is used for indexing and sorting elements of application
// Broken up into fields for ability to search

public class Indexer {

    public static void main(String indexDir, String dataDir) throws Exception{ // take hardcoded Strings instad of dir
        // write your code here
//        if (args.length !=2 )
//        {
//            throw new Exception("Usage Java "+ Indexer.class.getName()+"<index dir> <data dir>");
//        }

        // indexDir to get all files of similar directory
//        String indexDir = args[0]; //#1 Create Lucene index in this directory
//        String dataDir = args[1]; //#2 Index *.txt files in this directory

        long start = System.currentTimeMillis();
        Indexer indexer = new Indexer(indexDir);
        int numIndexed = indexer.index(dataDir);
        indexer.close();
        long end = System.currentTimeMillis();
        System.out.println("Indexing "+numIndexed+" files took "+(end-start)+" milliseconds.");
        System.out.println("Index dir = "+indexDir+":: data dir = "+dataDir);
    }

    private IndexWriter writer;

    public Indexer (String indexDir) throws IOException {
        File pathToIndex = new File(indexDir);
        Directory dir = FSDirectory.open(pathToIndex); //TODO -------------------------------- can hardcode parameters?
        //writer = new IndexWriter(dir, new StandardAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);

        //StandardAnalyzer class in luceneane -tokens input and allows to remove specific words, tonkeniezer
        //Done by separate class for stop word removal and stemming

        Analyzer analyzer = new StandardAnalyzer(StandardAnalyzer.STOP_WORDS_SET);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LATEST, analyzer);//add latest version to correct code for lucene
        writer = new IndexWriter(dir, indexWriterConfig); //#3 Create Lucene IndexWriter
        //LuceneSimilarityModified to specify similarity algorithm to use (cosine for our application)
        //For indexing set similarity algorithm by default or manually

    }
    public void close() throws IOException {
        writer.close(); //#4 Close IndexWriter
    }

    public int index(String dataDir) throws IOException {
        File[] files = new File(dataDir).listFiles();

        if ( files == null )
        {
            System.err.println("Error!! Segfault:: files: " + files);
            System.exit(1);
        }
        else
        {
            System.out.println("No. files to be indexed = "+files.length);
        }

        for ( int i=0; i<files.length; i++ )
        {
            File f = files[i];
            if ( !f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && acceptFile(f) )
            {
                indexFile(f);
            }

        }
        return writer.numDocs(); //#5 Return number of documents indexed
    }

    protected boolean acceptFile(File f) //#6 Index .txt files only
    {
        return f.getName().endsWith(".txt");
    }

    protected Document getDocument(File f) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("contents", new FileReader(f)));//#7 Index file content
        doc.add(new TextField("filename", f.getCanonicalPath(), Field.Store.YES));//#8 Index file path
        return doc;
    }

    private void indexFile(File f) throws IOException {
        System.out.println("Indexing "+f.getCanonicalPath());
        Document doc = getDocument(f);
        if ( doc != null )
        {
            writer.addDocument(doc);  //#9 Add document to Lucene index
        }
    }
}
