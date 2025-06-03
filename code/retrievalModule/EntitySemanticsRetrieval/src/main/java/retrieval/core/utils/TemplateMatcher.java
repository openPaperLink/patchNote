package retrieval.core.utils;

import retrieval.core.config.Configuration;
//import com.jsh.bishe.core.templateBasedSearcher.*;
import retrieval.core.PatternBasedSearcher.*;
import retrieval.entity.Pair;
import retrieval.jdt.tree.ITree;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TemplateMatcher {

    public static List<Integer> readAllNodeTypes(ITree node) {
        List<Integer> nodeTypes = new ArrayList<>();
        nodeTypes.add(node.getType());
        List<ITree> children = node.getChildren();
        for (ITree child : children) {
            int childType = child.getType();
            if (Checker.isFieldDeclaration(childType) ||
                    Checker.isMethodDeclaration(childType) ||
                    Checker.isTypeDeclaration(childType) ||
                    Checker.isStatement(childType)) break;
            nodeTypes.addAll(readAllNodeTypes(child));
        }
        return nodeTypes;
    }
    public static List<AbstractSearcher> templateMatch(Set<String> patterns){
        List<AbstractSearcher> res = new ArrayList<>();
        ChangedCodeAST astFactory = new ChangedCodeAST();
        Pair<List<Pair<ITree,String>>,List<Pair<ITree,String>>> pair = astFactory.getInstance();

        Pair<ITree, String> buggyTree = null;
        Pair<ITree, String> fixedTree = null;
        for (int i = 0; i < pair.getSecond().size() || i < pair.getFirst().size() ; i++) {

            if (i >= pair.getFirst().size()) { //Insert
                fixedTree = pair.getSecond().get(i);
                AbstractSearcher as = templateMatchSingle(fixedTree,astFactory.getFixedCompilationUnit(),patterns);
                res.add(as);
            } else if (i >= pair.getSecond().size()) {//Delete
                buggyTree = pair.getFirst().get(i);

                AbstractSearcher as = templateMatchSingle(buggyTree,astFactory.getBuggyCompilationUnit(),patterns);
                res.add(as);
            } else {   //Mutate
                buggyTree = pair.getFirst().get(i);
                fixedTree = pair.getSecond().get(i);

                Pair<AbstractSearcher,AbstractSearcher> as = templateMatchDouble(buggyTree, fixedTree, astFactory.getBuggyCompilationUnit(),
                                    astFactory.getFixedCompilationUnit(),patterns);

                res.add(as.getFirst());
                res.add(as.getSecond());
            }
        }

        return res;
    }

    public static AbstractSearcher templateMatchSingle( Pair<ITree,String> treePair, CompilationUnit compilationUnit,Set<String> patterns){

        ITree fixedTree = treePair.getFirst();
        if(Checker.isIfStatement(fixedTree.getType())  ) {  //|| Checker.isInfixExpression(fixedTree.getType())

            int childrenSum = fixedTree.getChildren().size();
            int blockChildSum = 0;
            for (ITree child : fixedTree.getChildren()) {
                if (Checker.isBlock(child.getType())) {
                    blockChildSum += 1;
                }
            }
            int thenBlockIndex = childrenSum - blockChildSum;
            ITree thenBlock = fixedTree.getChild(thenBlockIndex);
            ITree lastChild = thenBlock.getChild(thenBlock.getChildren().size() - 1);
            //类型：方法提前返回
            if(Checker.isReturnStatement(lastChild.getType()) ) {
                EarlyReturnSearcher cs = new EarlyReturnSearcher(new File(Configuration.fixedFilePath),
                        compilationUnit, Configuration.fixedLineNum);
                cs.setMethodRootTree(cs.traverseParentNodeToMethodDeclaration(fixedTree)) ;

                cs.searchStatementsBehind(fixedTree);
                patterns.add("EarlyReturn");
                return cs;
            }
            else{
                InsertIfStatementSearcher cs = new InsertIfStatementSearcher(new File(Configuration.fixedFilePath),
                        compilationUnit, Configuration.fixedLineNum);
                cs.setMethodRootTree(cs.traverseParentNodeToMethodDeclaration(fixedTree)) ;
                cs.searchInnerStatements(fixedTree);
                patterns.add("InsertIfStatement");
                return cs;
            }
        }
        else if (Checker.isMethodDeclaration(fixedTree.getType())) {
            MethodDeclaration method = (MethodDeclaration) fixedTree;
            for (Object modifier : method.modifiers()) {
                if (modifier instanceof Annotation) {
                    Annotation annotation = (Annotation) modifier;
                    if (annotation instanceof MarkerAnnotation) {
                        MarkerAnnotation ma = (MarkerAnnotation) annotation;
                        if ("Override".equals(ma.getTypeName().getFullyQualifiedName())) {
                            MethodOverrideSearcher mos = new MethodOverrideSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
                            mos.setMethodRootTree(mos.traverseParentNodeToMethodDeclaration(fixedTree)) ;

                            mos.searchParentClass(fixedTree);
                            patterns.add("MethodOverride");
                            return mos;
                        }
                    }
                    // 还有NormalAnnotation、SingleMemberAnnotation，可根据实际需要添加判断
                }
            }
            AbstractSearcher as = new AbstractSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
            as.setMethodRootTree(as.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            return as;
        } else if(Checker.isAssignment(fixedTree.getType()) ){
            AssignmentSearcher as = new AssignmentSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
            as.setMethodRootTree(as.traverseParentNodeToMethodDeclaration(fixedTree)) ;

            as.searchAssignment(fixedTree);
            patterns.add("OtherStatement"); //Assignment
            return as;
        }
        else if (Checker.isMethodInvocation(fixedTree.getType())) {
            InsertMethodInvocationSearcher imis = new InsertMethodInvocationSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
            imis.setMethodRootTree(imis.traverseParentNodeToMethodDeclaration(fixedTree)) ;

            imis.searchMethodInvocation(fixedTree);
            patterns.add("OtherStatement");  //InsertMethodInvocation
            return imis;
        }
        else if(Checker.isReturnStatement(fixedTree.getType()) ){
            ReturnStatementSearcher rss = new ReturnStatementSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum) ;
            rss.setMethodRootTree(rss.traverseParentNodeToMethodDeclaration(fixedTree)) ;

            rss.searchReturnStatement(fixedTree);
            patterns.add("OtherStatement"); //ReturnStatement
            return rss;
        }
        else if(Checker.isSwitchCase(fixedTree.getType()) ){
            SwitchCaseSearcher rss = new SwitchCaseSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum) ;
            rss.setMethodRootTree(rss.traverseParentNodeToMethodDeclaration(fixedTree)) ;

            rss.searchSwitchCase(fixedTree);
            patterns.add("InsertIfStatement"); //SwitchCase
            return rss;
        }
        else if(Checker.isForStatement(fixedTree.getType())){
            ForStatementSearcher fss = new ForStatementSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
            fss.setMethodRootTree(fss.traverseParentNodeToMethodDeclaration(fixedTree));

            fss.searchForStatement(fixedTree);
            patterns.add("InsertIfStatement");  //ForStatement
            return fss;
        }
        else if (Checker.isWhileStatement(fixedTree.getType())){
            WhileStatementSearcher wss = new WhileStatementSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
            wss.setMethodRootTree(wss.traverseParentNodeToMethodDeclaration(fixedTree));

            wss.searchWhileStatement(fixedTree);
            patterns.add("InsertIfStatement");  //WhileStatement
            return wss;
        }
        else if (Checker.isTryStatement(fixedTree.getType())){
            WhileStatementSearcher wss = new WhileStatementSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
            wss.setMethodRootTree(wss.traverseParentNodeToMethodDeclaration(fixedTree));
            wss.searchWhileStatement(fixedTree);
            patterns.add("AddTryStatement");
            return wss;
        }

        else {
            AbstractSearcher cs = new AbstractSearcher(new File(Configuration.fixedFilePath),compilationUnit, Configuration.fixedLineNum);
            cs.setMethodRootTree(cs.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            return cs;
        }
    }

    public static Pair<AbstractSearcher,AbstractSearcher> templateMatchDouble( Pair<ITree,String> buggyTreePair, Pair<ITree,String> fixedTreePair,
                              CompilationUnit buggyCompilationUnit,CompilationUnit fixedCompilationUnit,Set<String> patterns){

        ITree buggyTree = buggyTreePair.getFirst();
        ITree fixedTree = fixedTreePair.getFirst();
        //两个都是if语句
        if(Checker.isIfStatement(buggyTree.getType()) && Checker.isIfStatement(fixedTree.getType())) {
            ITree buggyInfixTree = buggyTree.getChildren().get(0);
            ITree fixedInfixTree = fixedTree.getChildren().get(0);

            if(buggyInfixTree.getChildren().size() == fixedInfixTree.getChildren().size() ){
                List<ITree> buggyParts = buggyInfixTree.getChildren();
                List<ITree> fixedParts = fixedInfixTree.getChildren();
                for (int j = 0; j < fixedParts.size(); j++) {
                    ITree buggyPart = buggyParts.get(j);
                    ITree fixedPart = fixedParts.get(j);
                    //如果存在不相等的子节点,则为修改处
                    //类型：新增/删除 条件分支 里的if语句
                    if(!buggyPart.hasSameTypeAndLabel(fixedPart)) {
                        //修改的不是operator,则一定是修改的expression
                        Pair<AbstractSearcher,AbstractSearcher> res = initMutateConditionSearch(buggyTree,fixedTree,buggyCompilationUnit,fixedCompilationUnit);
                        patterns.add("MutateCondition");
                        return res;
                    }
                }
                AbstractSearcher fcs = new AbstractSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
                fcs.setMethodRootTree(fcs.traverseParentNodeToMethodDeclaration(fixedTree)) ;
                AbstractSearcher bcs = new AbstractSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
                bcs.setMethodRootTree(bcs.traverseParentNodeToMethodDeclaration(buggyTree)) ;
                var res = new Pair<>(fcs,bcs);
                return res;
            }
            //否则，条件数量发生变化，仍然是修改条件语句
            else{
                Pair<AbstractSearcher,AbstractSearcher> res = initMutateConditionSearch(buggyTree,fixedTree,buggyCompilationUnit,fixedCompilationUnit);
                patterns.add("MutateCondition");
                return res;
            }
        }
        //fixed版本是if语句,但buggy版本没有if语句,说明是新增
        else if(Checker.isIfStatement(buggyTree.getType()) || Checker.isIfStatement(fixedTree.getType()) ){
            InsertIfStatementSearcher cs =  new InsertIfStatementSearcher(new File(Configuration.fixedFilePath),
                                                        fixedCompilationUnit, Configuration.fixedLineNum);
            cs.setMethodRootTree(cs.traverseParentNodeToMethodDeclaration(fixedTree)) ;

            cs.searchInnerStatements(fixedTree);

            AbstractSearcher bcs = new AbstractSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bcs.setMethodRootTree(bcs.traverseParentNodeToMethodDeclaration(buggyTree)) ;

            patterns.add("InsertIfStatement");
            return new Pair<>(cs,bcs);
        }
        //buggy和fixed版本都是赋值语句
        else if(Checker.isAssignment(buggyTree.getType()) && Checker.isAssignment(fixedTree.getType())){
            AssignmentSearcher fas = new AssignmentSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            fas.setMethodRootTree(fas.traverseParentNodeToMethodDeclaration(fixedTree)) ;

            AssignmentSearcher bas = new AssignmentSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bas.setMethodRootTree(bas.traverseParentNodeToMethodDeclaration(buggyTree)) ;

            bas.searchAssignment(buggyTree);
            fas.searchAssignment(fixedTree);
            Assignment buggyAssignment = (Assignment) buggyTree.getASTNode();
            Assignment fixedAssignment = (Assignment) fixedTree.getASTNode();

            if(!buggyAssignment.getRightHandSide().equals(fixedAssignment.getRightHandSide())) {
                bas.searchStatementsUseRHS(buggyTree);
                patterns.add("MutateAssignmentRHS");
            }
            //patterns.add("Assignment");
            return new Pair<>(fas,bas);
        }
        //buggyTree == fixedTree,所以对应类型：移动语句位置,由于方法getAstNodeBoundary是AbstractSearcher的实例方法，、
        // 因此先实例化，再在内部判断是否真正为移动语句（即位置是否不同）
        else if (buggyTree.hasSameTypeAndLabel(fixedTree)) {
            MoveStatementSearcher fms = new MoveStatementSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            MoveStatementSearcher bms = new MoveStatementSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bms.setMethodRootTree(fms.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            fms.setMethodRootTree(bms.traverseParentNodeToMethodDeclaration(buggyTree)) ;
            bms.searchMoveStatement(buggyTree);
            fms.searchMoveStatement(fixedTree);
            bms.searchInnerStatements(buggyTree,fixedTree);
            fms.searchInnerStatements(fixedTree,fixedTree);
            patterns.add("MoveStatement");
            return new Pair<>(bms,fms);
        }
        //buggy和fixed版本都是return语句，则需要进一步判断。
        else if(Checker.isReturnStatement(buggyTree.getType()) && Checker.isReturnStatement(fixedTree.getType())){
            ReturnStatementSearcher frss = new ReturnStatementSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum) ;
            frss.setMethodRootTree(frss.traverseParentNodeToMethodDeclaration(fixedTree)) ;

            frss.searchReturnStatement(fixedTree);
            ReturnStatementSearcher brss = new ReturnStatementSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum) ;
            brss.setMethodRootTree(brss.traverseParentNodeToMethodDeclaration(buggyTree)) ;

            brss.searchReturnStatement(buggyTree);
            patterns.add("OtherStatement");
            return new Pair<>(frss,brss);

        }
        //类型：对象实例创建
        else if (buggyTree.getLabel().contains("Instance") && fixedTree.getLabel().contains("Instance")) {
            ClassInstanceCreationSearcher bcics = new ClassInstanceCreationSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            ClassInstanceCreationSearcher fcics = new ClassInstanceCreationSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            bcics.searchCreatedClasses(buggyTree);
            fcics.searchCreatedClasses(fixedTree);
            patterns.add("ClassInstanceCreation");
            return new Pair<>(bcics,fcics);
        }
        else if(Checker.isAssignment(buggyTree.getType()) && Checker.isMethodInvocation(fixedTree.getType())){
            AssignmentSearcher as = new AssignmentSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            as.setMethodRootTree(as.traverseParentNodeToMethodDeclaration(buggyTree)) ;
            as.searchAssignment(buggyTree);
            MethodInvocationSearcher ms = new MethodInvocationSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            ms.setMethodRootTree(ms.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            ms.searchMethodInvocation(fixedTree);
            patterns.add("OtherStatement");
            return new Pair<>(as,ms);
        }else if(Checker.isVariableDeclarationStatement(buggyTree.getType()) && Checker.isVariableDeclarationStatement(fixedTree.getType())){
            VariableDeclarationStatementSearcher vds1 = new VariableDeclarationStatementSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            vds1.setMethodRootTree(vds1.traverseParentNodeToMethodDeclaration(fixedTree));
            vds1.searchVariableDeclaration(fixedTree);
            VariableDeclarationStatementSearcher vds2 = new VariableDeclarationStatementSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            vds2.setMethodRootTree(vds2.traverseParentNodeToMethodDeclaration(buggyTree));
            patterns.add("OtherStatement"); //
            vds2.searchVariableDeclaration(buggyTree);
            return new Pair<>(vds1,vds2);
        }
        else if(Checker.isMethodInvocation(buggyTree.getType()) && Checker.isMethodInvocation(fixedTree.getType())){
            MethodInvocationSearcher fmmis = new MethodInvocationSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            fmmis.setMethodRootTree(fmmis.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            fmmis.searchMethodInvocation(fixedTree);
            MethodInvocationSearcher bmmis = new MethodInvocationSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bmmis.setMethodRootTree(bmmis.traverseParentNodeToMethodDeclaration(buggyTree)) ;
            bmmis.searchMethodInvocation(buggyTree);
            patterns.add("OtherStatement");
            return new Pair<>(fmmis,bmmis);
        }else if(Checker.isSuperMethodInvocation(buggyTree.getType()) && Checker.isSuperMethodInvocation(fixedTree.getType())){
            SuperMethodInvocationSearcher fmmis = new SuperMethodInvocationSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            fmmis.setMethodRootTree(fmmis.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            fmmis.searchSuperMethodInvocation(fixedTree);
            SuperMethodInvocationSearcher bmmis = new SuperMethodInvocationSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bmmis.setMethodRootTree(bmmis.traverseParentNodeToMethodDeclaration(buggyTree)) ;
            bmmis.searchSuperMethodInvocation(buggyTree);
            patterns.add("OtherStatement");//SuperMethodInvocation
            return new Pair<>(fmmis,bmmis);
        }else if(Checker.isSuperConstructorInvocation(buggyTree.getType()) && Checker.isSuperConstructorInvocation(fixedTree.getType())){
            SuperConstructorInvocationSearcher fmmis = new SuperConstructorInvocationSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            fmmis.setMethodRootTree(fmmis.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            fmmis.searchSuperConstructorInvocation(fixedTree);
            SuperConstructorInvocationSearcher bmmis = new SuperConstructorInvocationSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bmmis.setMethodRootTree(bmmis.traverseParentNodeToMethodDeclaration(buggyTree)) ;
            bmmis.searchSuperConstructorInvocation(buggyTree);
            patterns.add("OtherStatement"); //SuperConstructorInvocation
            return new Pair<>(fmmis,bmmis);
        }
        //类型2：修改条件表达式 里的for
        else if(Checker.isForStatement(buggyTree.getType()) && Checker.isForStatement(fixedTree.getType())){
            ForStatementSearcher ffss = new ForStatementSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            ffss.setMethodRootTree(ffss.traverseParentNodeToMethodDeclaration(fixedTree));
            ffss.searchForStatement(fixedTree);
            ForStatementSearcher bfss = new ForStatementSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bfss.setMethodRootTree(bfss.traverseParentNodeToMethodDeclaration(buggyTree));
            bfss.searchForStatement(buggyTree);
            if(!buggyTree.getChild(1).equals(fixedTree.getChild(1))){
                ffss.searchForStatement(fixedTree);
                bfss.searchOuterStatement(buggyTree);
            }
            patterns.add("MutateCondition");
            return new Pair<>(ffss,bfss);
        }
        //类型2：修改条件表达式 里的while
        else if(Checker.isWhileStatement(buggyTree.getType()) && Checker.isWhileStatement(fixedTree.getType())){
            WhileStatementSearcher ffss = new WhileStatementSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            ffss.setMethodRootTree(ffss.traverseParentNodeToMethodDeclaration(fixedTree));
            ffss.searchWhileStatement(fixedTree);
            WhileStatementSearcher bfss = new WhileStatementSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bfss.setMethodRootTree(bfss.traverseParentNodeToMethodDeclaration(buggyTree));
            bfss.searchWhileStatement(buggyTree);
            if(!buggyTree.getChild(1).equals(fixedTree.getChild(1))){
                ffss.searchOuterStatement(fixedTree);
                bfss.searchOuterStatement(buggyTree);
            }
            patterns.add("MutateCondition");
            return new Pair<>(ffss,bfss);
        }
        else{
            AbstractSearcher fcs = new AbstractSearcher(new File(Configuration.fixedFilePath),fixedCompilationUnit, Configuration.fixedLineNum);
            fcs.setMethodRootTree(fcs.traverseParentNodeToMethodDeclaration(fixedTree)) ;
            AbstractSearcher bcs = new AbstractSearcher(new File(Configuration.buggyFilePath),buggyCompilationUnit, Configuration.buggyLineNum);
            bcs.setMethodRootTree(bcs.traverseParentNodeToMethodDeclaration(buggyTree)) ;
            var res = new Pair<>(fcs,bcs);
            return res;
        }
    }

/*    private static Pair<AbstractSearcher,AbstractSearcher> initInsertIfSearch(ITree buggyTree, ITree fixedTree,
                                                                              CompilationUnit buggyCompilationUnit, CompilationUnit fixedCompilationUnit){
        InsertIfStatementSearcher fcs =  new InsertIfStatementSearcher(
                new File(Configuration.fixedFilePath),fixedCompilationUnit,
                Configuration.fixedLineNum);
        fcs.setMethodRootTree(fcs.traverseParentNodeToMethodDeclaration(fixedTree)) ;
        //修改if语句对应的额外实体信息
        fcs.searchInnerStatements(fixedTree);

        InsertIfStatementSearcher bcs =  new InsertIfStatementSearcher(
                new File(Configuration.buggyFilePath),buggyCompilationUnit,
                Configuration.buggyLineNum);
        bcs.setMethodRootTree(bcs.traverseParentNodeToMethodDeclaration(buggyTree)) ;

        bcs.searchInnerStatements(buggyTree);

        return new Pair<>(fcs,bcs);
    }*/

    private static Pair<AbstractSearcher,AbstractSearcher> initMutateConditionSearch(ITree buggyTree, ITree fixedTree,
                                                                                     CompilationUnit buggyCompilationUnit, CompilationUnit fixedCompilationUnit){
        MutateConditionSearcher fcs =  new MutateConditionSearcher(
                new File(Configuration.fixedFilePath),fixedCompilationUnit,
                Configuration.fixedLineNum);
        fcs.setMethodRootTree(fcs.traverseParentNodeToMethodDeclaration(fixedTree)) ;
        //修改if语句对应的额外实体信息
        fcs.searchOuterIfStatement(fixedTree);
        MutateConditionSearcher bcs =  new MutateConditionSearcher(
                new File(Configuration.buggyFilePath),buggyCompilationUnit,
                Configuration.buggyLineNum);
        bcs.setMethodRootTree(bcs.traverseParentNodeToMethodDeclaration(buggyTree)) ;
        bcs.searchOuterIfStatement(buggyTree);

        return new Pair<>(fcs,bcs);
    }
}
