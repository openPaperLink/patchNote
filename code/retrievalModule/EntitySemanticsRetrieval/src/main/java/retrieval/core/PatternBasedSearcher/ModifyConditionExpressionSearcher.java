package retrieval.core.PatternBasedSearcher;

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class ModifyConditionExpressionSearcher extends AbstractSearcher{

    public ModifyConditionExpressionSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile, unit, lineNum);
    }

}
