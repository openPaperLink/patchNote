package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class MethodInvocationSearcher extends AbstractSearcher{
    public MethodInvocationSearcher(File javaFile, CompilationUnit unit, Integer lineNum){
        super(javaFile, unit, lineNum);
    }
    public void searchMethodInvocation(ITree tree){
        processMethodInvocation(tree);
    }
}
