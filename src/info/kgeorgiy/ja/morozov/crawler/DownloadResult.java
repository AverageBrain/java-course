package info.kgeorgiy.ja.morozov.crawler;

import java.io.IOException;
import java.util.List;


/**
 * Page download result
 *
 * @author Anton Morozov
 * */
public class DownloadResult {
    private final String curUrl;
    private IOException exception;
    private List<String> links;
    private boolean isDownloaded;


    /**
     * Create unnamed undownloaded instance of class
     * */
    public DownloadResult() {
        this.curUrl = "";
        this.exception = null;
        this.links = List.of();
        this.isDownloaded = false;
    }

    /**
     * Create undownloaded instance of class without expection
     *
     * @param url the url from which they tried to download
     * */
    public DownloadResult(String url) {
        this.curUrl = url;
        this.exception = null;
        this.links = List.of();
        this.isDownloaded = false;
    }

    /**
     * Create downloaded instance of class with list of links from cur url
     *
     * @param url the url from which they tried to download
     * @param links list of links from this url
     * */
    public DownloadResult(String url, List<String> links) {
        this.curUrl = url;
        this.links = links;
        this.isDownloaded = true;
        this.exception = null;
    }

    /**
     * Create undownloaded instance of class with exception
     *
     * @param url the url from which they tried to download
     * @param exception exception which occurred during to download
     * */
    public DownloadResult(String url, IOException exception) {
        this.curUrl = url;
        this.exception = exception;
        this.isDownloaded = false;
        this.links = List.of();
    }

    /**
     * Links setter
     *
     * @param links new list of links
     * */
    public void setLinks(final List<String> links) {
        this.links = links;
    }

    /**
     * Download setter
     *
     * @param downloaded true if page downloaded else false
     * */
    public void setDownloaded(final boolean downloaded) {
        isDownloaded = downloaded;
    }

    /***
     * Exception setter
     *
     * @param exception new exception
     */
    public void setException(final IOException exception) {
        this.exception = exception;
    }

    /**
     * Downloaded getter
     * */
    public boolean isDownloaded() {
        return isDownloaded;
    }

    /**
     * Exception getter
     * */
    public IOException getException() {
        return exception;
    }

    /**
     * Links getter
     * */
    public List<String> getLinks() {
        return links;
    }

    /**
     * URL getter
     * */
    public String getCurUrl() {
        return curUrl;
    }
}
