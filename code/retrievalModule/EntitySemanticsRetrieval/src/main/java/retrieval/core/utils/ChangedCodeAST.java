package retrieval.core.utils;

import retrieval.core.config.Configuration;
import retrieval.core.PatternBasedSearcher.AbstractSearcher;
import retrieval.entity.Pair;
import retrieval.jdt.tree.ITree;
import lombok.Data;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.List;

@Data
public class ChangedCodeAST {

    private CompilationUnit buggyCompilationUnit;
    private CompilationUnit fixedCompilationUnit;
    private ITree buggyMethodRootTree;
    private ITree fixedMethodRootTree;
    public Pair<List<Pair<ITree,String>>,List<Pair<ITree,String>>> getInstance(){
        File buggyJavaFile = new File(Configuration.buggyFilePath);

        //将buggy版本代码转换成AST
        Pair<ITree, CompilationUnit> buggyAst = CodeParser.parseCode(buggyJavaFile);
        ITree buggyRootTree = buggyAst.getFirst();
        this.buggyCompilationUnit = buggyAst.getSecond();

        File fixedJavaFile = new File(Configuration.fixedFilePath);
        //将fixed版本代码转换成AST
        Pair<ITree,CompilationUnit> fixedAst = CodeParser.parseCode(fixedJavaFile);
        ITree fixedRootTree = fixedAst.getFirst();
        this.fixedCompilationUnit = fixedAst.getSecond();

        AbstractSearcher bsi = new AbstractSearcher(buggyJavaFile,this.buggyCompilationUnit,Configuration.buggyLineNum);
        AbstractSearcher fsi = new AbstractSearcher(fixedJavaFile,this.fixedCompilationUnit,Configuration.fixedLineNum);
        bsi.searchCodeAst(buggyRootTree);
        fsi.searchCodeAst(fixedRootTree);
        this.buggyMethodRootTree = bsi.getMethodRootTree();
        this.fixedMethodRootTree = fsi.getMethodRootTree();
        Pair<List<Pair<ITree,String>>,List<Pair<ITree,String>>> pair = new Pair<>(bsi.getFoundCodeNodes(),fsi.getFoundCodeNodes());
        return pair;
    }


}
