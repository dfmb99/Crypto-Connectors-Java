package utils;

public class MathCustom {
    public static float calculateSD(float numArray[])
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += java.lang.Math.pow(num - mean, 2);
        }

        return (float) java.lang.Math.sqrt(standardDeviation/ (length-1) );
    }
}
