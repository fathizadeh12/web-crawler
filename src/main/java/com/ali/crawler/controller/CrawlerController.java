package com.ali.crawler.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import com.ali.crawler.service.CrawlerService;
import java.io.IOException;

@RestController
@RequestMapping("/crawl")
public class CrawlerController {

    @Value("${source.web.url}")
   private String sourceWebUrl;

    final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping()
    public void crawl() throws IOException {
        crawlerService.fetchProductsCategoriesLinks(sourceWebUrl);
    }
}
