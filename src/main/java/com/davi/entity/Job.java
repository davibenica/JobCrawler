package com.davi.entity;

public class Job {
    private final String title;
    private final String company;
    private final String location;
    private final Double salary;
    private final String description;
    private final String startDate;
    private final String jobUrl;
    private final String postedDate;



    private Job(Builder builder) {
        this.title = builder.title;
        this.company = builder.company;
        this.location = builder.location;
        this.salary = builder.salary;
        this.description = builder.description;
        this.startDate = builder.startDate;
        this.jobUrl = builder.jobUrl;
        this.postedDate = builder.postedDate;
    }

    public static class Builder {
        private String title;
        private String company;
        private String location;
        private Double salary;
        private String description;
        private String startDate;
        private String jobUrl;
        private String postedDate;
        public Builder setPostedDate(String postedDate){
            this.postedDate = postedDate;
            return this;
        }
        public Builder setJobUrl(String url){
            this.jobUrl = url;
            return this;
        }
        public Builder setStartDate(String startDate){
            this.startDate = startDate;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setCompany(String company) {
            this.company = company;
            return this;
        }

        public Builder setLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder setSalary(Double salary) {
            this.salary = salary;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Job build() {
            if (title == null || description == null) {
                throw new IllegalArgumentException("Title and description are required");
            }
            return new Job(this);
        }
    }

    // Getters only (immutability)
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public Double getSalary() { return salary; }
    public String getDescription() { return description; }
    public String getPostedDate() { return postedDate; }
    public String getJobUrl() { return jobUrl; }
    public String getStartDate() { return startDate; }

    @Override
    public String toString() {
        return "Job{" +
                "title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", location='" + location + '\'' +
                ", salary=" + salary +
                ", description='" + description + '\'' +
                ", startDate='" + startDate + '\'' +
                ", jobUrl='" + jobUrl + '\'' +
                ", postedDate='" + postedDate + '\'' +
                '}';
    }
}
