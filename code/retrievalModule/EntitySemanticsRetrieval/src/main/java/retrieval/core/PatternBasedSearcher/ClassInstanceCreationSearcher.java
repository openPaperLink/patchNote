package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


public class ClassInstanceCreationSearcher extends AbstractSearcher{
    public ClassInstanceCreationSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile, unit, lineNum);
    }

    //查找外层if结构,后续将外层if内的condition expression
    // 中的变量和方法调用加入到对应集合中
    public void searchCreatedClasses(ITree tree) {
        processChild(tree);
    }

}
