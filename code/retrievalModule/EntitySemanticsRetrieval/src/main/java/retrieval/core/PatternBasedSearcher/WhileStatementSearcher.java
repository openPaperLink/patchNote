package retrieval.core.PatternBasedSearcher;

import retrieval.core.utils.Checker;
import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class WhileStatementSearcher extends AbstractSearcher{
    public WhileStatementSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile, unit, lineNum);
    }

    public void searchWhileStatement(ITree tree){
        processWhileStatement(tree);
    }

    public void searchOuterStatement(ITree tree) {
        ITree parent = tree.getParent();
        if (tree == null) return ;
        if(Checker.isMethodDeclaration(tree.getType())){
            return;
        }
        if (Checker.isIfStatement(tree.getType())) {
            super.processIfStatementChildren(tree);
        }
        searchOuterStatement(parent);
    }
}
