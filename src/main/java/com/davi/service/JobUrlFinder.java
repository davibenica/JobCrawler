package com.davi.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JobUrlFinder {
    /*
    This class is used to find job posting url by scrapping web pages
     */

    // Gets all valid urls from github page
    public static List<String> getGithubUrl() throws IOException {
        String gitHubUrl = "https://raw.githubusercontent.com/SimplifyJobs/New-Grad-Positions/dev/README.md";
        List<String> urls = new ArrayList<>();
        Document doc = Jsoup.connect(gitHubUrl).get();
        Elements anchors = doc.select("a[href]");
        for (Element a : anchors) {
            String link = a.attr("href").trim();
            if (!link.contains("simplify.jobs") && !link.contains("swelist.com")){
                urls.add(link);
            }
        }
        return urls;
    }

}
