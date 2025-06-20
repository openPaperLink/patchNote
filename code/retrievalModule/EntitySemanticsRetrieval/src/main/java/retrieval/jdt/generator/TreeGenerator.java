package retrieval.jdt.generator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import retrieval.entity.Pair;
import retrieval.jdt.tree.TreeContext;
import org.eclipse.jdt.core.dom.CompilationUnit;

public abstract class TreeGenerator {

    protected abstract Pair<TreeContext, CompilationUnit> generate(Reader r) throws IOException;
    
    protected abstract Pair<TreeContext, CompilationUnit> generate(Reader r, int astParserType) throws IOException;

    /**
     * Generate ASTs for Java code from the IOReader of Java code.
     * @param r of the IOReader of Java code.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromReader(Reader r) throws IOException {
        Pair<TreeContext, CompilationUnit> pair = generate(r);
        pair.getFirst().validate();
        return pair;
    }

    /**
     * Generate ASTs for Java code from the Java code file.
     * @param fileName of the Java code file with complete and correct path.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromFile(String fileName) throws IOException {
        return generateFromReader(new FileReader(fileName));
    }

    /**
     * Generate ASTs for Java code from the Java code file.
     * @param file of the Java code file object.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromFile(File file) throws IOException {
        return generateFromReader(new FileReader(file));
    }

    /**
     * Generate ASTs for Java code from InputStream of Java code.
     * @param stream of the InputStream of Java code.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromStream(InputStream stream) throws IOException {
        return generateFromReader(new InputStreamReader(stream));
    }

    /**
     * Generate ASTs for Java code from the Java code string.
     * @param codeString of the Java code string.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromCodeString(String codeString) throws IOException {
        return generateFromReader(new StringReader(codeString));
    }
    
    /**
     * Generate ASTs for Java code from the IOReader of Java code with the specific ASTParser.
     * @param r of the IOReader of Java code.
     * @param astParserType of the specific ASTParser. e.g., ASTParser.K_STATEMENTS.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromReader(Reader r, int astParserType) throws IOException {
        Pair<TreeContext, CompilationUnit> res = generate(r, astParserType);
        res.getFirst().validate();
        return res;
    }

    /**
     * Generate ASTs for Java code from the Java code file with the specific ASTParser.
     * @param fileName of the Java code file name with complete and correct path.
     * @param astParserType of the specific ASTParser. e.g., ASTParser.K_STATEMENTS.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromFile(String fileName, int astParserType) throws IOException {
        return generateFromReader(new FileReader(fileName), astParserType);
    }

    /**
     * Generate ASTs for Java code from the Java code file with the specific ASTParser.
     * @param file of the Java code file object.
     * @param astParserType of the specific ASTParser. e.g., ASTParser.K_STATEMENTS.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromFile(File file, int astParserType) throws IOException {
        return generateFromReader(new FileReader(file), astParserType);
    }

    /**
     * Generate ASTs for Java code from the InputStream of Java code with the specific ASTParser.
     * @param stream of the InputStream of Java code.
     * @param astParserType of the specific ASTParser. e.g., ASTParser.K_STATEMENTS.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromStream(InputStream stream, int astParserType) throws IOException {
        return generateFromReader(new InputStreamReader(stream), astParserType);
    }

    /**
     * Generate ASTs for Java code fragment file with the specific ASTParser.
     * @param codeFragment of Java code fragment.
     * @param astParserType of the specific ASTParser. e.g., ASTParser.K_STATEMENTS.
     * @return
     * @throws IOException
     */
    public Pair<TreeContext, CompilationUnit> generateFromCodeFragment(String codeFragment, int astParserType) throws IOException {
        return generateFromReader(new StringReader(codeFragment), astParserType);
    }
}
