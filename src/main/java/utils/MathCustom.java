package utils;

public class MathCustom {
    public static float calculateSD(float numArray[]) {
        float sum = 0.0f, standardDeviation = 0.0f;
        int length = numArray.length;

        for (float num : numArray) {
            sum += num;
        }

        float mean = sum / (float) length;

        for (float num : numArray) {
            standardDeviation += (float) java.lang.Math.pow(num - mean, 2f);
        }

        return (float) java.lang.Math.sqrt(standardDeviation /  ((float) length - 1f));
    }

    public static float roundToFraction(float x, float fraction) {
        return (float) Math.round(x * (1f / fraction)) / (1f / fraction);
    }
}
