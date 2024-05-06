package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CrawlTask extends RecursiveAction {
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final PageParserFactory parserFactory;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;

    private CrawlTask(String url,
                      Instant deadline,
                      int maxDepth,
                      Map<String, Integer> counts,
                      Set<String> visitedUrls,
                      PageParserFactory parserFactory,
                      Clock clock,
                      List<Pattern> ignoredUrls) {
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.parserFactory = parserFactory;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (!visitedUrls.add(url)) {
            return;
        }
        PageParser.Result result = parserFactory.get(url).parse();
        synchronized (counts) {
            for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
                counts.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        List<CrawlTask> subTasks =
                result.getLinks().stream()
                        .map(link -> new CrawlTask.Builder()
                                .setUrl(link)
                                .setDeadline(deadline)
                                .setMaxDepth(maxDepth - 1)
                                .setCounts(counts)
                                .setVisitedUrls(visitedUrls)
                                .setParserFactory(parserFactory)
                                .setClock(clock)
                                .setIgnoredUrls(ignoredUrls)
                                .build())
                        .collect(Collectors.toList());
        invokeAll(subTasks);
    }

    public static class Builder {
        private String url;
        private Instant deadline;
        private int maxDepth;
        private Map<String, Integer> counts;
        private Set<String> visitedUrls;
        private PageParserFactory parserFactory;
        private Clock clock;
        private List<Pattern> ignoredUrls;

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }
        public Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }
        public Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }
        public Builder setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }
        public Builder setVisitedUrls(Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }
        public Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }
        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }
        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public CrawlTask build() {
            return new CrawlTask(url, deadline, maxDepth, counts, visitedUrls, parserFactory, clock, ignoredUrls);
        }
    }
}
