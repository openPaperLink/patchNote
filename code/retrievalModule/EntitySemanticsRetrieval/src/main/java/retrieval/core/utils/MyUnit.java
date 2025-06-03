package retrieval.core.utils;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MyUnit {

    public CompilationUnit createCompilationUnit(File javaFile) {
        char[] javaCode = readFileToCharArray(javaFile);
        ASTParser parser = createASTParser(javaCode);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);

        return unit;
    }

    private ASTParser createASTParser(char[] javaCode) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(javaCode);

        return parser;
    }

    private char[] readFileToCharArray(File javaFile) {
        StringBuilder fileData = new StringBuilder();
        BufferedReader br = null;

        char[] buf = new char[10];
        int numRead = 0;
        try {
            FileReader fileReader = new FileReader(javaFile);
            br = new BufferedReader(fileReader);
            while ((numRead = br.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                    br = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (fileData.length() > 0)
            return fileData.toString().toCharArray();
        else return new char[0];
    }
}
