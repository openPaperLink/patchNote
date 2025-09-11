package retrieval.core.PatternBasedSearcher;

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class ModifyOperatorSearcher extends AbstractSearcher{
    public ModifyOperatorSearcher(File javaFile, CompilationUnit unit, Integer lineNum){
        super(javaFile, unit, lineNum);
    }

}
