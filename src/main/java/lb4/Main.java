package lb4;

public class Main {

    public static void main(String[] args) {
        String mapPath = "src/main/java/lb4/citymap.txt";
        String outputPath = "src/main/java/lb4/output";

        Pipeline pipe = new Pipeline(mapPath, outputPath);
        pipe.run();
    }
}
