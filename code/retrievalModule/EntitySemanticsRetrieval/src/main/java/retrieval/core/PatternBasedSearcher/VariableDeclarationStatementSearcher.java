package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

public class VariableDeclarationStatementSearcher extends AbstractSearcher{
    public VariableDeclarationStatementSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void searchVariableDeclaration(ITree tree) {
        processVariableDeclarationStatement(tree);
    }

}
