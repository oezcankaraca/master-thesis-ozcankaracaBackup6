public class DataTransferTimeCalculator {
    public static void main(String[] args) {
        long dataSizeBytes = 137155097; 
        int speedKbps = 780; 
        double speedKbytesPerSecond = speedKbps / 8.0;
        double dataSizeKilobytes = dataSizeBytes / 1000.0;
        double timeSeconds = dataSizeKilobytes / speedKbytesPerSecond;

        System.out.println("Time required for data transfer: " + timeSeconds + " seconds");
    }
}