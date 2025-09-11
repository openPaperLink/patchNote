package retrieval.core.PatternBasedSearcher;

import retrieval.entity.Pair;
import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class EarlyReturnSearcher extends AbstractSearcher{
    public EarlyReturnSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void searchStatementsBehind(ITree tree){
        Pair<Integer,Integer> boundary = getAstNodeBoundary(tree,this.getUnit());

        ITree methodRoot = this.getMethodRootTree();
        int count = 0;
        for(ITree child : methodRoot.getChildren()){
            int start = getAstNodeBoundary(child,this.getUnit()).getFirst();
            if(start < boundary.getSecond()){
                continue;
            }else{
                if(count < 5){
                    processChild(child);
                    count++;
                }
            }
        }
    }

}
