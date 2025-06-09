package com.davi.service;

import com.davi.config.AppConfig;
import com.davi.entity.Job;
import com.davi.models.ChatModel;
import com.davi.config.HttpClientProvider;
import com.davi.models.OpenAI;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.*;


public class Scrapper {

    private final ChatModel chatModel;
    private final HttpClient client = HttpClientProvider.CLIENT;
    private final String prompt;
    public Scrapper(){
        AppConfig config = new AppConfig();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("prompt.txt")){
            if (input == null){
                throw new NullPointerException("prompt.txt file not found");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null){
                sb.append(line).append(System.lineSeparator());
            }
            this.prompt = sb.toString();
        }
        catch (IOException e){
            throw new RuntimeException("Failed to read prompt in resources/prompt.txt");

        }

        switch (config.get("ai.provider")){
            case "OpenAI":
                chatModel = new OpenAI();
            break;
            default:
                throw new IllegalStateException("Must provide a valid ai provider");
        }
    }


    private String getUrlType(String url){
        if (url.contains("myworkdayjobs")){
            return "Workday";
        }
        if (url.contains("oraclecloud")){
            return "Oracle";
        }
        return "Generic";

    }
    private String extractOracleJobId(String url){
        // Get oracle job id
        Pattern pattern = Pattern.compile("/job/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    private Job scrapeOracle(String url) throws IOException, InterruptedException {
        // Get host
        String host = url.split("/")[2];

        // Get oracle job id
        String jobId = extractOracleJobId(url);
        if (jobId == null){
            throw new RuntimeException("Cannot extract job id from oracle url: " + url );
        }
        String finderParam = String.format("ById;Id=\"%s\",siteNumber=CX_1001", jobId);
        String encodedFinder = URLEncoder.encode(finderParam, StandardCharsets.UTF_8);

        String oracleUrl = String.format("https://%s/hcmRestApi/resources/latest/recruitingCEJobRequisitionDetails?expand=all&onlyData=true&finder=%s",
                host,
                encodedFinder);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(oracleUrl)).GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(res.body());
        JSONObject jobDetails = json.getJSONArray("items").getJSONObject(0);

        String title = jobDetails.getString("Title");
        String startDate = jobDetails.getString("ExternalPostedStartDate");
        String jobLocation = jobDetails.getString("PrimaryLocation");
        String description = "";

        description += jobDetails.getString("ExternalDescriptionStr");
        description += jobDetails.getString("CorporateDescriptionStr");
        description += jobDetails.getString("OrganizationDescriptionStr");


        return new Job.Builder().setJobUrl(url)
                .setStartDate(startDate)
                .setTitle(title)
                .setLocation(jobLocation)
                .setDescription(description)
                .build();
    }

    public String getWorkDayUrl(String url) throws IOException {

        //https://jda.wd5.myworkdayjobs.com/en-US/JDA_Careers/job/Dallas/Software-Engineer-I_251265?utm_source=Simplify&ref=Simplify
        // https://jda.wd5.myworkdayjobs.com/wday/cxs/jda/JDA_Careers/job/Dallas/Software-Engineer-I_251265
        //https://roberthalf.wd1.myworkdayjobs.com/roberthalfcareers/job/SAN-RAMON/Software-Engineer-I_JR-257276-1?utm_source=Simplify&ref=Simplify
        // https://roberthalf.wd1.myworkdayjobs.com/wday/cxs/roberthalf/roberthalfcareers/job/SAN-RAMON/Software-Engineer-I_JR-257276-1
        String[] splitUrl = url.split("/");
        String host = splitUrl[2];
        String company = host.split("\\.")[0];
        String rest = "";
        int i = splitUrl.length == 7 ? 3 : 4;
        for (; i < splitUrl.length; i ++){
            rest += "/" + splitUrl[i];
        }
        return String.format("https://%s/wday/cxs/%s%s",
                host,
                company,
                rest);
    }

    private Job scrapeWorkday(String url) throws IOException, InterruptedException {
        // Worksay uses Server Side Rendering so to get infomartion we will need to hit the url
        // to get the information from the server
        // build workday url
//        String workdayUrl = url.replaceAll(
//                "^https://([a-zA-Z0-9-]+)\\.wd\\d+\\.myworkdayjobs\\.com/([^/]+)/(job/.+?)(?:\\?.*)?$",
//                "https://$1.wd.myworkdayjobs.com/wday/cxs/$1/$2/$3"
//        );
//        // https://roberthalf.wd1.myworkdayjobs.com/wday/cxs/roberthalf/roberthalfcareers/job/SAN-RAMON/Software-Engineer-I_JR-257276-1
        // https://roberthalf.wd5.myworkdayjobs.com/wday/cxs/roberthalf/roberthalfcareers/job/SAN-RAMON/Software-Engineer-I_JR-257276-1
        String workdayUrl = getWorkDayUrl(url);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(workdayUrl)).GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(res.body());
        JSONObject jobPostingInfo =  json.getJSONObject("jobPostingInfo");
        JSONObject hiringOrganization = json.getJSONObject("hiringOrganization");

        String title = jobPostingInfo.getString("title");
        String description = jobPostingInfo.getString("jobDescription");
        String location = jobPostingInfo.getString("location");
        String postedDate = jobPostingInfo.getString("postedOn");
        String company = hiringOrganization.getString("name");
        String startDate = jobPostingInfo.getString("startDate");

        return new Job.Builder().setTitle(title)
                .setJobUrl(url)
                .setDescription(description)
                .setLocation(location)
                .setPostedDate(postedDate)
                .setCompany(company)
                .setStartDate(startDate)
                .build();

    }
    private JSONObject stringToJson(String json){
        String cleaned = json.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove "```json"
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove just "```"
        }
        // Remove ``` at the end
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return new JSONObject(cleaned.trim());

    }

    public String getWebContentsJsoup(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        // Document text
        String text = doc.text();
        Elements metaTags = doc.getElementsByTag("meta");
        for (Element curTag: metaTags){
            text += curTag.attr("content");
        }
        return text;
    }

    private Job scrapeWithAI(String url) throws IOException {
        // TODO: Work on getting data for pages with server side rendering

        String webPageTextInfo = getWebContentsJsoup(url);
        String res = chatModel.chat(this.prompt + webPageTextInfo);
        JSONObject json = stringToJson(res);
        if (json.isEmpty()){
            throw new IOException("Not able to scrape the url: " + url);
        }
        String title = json.optString("title", null);
        String description = json.optString("description", null);
        String company = json.optString("company", null);
        String location = json.optString("location", null);
        Double salary = json.optDouble("salary", 0.0);
        String startDate = json.optString("startDate", null);
        return new Job.Builder().setTitle(title)
                .setDescription(description)
                .setLocation(location)
                .setStartDate(startDate)
                .setJobUrl(url)
                .setCompany(company)
                .setSalary(salary)
                .build();
    }


    public Job getJobFromUrl(String url) throws IOException, InterruptedException {
        switch (getUrlType(url)){
            case "Workday": return scrapeWorkday(url);
            case "Oracle": return scrapeOracle(url);
            case "Generic": return scrapeWithAI(url);
            default: throw new RuntimeException("Failed to find url type for the url: " + url);

        }
    }
    public List<Job> getJobFromUrls(List<String> urls) throws IOException, InterruptedException {
        int numThreads = 5;

        List<String>[] threadUrls = new List[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threadUrls[i] = new ArrayList<>();
        }

        for (int i = 0; i < urls.size(); i++) {
            threadUrls[i % numThreads].add(urls.get(i));
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<List<Job>>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int id = i;
            tasks.add(() -> {
                List<Job> res = new ArrayList<>();
                for (String url : threadUrls[id]) {
                    try {
                        res.add(getJobFromUrl(url));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return res;
            });
        }

        List<Job> jobs = new ArrayList<>();
        List<Future<List<Job>>> futures = executor.invokeAll(tasks);

        for (Future<List<Job>> future : futures) {
            try {
                jobs.addAll(future.get());
            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to retrieve results from a thread", e);
            }
        }
        executor.shutdown();
        return jobs;
    }

}
