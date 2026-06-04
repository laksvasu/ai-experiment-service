package com.razorpay.experiment.model;

import java.util.Objects;

public final class RolloutConfig {

    private final int percentage;
    private final String bucketingSalt;
    private final boolean shareBucketAcrossExperiments;

    public RolloutConfig(int percentage, String bucketingSalt, boolean shareBucketAcrossExperiments) {
        this.percentage = percentage;
        this.bucketingSalt = bucketingSalt != null ? bucketingSalt : "";
        this.shareBucketAcrossExperiments = shareBucketAcrossExperiments;
    }

    public int getPercentage() {
        return percentage;
    }

    public String getBucketingSalt() {
        return bucketingSalt;
    }

    public boolean isShareBucketAcrossExperiments() {
        return shareBucketAcrossExperiments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RolloutConfig that = (RolloutConfig) o;
        return percentage == that.percentage
                && shareBucketAcrossExperiments == that.shareBucketAcrossExperiments
                && Objects.equals(bucketingSalt, that.bucketingSalt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percentage, bucketingSalt, shareBucketAcrossExperiments);
    }
}
