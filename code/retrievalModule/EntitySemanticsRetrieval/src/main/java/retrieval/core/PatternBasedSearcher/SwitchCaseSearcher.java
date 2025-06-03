package retrieval.core.PatternBasedSearcher;

import retrieval.core.utils.Checker;
import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class SwitchCaseSearcher extends AbstractSearcher{
    public SwitchCaseSearcher(File javaFile, CompilationUnit unit, Integer lineNum){
        super(javaFile, unit, lineNum);
    }

    public void searchSwitchCase(ITree tree){
        processSwitchCase(tree);
    }

    public void searchOuterSwitchStatement(ITree tree) {
        ITree parent = tree.getParent();
        if (tree == null) return ;
        if(Checker.isMethodDeclaration(tree.getType())){
            return;
        }
        if (Checker.isSwitchStatement(tree.getType())) {
            super.processSwitchStatement(tree);
        }
        searchOuterSwitchStatement(parent);
    }
}
