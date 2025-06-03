package retrieval.jdt.generator;

import retrieval.entity.Pair;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import retrieval.jdt.tree.TreeContext;
import retrieval.jdt.visitor.AbstractJdtVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public abstract class AbstractJdtTreeGenerator extends TreeGenerator {

    private static char[] readerToCharArray(Reader r) throws IOException {
        StringBuilder fileData = new StringBuilder();
        try (BufferedReader br = new BufferedReader(r)) {
            char[] buf = new char[10];
            int numRead = 0;
            while ((numRead = br.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
        }
        return  fileData.toString().toCharArray();
    }

    @Override
    public Pair<TreeContext, CompilationUnit> generate(Reader r) throws IOException {
    	return generate(r, ASTParser.K_COMPILATION_UNIT);
    }
    
    @Override
    public Pair<TreeContext, CompilationUnit> generate(Reader r, int astParserType) throws IOException {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(astParserType);
        Map<String, String> pOptions = JavaCore.getOptions();
        pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
        pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
        pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
        pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        parser.setCompilerOptions(pOptions);
        parser.setSource(readerToCharArray(r));
        AbstractJdtVisitor v = createVisitor();

        ASTNode ast = parser.createAST(null);
        CompilationUnit unit = (CompilationUnit) ast;
        ast.accept(v);
        return new Pair<TreeContext,CompilationUnit>(v.getTreeContext(),unit);
    }

    protected abstract AbstractJdtVisitor createVisitor();
}
