package utils;

import java.util.Arrays;

public class MathCustom {
    public static float calculateSD(float[] numArray) {
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

    public static float calculateSD(long[] numArray) {
        float sum = 0.0f, standardDeviation = 0.0f;
        int length = numArray.length;

        for (long num : numArray) {
            sum += num;
        }

        float mean = sum / (float) length;

        for (long num : numArray) {
            standardDeviation += (float) java.lang.Math.pow(num - mean, 2f);
        }

        return (float) java.lang.Math.sqrt(standardDeviation /  ((float) length - 1f));
    }

    public static float roundToFraction(float x, float fraction) {
        return (float) Math.round(x * (1f / fraction)) / (1f / fraction);
    }

    public static float calculateMedian(float[] numArray) {
        Arrays.sort(numArray);
        if (numArray.length % 2 == 0)
            return (numArray[numArray.length/2] + numArray[numArray.length/2 - 1]) / 2f;
        else
            return numArray[numArray.length/2];
    }
}
