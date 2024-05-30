package pt.ulisboa.tecnico.cnv.metrics;

import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.document.Item;

public class AggregatedMetrics {
    private double alpha;
    private double beta;
    private long MaxInput;
    private long MinInput;
    private long MaxOutput;
    private long MinOutput;
    private String Function;
    private String MetricType;
    private long Timestamp;

    public AggregatedMetrics(double alpha, double beta, long MaxInput, long MinInput, long MaxOutput, long MinOutput,
            String Function, String MetricType, long Timestamp) {
        this.alpha = alpha;
        this.beta = beta;
        this.MaxInput = MaxInput;
        this.MinInput = MinInput;
        this.MaxOutput = MaxOutput;
        this.MinOutput = MinOutput;
        this.Function = Function;
        this.MetricType = MetricType;
        this.Timestamp = Timestamp;
    }

    public double estimateComplexity(double input) {
        // Input is scaled to the range [0, 1]
        // Output is scaled to the range [0, 100]

        // Appluy normalization
        double normalizedInput = (input - MinInput) / (MaxInput - MinInput);

        // Apply the function
        double output = alpha * normalizedInput + beta;
        System.out.println("Output: " + output);

        // Apply denormalization
        // TODO: Check if this is correct
        output = output * (MaxOutput - MinOutput) + MinOutput;
        System.out.println("Output after denormalization: " + output);

        return output;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public long getMaxInput() {
        return MaxInput;
    }

    public long getMinInput() {
        return MinInput;
    }

    public long getMaxOutput() {
        return MaxOutput;
    }

    public long getMinOutput() {
        return MinOutput;
    }

    public String getFunction() {
        return Function;
    }

    public String getMetricType() {
        return MetricType;
    }

    public long getTimestamp() {
        return Timestamp;
    }

    public Item toItem() {
        return new Item().withPrimaryKey("MetricType", MetricType, "Timestamp", String.valueOf(Timestamp))
                .withDouble("alpha", alpha)
                .withDouble("beta", beta)
                .withLong("MaxInput", MaxInput)
                .withLong("MinInput", MinInput)
                .withLong("MaxOutput", MaxOutput)
                .withLong("MinOutput", MinOutput)
                .withString("Function", Function);
    }

    public static AggregatedMetrics fromItem(Item item) {
        AggregatedMetrics metrics = new AggregatedMetrics(item.getDouble("alpha"), item.getDouble("beta"),
                item.getLong("MaxInput"), item.getLong("MinInput"), item.getLong("MaxOutput"),
                item.getLong("MinOutput"),
                item.getString("Function"), item.getString("MetricType"), item.getLong("Timestamp"));
        return metrics;

    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject(this);
        return json;
    }

    public String toString() {
        return this.toJSON().toString();
    }

}
