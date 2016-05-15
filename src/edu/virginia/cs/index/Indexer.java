package edu.virginia.cs.index;

import edu.virginia.cs.utility.SpecialAnalyzer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Indexer {

    private SAXParserFactory factory;
    private SAXParser saxParser;

    public Indexer() {
        try {
            factory = SAXParserFactory.newInstance();
            saxParser = factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException ex) {
            Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Start creating the lucene index.
     * 
     * @param indexPath
     * @param filename
     */
    public void createIndex(String indexPath, String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            fis.read();
            fis.read();
            CBZip2InputStream inputStream = new CBZip2InputStream(fis);
            DataHandler handler = new DataHandler(indexPath);
            saxParser.parse(inputStream, handler);
            handler.finish();
        } catch (SAXException | IOException ex) {
            Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws IOException {
        Indexer indexer = new Indexer();
        indexer.createIndex("lucene-AOL-index", "./data/AolCrawledData.xml.bz2");
    }
}

class DataHandler extends DefaultHandler {

    private IndexWriter writer;
    private final FieldType _contentFieldType;
    private final StringBuilder content;
    private String currentURL;
    private boolean isContent;
    private int pagesCompleted;

    public DataHandler(String indexPath) throws IOException {
        content = new StringBuilder();
        _contentFieldType = new FieldType();
        _contentFieldType.setIndexed(true);
        _contentFieldType.setStored(true);
        pagesCompleted = 0;

        setupIndex(indexPath);
    }

    public void finish() throws IOException {
        writer.close();
    }

    /**
     * Creates the initial index files on disk.
     *
     * @param indexPath
     * @return
     * @throws IOException
     */
    private void setupIndex(String indexPath) throws IOException {
        Analyzer analyzer = new SpecialAnalyzer();///special analyzer used here
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
                analyzer);
        config.setOpenMode(OpenMode.CREATE);
        config.setRAMBufferSizeMB(2048.0);

        FSDirectory dir = FSDirectory.open(new File(indexPath));
        writer = new IndexWriter(dir, config);
    }

    @Override
    public void startElement(String uri,
            String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase("crawledData")) {
            System.out.println("Parsing Started!!!");
        } else if (qName.equalsIgnoreCase("page")) {
            currentURL = attributes.getValue("url");
        } else if (qName.equalsIgnoreCase("content")) {
            isContent = true;
            content.setLength(0);
        }
    }

    @Override
    public void endElement(String uri,
            String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("crawledData")) {
            System.out.println("Parsing Completed!!!");
        } else if (qName.equalsIgnoreCase("page")) {
            try {
                Document doc = new Document();
                doc.add(new Field("content", content.toString(), _contentFieldType));
                doc.add(new Field("clicked_url", currentURL, _contentFieldType));
                writer.addDocument(doc);
                pagesCompleted++;
                if (pagesCompleted % 10000 == 0) {
                    System.out.println(pagesCompleted + " pages completed");
                }
            } catch (IOException ex) {
                Logger.getLogger(DataHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (qName.equalsIgnoreCase("content")) {
        }
    }

    @Override
    public void characters(char ch[],
            int start, int length) throws SAXException {
        if (isContent) {
            content.append(ch, start, length);
        }
    }
}
