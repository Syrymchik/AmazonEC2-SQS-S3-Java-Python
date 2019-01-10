package aws.cloud.sqs.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Result {

    @JsonProperty("name")
    private String name;

    @JsonProperty("min")
    private Integer min;

    @JsonProperty("max")
    private Integer max;

    @JsonProperty("mean")
    private float mean;

    @JsonProperty("median")
    private float median;

    public Result() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public float getMean() {
        return mean;
    }

    public void setMean(float mean) {
        this.mean = mean;
    }

    public float getMedian() {
        return median;
    }

    public void setMedian(float median) {
        this.median = median;
    }
}
