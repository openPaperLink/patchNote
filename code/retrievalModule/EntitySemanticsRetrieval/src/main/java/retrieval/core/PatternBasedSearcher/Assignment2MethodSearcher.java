package retrieval.core.PatternBasedSearcher;

import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Assignment2MethodSearcher extends AbstractSearcher{
    public Assignment2MethodSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }

    public void  searchAssignment2Method(ITree buggyTree, ITree fixedTree){
        processAssignment1(buggyTree);
        processMethodInvocation(fixedTree);
    }

    private void processAssignment1(ITree tree) {
        Assignment assignment = (Assignment) tree.getASTNode();
        Assignment.Operator operator = assignment.getOperator();
        List<Expression> vals = new ArrayList<>();
        vals.add(assignment.getLeftHandSide());
        vals.add(assignment.getRightHandSide());
        for (int i = 0; i < vals.size(); i++) {
            Expression side = vals.get(i);
            if(side instanceof InfixExpression){
                processInfixExpression(tree.getChild( i==1 ? 2 : i ));
            }else if(side instanceof MethodInvocation){
                processMethodInvocation(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof Name){
                processName(side);
            }else if (side instanceof ArrayCreation){
                processArrayAccess(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof ConditionalExpression){ //三目运算符 ?:
                processCoditionalExpression(tree.getChild(i==1 ? 2 : i));
            }else if (side instanceof FieldAccess)
                processFieldAccess(tree.getChild(i==1 ? 2 : i));
            else{
                processAssignment1(tree.getChild(i==1 ? 2 : i));
            }
        }
    }

}
