package com.razorpay.experiment.dto;

public class RolloutConfigDto {

    private int percentage;
    private String bucketingSalt = "";
    private boolean shareBucketAcrossExperiments;

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public String getBucketingSalt() {
        return bucketingSalt;
    }

    public void setBucketingSalt(String bucketingSalt) {
        this.bucketingSalt = bucketingSalt;
    }

    public boolean isShareBucketAcrossExperiments() {
        return shareBucketAcrossExperiments;
    }

    public void setShareBucketAcrossExperiments(boolean shareBucketAcrossExperiments) {
        this.shareBucketAcrossExperiments = shareBucketAcrossExperiments;
    }
}
