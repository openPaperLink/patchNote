package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class InsertMethodInvocationSearcher extends AbstractSearcher{
    public InsertMethodInvocationSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void searchMethodInvocation(ITree tree) {
        processMethodInvocation(tree);
    }
}
