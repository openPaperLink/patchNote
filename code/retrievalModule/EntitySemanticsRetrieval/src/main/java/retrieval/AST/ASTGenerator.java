package retrieval.AST;


import java.io.File;
import java.io.IOException;

import retrieval.entity.Pair;
import org.eclipse.jdt.core.dom.ASTParser;

import retrieval.jdt.generator.ExpJdtTreeGenerator;
import retrieval.jdt.tree.ITree;
import retrieval.jdt.tree.TreeContext;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ASTGenerator {
	
	public enum TokenType {
		EXP_JDT,
		RAW_TOKEN,
	}
	
	/**
	 * Generate AST for Java code file.
	 * 
	 * @param javaFile
	 * @param type
	 * @return
	 */
	public Pair<ITree,CompilationUnit> generateTreeForJavaFile(File javaFile, TokenType type) {
		ITree asTree = null;
		CompilationUnit unit = null;
		try {
			TreeContext tc = null;

			switch (type) {
			case EXP_JDT:
				Pair<TreeContext,CompilationUnit> res = new ExpJdtTreeGenerator().generateFromFile(javaFile);
				tc =res.getFirst();
				unit = res.getSecond();
				break;
			default:
				break;
			}
			
			if (tc != null){
				asTree = tc.getRoot();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Pair<>(asTree,unit);
	}
	
	public Pair<ITree,CompilationUnit> generateTreeForJavaFile(String javaFile, TokenType type) {
		ITree asTree = null;
		CompilationUnit unit = null;
		try {
			TreeContext tc = null;

			switch (type) {
				case EXP_JDT:

					Pair<TreeContext,CompilationUnit> res = new ExpJdtTreeGenerator().generateFromFile(javaFile);
					tc =res.getFirst();
					unit = res.getSecond();
					break;
				default:
					break;
			}

			if (tc != null){
				asTree = tc.getRoot();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Pair<>(asTree,unit);
	}
	
	public Pair<ITree,CompilationUnit> generateTreeForCodeFragment(String codeBlock, TokenType type) {
		ITree asTree = null;
		CompilationUnit unit = null;
		try {
			TreeContext tc = null;

			switch (type) {
				case EXP_JDT:
					Pair<TreeContext,CompilationUnit> res = new ExpJdtTreeGenerator().generateFromFile(codeBlock,ASTParser.K_STATEMENTS);
					tc =res.getFirst();
					unit = res.getSecond();
					break;
				default:
					break;
			}

			if (tc != null){
				asTree = tc.getRoot();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Pair<>(asTree,unit);
	}
	
	public Pair<ITree,CompilationUnit> generateTreeForJavaFileContent(String codeBlock, TokenType type) {
		ITree asTree = null;
		CompilationUnit unit = null;
		try {
			TreeContext tc = null;

			switch (type) {
				case EXP_JDT:
					Pair<TreeContext,CompilationUnit> res = new ExpJdtTreeGenerator().generateFromFile(codeBlock);
					tc =res.getFirst();
					unit = res.getSecond();
					break;
				default:
					break;
			}

			if (tc != null){
				asTree = tc.getRoot();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Pair<>(asTree,unit);
	}

}
