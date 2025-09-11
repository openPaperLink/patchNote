package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.*;

import java.io.File;


public class MethodOverrideSearcher extends AbstractSearcher{
    public MethodOverrideSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void searchParentClass(ITree tree){
        if( superClassName != null){
            classesToSearch.add(new File(superClassName));
        }
        processChild(tree);
    }



}
