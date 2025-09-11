package retrieval.core.PatternBasedSearcher;

import retrieval.entity.Pair;
import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class MoveStatementSearcher extends AbstractSearcher {



    public MoveStatementSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void searchMoveStatement(ITree tree) {
        processChild(tree);
    }

    public void searchInnerStatements(ITree first,ITree second) {
        Pair<Integer,Integer> boundary1 = getAstNodeBoundary(first,this.getUnit());
        Pair<Integer,Integer> boundary2 = getAstNodeBoundary(second,this.getUnit());
        Pair<Integer,Integer> front = null;
        Pair<Integer,Integer> behind = null;
        if(boundary1.getFirst() < boundary2.getFirst()) {
            front = boundary1;
            behind = boundary2;
        }else{
            front = boundary2;
            behind = boundary1;
        }
        ITree methodRoot = this.getMethodRootTree();
        for(ITree child : methodRoot.getChildren()){
            int start = getAstNodeBoundary(child,this.getUnit()).getFirst();
            int end = getAstNodeBoundary(child,this.getUnit()).getSecond();
            if(start < front.getSecond() || end > behind.getFirst()){
                continue;
            }else{
                //处于两个位置之间
                processChild(child);
            }
        }
    }
}
