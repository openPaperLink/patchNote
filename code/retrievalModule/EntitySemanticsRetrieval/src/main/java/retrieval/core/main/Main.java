package retrieval.core.main;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import retrieval.core.config.Configuration;
import retrieval.core.PatternBasedSearcher.AbstractSearcher;
import retrieval.core.utils.FileHelper;
import java.io.File;
import com.google.gson.Gson;
import java.io.FileReader;
import java.util.Map;

import retrieval.core.utils.TemplateMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrieval.entity.ClassInfo;
import retrieval.entity.MethodInfo;
import retrieval.entity.TestInfo;
import retrieval.entity.VariableInfo;

public class Main {

    private static Set<MethodInfo> methodInfoSet = new HashSet<>();
    private static Set<VariableInfo> variableInfoSet = new HashSet<>();
    private static Set<ClassInfo> classInfoSet = new HashSet<>();
    private static Set<TestInfo> testInfoSet = new HashSet<>();
    private static Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws IOException {

        FileHelper fileHelper = new FileHelper();
        String cont = fileHelper.readFileContent("./inputs/bug_info_4j1.txt"); //bug-fixing patch change line
        int count = 0;
        for (String line : cont.split("\n")) {
            count++;
//            if (count != 36 ){
//                continue;
//            }
            String[] info = line.split(";");
            String bugid = info[0];
            String[] bugid_parts = bugid.split("-");
            String project = bugid_parts[0];
            String id = bugid_parts[1];

            System.out.println("project = " + project);
            System.out.println("id = " + id);
            if(project.equals("Math") || project.equals("Time")) {
                Configuration.srcDir = "src/main/java/";
                Configuration.testDir = "src/test/java/";
            }else if(project.equals("Chart")){
                Configuration.srcDir = "source/";
                Configuration.testDir = "tests/";
            }else if(project.equals("Closure")){
                Configuration.srcDir = "src/";
                Configuration.testDir = "test/";
            }else if (project.equals("Cli")){
                int ID = Integer.parseInt(id);
                if(ID < 32){
                    Configuration.srcDir = "src/java/";
                    Configuration.testDir = "src/test/";
                }else{
                    Configuration.srcDir = "src/main/java/";
                    Configuration.testDir = "src/test/java/";
                }
            }else if(project.equals("Lang")){
                int ID = Integer.parseInt(id);
                if(ID <= 35){
                    Configuration.srcDir = "src/main/java/";
                    Configuration.testDir = "src/test/java/";
                }else{
                    Configuration.srcDir = "src/java/";
                    Configuration.testDir = "src/test/";
                }
            }else{
                try (FileReader reader = new FileReader("./inputs/classes_path.json")) {
                    Gson gson = new Gson();
                    Map<String, Object> map = gson.fromJson(reader, Map.class);
                    if(map.containsKey(bugid)){
                        Configuration.srcDir = map.get(bugid).toString()+"/";
                    }else{
                        continue;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try (FileReader reader = new FileReader("./inputs/tests_path.json")) {
                    Gson gson = new Gson();
                    Map<String, Object> map = gson.fromJson(reader, Map.class);
                    if(map.containsKey(bugid)){
                        Configuration.testDir = map.get(bugid).toString()+"/";
                    }else{
                        continue;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(project.equals("Tika_core")){
                Configuration.srcDir = "tika-core/src/main/java/";
                Configuration.testDir = "tika-core/src/test/java/";
            }else{
                // some projects in GrowingBugs contains sub-project, need to obtain the path of sub-project
                try (FileReader reader = new FileReader("./inputs/subprojects.json")) {
                    Gson gson = new Gson();
                    Map<String, Object> map = gson.fromJson(reader, Map.class);
                    String subproject = null;
                    if(map.containsKey(bugid)){
                        subproject = map.get(bugid).toString()+"/";
                    }else{
                        continue;
                    }
                    if(subproject.length()!=0){
                        Configuration.testDir = subproject + "/" + Configuration.testDir;
                        Configuration.srcDir = subproject + "/" + Configuration.srcDir;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            setJavaFilePath(project, id);
            Set<String> patterns = new HashSet<>();
            for (int i = 1; i < info.length; i++) {
                String[] linesNum = info[i].split(",");
                setFaullocalizationInfo(linesNum[0], linesNum[1]);
                List<AbstractSearcher> css = TemplateMatcher.templateMatch(patterns);
                for (int j = 0; j < css.size(); j++) {
                    AbstractSearcher searcher = css.get(j);
                    searcher.searchVariableDefinitionAndComments();
                    searcher.searchDependedClass(project,id);
                    searcher.searchMethodDefinitionAndComments();
                    searcher.searchClassComments();
                    searcher.searchTriggerTests(info[0]);
                    mergeInfo(searcher,info[0]);

                }
            }
            recordPatchPatterns(bugid,patterns);
            saveInfo(project+"-"+id);
            clearCachedInfo();
        }
    }
    public static void recordPatchPatterns(String bugid, Set<String> patterns) {
        try {
            FileWriter writer = new FileWriter("./outputs/patch_patterns.txt", true);
            String content = bugid+",";
            for(String pattern : patterns){
                //System.out.println(pattern);
                content += pattern+",";
            }
            writer.write(content+"\n");
            writer.close();
        }catch (IOException e) {
            System.out.println("File Operation Error");
            e.printStackTrace();
        }
    }

    public static void mergeInfo(AbstractSearcher cs, String bugid) {
        var classAnnotations = cs.getClassAnnotations();
        var testinfos = cs.getTestInfos();
        mergeClassInfo(classAnnotations);
        mergeMethodInfo(cs);
        mergeVariableInfo(cs);
        mergeTestInfo(testinfos);
    }

    public static void saveInfo(String bugid) {
        saveClassInfo(bugid);
        saveTestInfo(bugid);
        saveVariableInfo(bugid);
        saveMethodInfo(bugid);
    }

    public static void clearCachedInfo(){
        methodInfoSet = new HashSet<>();
        variableInfoSet = new HashSet<>();
        classInfoSet = new HashSet<>();
        testInfoSet = new HashSet<>();
    }

    private static void mergeClassInfo(List<ClassInfo> classInfos){
            for (ClassInfo classInfo : classInfos) {
                classInfoSet.add(classInfo);
            }
    }

    public static void saveClassInfo(String bugid) {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("description", "the following is information about java class related with the patch");
            info.put("information", classInfoSet);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(Configuration.savedBasePath+"classesInfo/"+ bugid +".json"), info);
        } catch (IOException e) {
        }
    }

    private static void mergeTestInfo(Set<TestInfo> testInfos) {
            for (TestInfo tInfo : testInfos) {
                testInfoSet.add(tInfo);
            }
    }

    public static void saveTestInfo(String bugid) {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("description", "the following is information about unit tests related with the patch");
            info.put("information", testInfoSet);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(Configuration.savedBasePath + "testsInfo/" + bugid + ".json"), info);
        } catch (IOException e) {
        }
    }

    private static void mergeVariableInfo(AbstractSearcher cs) {
        List<VariableInfo> vi = cs.getVariableInfos();
        for (VariableInfo varInfo : vi){
            variableInfoSet.add(varInfo);
        }
    }

    public static void saveVariableInfo(String bugid) {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("description", "the following is information about values related with the patch");
            info.put("information", variableInfoSet);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(Configuration.savedBasePath+"variablesInfo/"+ bugid +".json"), info);

        } catch (IOException e) {
        }
    }
    //TODO:
    private static void mergeMethodInfo(AbstractSearcher cs) {

            for (MethodInfo mInfo : cs.getMethodInfos()) {
                methodInfoSet.add(mInfo);
            }

    }

    public static void saveMethodInfo(String bugid) {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("description", "the following is information about methods related with the patch");
            info.put("buggyMethodInfomation", methodInfoSet);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(Configuration.savedBasePath+"methodsInfo/"+ bugid +".json"), info);
        } catch (IOException e) {
        }
    }

    public static void setFaullocalizationInfo(String buggyLineNum, String fixedLineNum){
        Configuration.buggyLineNum = Integer.parseInt(buggyLineNum.trim());
        Configuration.fixedLineNum = Integer.parseInt(fixedLineNum.trim());
    }

    public static void setJavaFilePath(String project, String id){

        FileHelper fileHelper = new FileHelper();
        //String bugID = project + "_" + id;
        String patchPath = "./inputs/patches/" + project+"/"+ id + ".src.patch";

        String cont = "";

        try {
            cont = fileHelper.readFileContent(patchPath);

        } catch (IOException e) {
            System.out.println("file reading error");
        }

        String packageName = "";
        for (String s : cont.split("\n")) {
            if (s.startsWith("---")) {
                packageName = s.split("\\s")[1];
                packageName = packageName.substring(2);
                break;
            }
        }
        Configuration.buggyFilePath = FileHelper.pathToPackage("b",project,id,packageName);
        Configuration.fixedFilePath = FileHelper.pathToPackage("f",project,id,packageName);
    }
}
