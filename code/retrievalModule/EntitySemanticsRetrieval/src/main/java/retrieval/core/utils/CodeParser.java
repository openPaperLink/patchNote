package retrieval.core.utils;

import retrieval.entity.Pair;
import retrieval.jdt.tree.ITree;

import java.io.File;

import retrieval.AST.ASTGenerator;
import retrieval.AST.ASTGenerator.TokenType;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class CodeParser {

    public static Pair<ITree, CompilationUnit> parseCode(File javaFile) {
        Pair<ITree, CompilationUnit> res = new ASTGenerator().generateTreeForJavaFile(javaFile, TokenType.EXP_JDT);
        return res;

    }




}
