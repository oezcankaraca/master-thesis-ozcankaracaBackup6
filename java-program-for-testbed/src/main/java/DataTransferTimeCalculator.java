public class DataTransferTimeCalculator {
    public static void main(String[] args) {
        long dataSizeBytes = 10705702; 
        int speedKbps = 5732; 
        double speedKbytesPerSecond = speedKbps / 8.0;
        double dataSizeKilobytes = dataSizeBytes / 1000.0;
        double timeSeconds = dataSizeKilobytes / speedKbytesPerSecond;

        System.out.println("Info: Time required for data transfer: " + timeSeconds + " seconds");
    }
}
