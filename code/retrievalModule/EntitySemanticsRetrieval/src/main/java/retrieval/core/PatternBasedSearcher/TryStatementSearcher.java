package retrieval.core.PatternBasedSearcher;

import retrieval.core.utils.Checker;
import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

public class TryStatementSearcher extends AbstractSearcher {



    public TryStatementSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void searchInnerStatements(ITree tree) {
        ITree parent = tree.getParent();
        if (tree == null) return;
        if (Checker.isIfStatement(tree.getType())) {
            int childrenSum = tree.getChildren().size();
            int blockChildSum = 0;
            for (ITree child : tree.getChildren()) {
                if (Checker.isBlock(child.getType())) {
                    blockChildSum += 1;
                }
            }
            int tryBlockIndex = childrenSum - blockChildSum;
            ITree thenBlock = tree.getChild(tryBlockIndex);
            for (ITree child : thenBlock.getChildren()) {
                processChild(child);
            }
        }
    }
}
