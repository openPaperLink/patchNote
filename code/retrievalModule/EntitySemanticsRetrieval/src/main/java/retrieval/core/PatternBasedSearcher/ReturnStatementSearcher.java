package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.*;

import java.io.File;


public class ReturnStatementSearcher extends AbstractSearcher{
    public ReturnStatementSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void searchReturnStatement(ITree tree){
        processReturnStatement(tree);
    }


}
