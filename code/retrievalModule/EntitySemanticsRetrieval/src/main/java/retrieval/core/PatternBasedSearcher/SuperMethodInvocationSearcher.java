package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class SuperMethodInvocationSearcher extends AbstractSearcher {
    public SuperMethodInvocationSearcher(File javaFile, CompilationUnit unit, Integer lineNum){
        super(javaFile, unit, lineNum);
    }
    public void searchSuperMethodInvocation(ITree tree){
        processSuperMethodInvocation(tree);
    }

}
