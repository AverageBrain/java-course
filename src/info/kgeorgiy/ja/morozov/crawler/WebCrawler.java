package info.kgeorgiy.ja.morozov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;


/**
 * Implementation {@link Crawler} interfaces.
 * Recursive walk websites
 *
 * @author Anton Morozov
 * */
public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final ExecutorService downloadService;
    private final ExecutorService extractService;
    private final int perHost;
    private final ConcurrentHashMap<String, HostQueue> hostsDownloading;


    /**
     * Constructor of WebCrowler
     *
     * @param downloader Downloader
     * @param downloaders max number of threads to download pages
     * @param extractors max number of threads to extreact links
     * @param perHost max number of threads to download pages from one host
     * @see Downloader
     * @see CachingDownloader
     * */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadService = Executors.newFixedThreadPool(downloaders);
        this.extractService = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        this.hostsDownloading = new ConcurrentHashMap<>();
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, false, ConcurrentHashMap.newKeySet());
    }

    @Override
    public Result download(String url, int depth, List<String> hosts) {
        Set<String> setHosts = ConcurrentHashMap.newKeySet();
        setHosts.addAll(hosts);
        return download(url, depth, true, setHosts);
    }

    private Result download(String url, int depth, boolean acceptHosts, Set<String> hosts) {
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Set<String> downloadedURLS = ConcurrentHashMap.newKeySet();
        final Set<String> urls = ConcurrentHashMap.newKeySet();

        urls.add(url);

        for (int i = 0; i < depth; i++) {
            List<String> curDepth = List.copyOf(urls);
            urls.clear();
            boolean isLastLayer = (i == depth - 1);
            List<Future<DownloadResult>> futures = new ArrayList<>();
            for (String curURL : curDepth.stream().filter(Predicate.not(downloadedURLS::contains)).toList()) {
                futures.add(downloadURL(curURL, errors, urls, isLastLayer, downloadedURLS, curDepth, acceptHosts, hosts));
            }
            for (Future<DownloadResult> future : futures) {
                try {
                    DownloadResult result = future.get();
                    if (result.isDownloaded()) {
                        downloadedURLS.add(result.getCurUrl());
                        urls.addAll(result.getLinks());
                    } else if (result.getException() != null) {
                        errors.put(result.getCurUrl(), result.getException());
                    }
                } catch (ExecutionException | InterruptedException ignored) {
                }
            }
        }
        return new Result(List.copyOf(downloadedURLS), errors);
    }

    private Future<List<String>> extractLinks(Map<String, IOException> errors,
                              Set<String> urls,
                              Set<String> downloadedURLS,
                              List<String> curLinks,
                              Document document) {
        return extractService.submit(() -> {
            List<String> ans = new ArrayList<>();
            try {
                ans.addAll(document.extractLinks().stream().filter(link -> !(downloadedURLS.contains(link)
                       || urls.contains(link)
                       || errors.containsKey(link)
                       || curLinks.contains(link))).toList());
            } catch (IOException ignored) {
            }
            return ans;
        });

    }

    private Future<DownloadResult> downloadURL(String curURL,
                                               Map<String, IOException> errors,
                                               Set<String> urls,
                                               boolean isLastLayer,
                                               Set<String> downloadedURLS,
                                               List<String> curLinks,
                                               boolean acceptHosts,
                                               Set<String> hosts) {
        try {
            String host = URLUtils.getHost(curURL);
            HostQueue queue = hostsDownloading.computeIfAbsent(host, (key) -> new HostQueue(perHost));

            if (acceptHosts && !hosts.contains(host)) {
                CompletableFuture<DownloadResult> future = new CompletableFuture<>();
                future.complete(new DownloadResult(curURL));
                return future;
            }
            return queue.add(
                    () -> {
                        try {
                            final Document document = downloader.download(curURL);
                            DownloadResult res = new DownloadResult(curURL);
                            res.setDownloaded(true);
                            if (!isLastLayer) {
                                res.setLinks(extractLinks(errors, urls, downloadedURLS, curLinks, document).get());
                            }
                            return res;
                        } catch (IOException e) {
                            return new DownloadResult(curURL, e);
                        }
                    });
        } catch (MalformedURLException e) {
            CompletableFuture<DownloadResult> future = new CompletableFuture<>();
            future.complete(new DownloadResult(curURL, e));
            return future;
        }
    }

    private void shutdownAndAwait(final ExecutorService service) {
        service.shutdown();
        try {
            service.awaitTermination(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Service was interrupted: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        shutdownAndAwait(downloadService);
        shutdownAndAwait(extractService);
    }

    private class HostQueue {
        ConcurrentLinkedDeque<Map.Entry<Callable<DownloadResult>, CompletableFuture<DownloadResult>>> queue;

        Semaphore semaphore;

        public HostQueue(int perHost) {
            this.queue = new ConcurrentLinkedDeque<>();
            this.semaphore = new Semaphore(perHost);
        }

        public CompletableFuture<DownloadResult> add(Callable<DownloadResult> task) {
            CompletableFuture<DownloadResult> res = new CompletableFuture<>();
            queue.add(Map.entry(task, res));
            Runnable worker = () -> {
                Map.Entry<Callable<DownloadResult>, CompletableFuture<DownloadResult>> lastTask;
                while ((lastTask = queue.poll()) != null) {
                    DownloadResult ans;
                    try {
                        ans = lastTask.getKey().call();
                    } catch (Exception e) {
                        ans = new DownloadResult();
                    }
                    lastTask.getValue().complete(ans);
                }
                semaphore.release();
            };
            if (semaphore.tryAcquire()) {
                downloadService.execute(worker);
            }
            return res;
        }
    }


    /**
     * Аccepts requests {@code WebCrawler url [depth [downloads [extractors [perHost]]]]}, where
     * <ul>
     *     <li>{@code url} - url,</li>
     *     <li>{@code depth} - depth of crawler (default value = 1),</li>
     *     <li>{@code downloads} - max count of threads for downloading pages (default value = 1),</li>
     *     <li>{@code extractors} - max count of threads for extract links from page (default value = 1),</li>
     *     <li>{@code perHost} - max count of threads, whose download page from one host (default value = 2);</li>
     * </ul>
     *
     * @param args arguments of terminal
     * */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 5) {
            System.err.println("Expected 1 to 5 args");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected not null args");
            return;
        }
        int depth = getOrDefault(args, 1, 1);
        int downloads = getOrDefault(args, 2, 1);
        int extractors = getOrDefault(args, 3, 1);
        int perHost = getOrDefault(args, 4, 2);
        try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(0.005), downloads, extractors, perHost);) {
            Result result = webCrawler.download(args[0], depth);
            System.out.println("Downloaded urls:");
            for (String url : result.getDownloaded()) {
                System.out.println(url);
            }
            System.out.println("Errors:");
            for (Map.Entry<String, IOException> error : result.getErrors().entrySet()) {
                System.out.printf("%s: %s%n", error.getKey(), error.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error occurred during to create CachingDownloader");
        }
    }

    private static int getOrDefault(String[] args, int ind, int defaultValue) {
        try {
            return (ind < args.length ? Integer.parseInt(args[ind]) : defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
