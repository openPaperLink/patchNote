package retrieval.core.config;

public class Configuration {
    public static String projectBaseDir = "Path2patchEnclosingProjectSourceCode"; // patch enclosing project path
    public static String srcDir = "src/main/java/";
    public static String testDir = "src/test/java/";
    public static String buggyFilePath = "";
    public static String fixedFilePath = "";
    public static Integer buggyLineNum = -1;
    public static Integer fixedLineNum = -1;
    public static String savedBasePath = "./outputs/";
}
