package com.davi;
import com.davi.entity.Job;
import com.davi.service.JobUrlFinder;
import com.davi.service.Scrapper;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Scrapper scrapper = new Scrapper();
//        //System.out.println(scrapper.getWebContentsJsoup("https://jobs.ashbyhq.com/numeric/f9c9e075-7799-414f-8601-f770c0cd9061/application?utm_source=Simplify&ref=Simplify"));
        List<String> urls = JobUrlFinder.getGithubUrl();
        List<Job> jobs = scrapper.getJobFromUrls(urls.subList(0,20));
        for (Job job: jobs){
            System.out.println("Scrapped " + job.getJobUrl());
        }
//        ArrayList<String> notScrapable = new ArrayList<>();
//
//        for (int i = 0; i < 20; i ++) {
//            try {
//                Job curJob = scrapper.getJobFromUrl(urls.get(i));
//                jobs.add(curJob);
//            } catch (Exception e) {
//                notScrapable.add(urls.get(i));
//                System.out.println(e);
//            }
//        }
//        for (Job job: jobs){
//            System.out.println(job.getJobUrl());
//        }
//        System.out.println(notScrapable.toString());


    }
}