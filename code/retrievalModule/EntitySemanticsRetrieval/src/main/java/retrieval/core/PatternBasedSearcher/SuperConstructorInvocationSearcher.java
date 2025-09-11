package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class SuperConstructorInvocationSearcher extends AbstractSearcher{
    public SuperConstructorInvocationSearcher(File javaFile, CompilationUnit unit, Integer lineNum){
        super(javaFile, unit, lineNum);
    }
    public void searchSuperConstructorInvocation(ITree tree){
        processSuperConstructorInvocation(tree);
    }
}
