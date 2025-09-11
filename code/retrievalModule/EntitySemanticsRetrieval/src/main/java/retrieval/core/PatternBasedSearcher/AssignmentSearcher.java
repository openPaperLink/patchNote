package retrieval.core.PatternBasedSearcher;

import retrieval.entity.Pair;
import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AssignmentSearcher extends AbstractSearcher{
    public AssignmentSearcher(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }
    public void searchAssignment(ITree tree){
        processAssignment(tree);
    }
    public void searchStatementsUseRHS(ITree tree){
        Pair<Integer,Integer> boundary = getAstNodeBoundary(tree,this.getUnit());
        Assignment assg = (Assignment) tree.getASTNode();
        var rhs = assg.getRightHandSide();
        ITree methodRoot = this.getMethodRootTree();
        for(ITree child : methodRoot.getChildren()){
            int start = getAstNodeBoundary(child,this.getUnit()).getFirst();
            if(start < boundary.getSecond()){
                continue;
            }else{
                if( StamentContainsCertainName(child,rhs.toString()) ){
                    processChild(child);
                }
            }
        }
    }
    @Override
    protected void processAssignment(ITree tree) {
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
            }else if (side instanceof ArrayAccess){
                processArrayAccess(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof ConditionalExpression){ //三目运算符 ?:
                processCoditionalExpression(tree.getChild(i==1 ? 2 : i));
            } else if (side instanceof FieldAccess){
                processFieldAccess(tree.getChild(i==1 ? 2 : i));
            }else if (isLiteral(side)){
                processLiteral(side);
            }else if(side instanceof ArrayCreation){
                processArrayCreation(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof ClassInstanceCreation){
                processClassInstanceCreation(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof CastExpression){
                processCastExpression(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof PrefixExpression){
                processPrefixExpression(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof PostfixExpression){
                processPostfixExpression(tree.getChild(i==1 ? 2 : i));
            }else if (side instanceof Assignment){
                processAssignment(tree.getChild(i==1 ? 2 : i));
            }else if(side instanceof ParenthesizedExpression){
                processParenthesizedExpression(tree.getChild(i==1 ? 2 : i));
            }
            else{
                throw new RuntimeException("Unsupported Node Type: " + side.getClass().getName());
            }
        }
    }

}
