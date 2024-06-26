package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerImpl;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;


//I used Udacity GPT to help me with some sample codes and understanding the starter code and the logic.
public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    // Write the crawl results to a JSON file (or System.out if the file name is empty)
    String outputPath = config.getResultPath();
    if(!outputPath.isEmpty()) {
      resultWriter.write(Path.of(outputPath));
    } else {
      Writer writer = new OutputStreamWriter(System.out);
      resultWriter.write(writer);
      writer.flush();
    }
    // Write the profile data to a text file (or System.out if the file name is empty)
    String profileOutputPath = config.getProfileOutputPath();

    if(!profileOutputPath.isEmpty()) {
      profiler.writeData(Path.of(profileOutputPath));
    } else {
      Writer writer = new OutputStreamWriter(System.out);
      profiler.writeData(writer);
      writer.flush();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
