package retrieval.core.PatternBasedSearcher;

import retrieval.core.config.Configuration;
import retrieval.core.utils.Checker;
import retrieval.core.utils.CodeParser;
import retrieval.jdt.tree.ITree;
import lombok.*;
import org.eclipse.jdt.core.dom.*;
import retrieval.core.utils.FileHelper;
import retrieval.entity.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ClassName: AbstractSearcher
 * Package: com.jsh.bishe.core.templateBasedSearcher
 * Description
 *
 * @Author:
 * @Create: 2024/7/19 - 17:15
 * @Version:
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AbstractSearcher {

    @Setter
    protected Integer lineNum = -1;
    @Setter
    //所要处理的java文件对象
    protected  File javaFile = null;
    protected String superClassName = null;

    @Getter
    protected ITree rootTree = null;
    @Setter
    //java文件所对应的CompilationUnit对象
    protected  CompilationUnit unit = null;
    //buggy代码所在方法的方法声明对应的树节点

    @Getter
    @Setter
    protected ITree methodRootTree = null;


    //查找到的buggy代码所对应的结构的树节点
    protected Set<Pair<ITree,String>> foundTestNodes = new HashSet<>();
    protected List<Pair<ITree, String>> foundCodeNodes = new ArrayList<>();
    protected Set<TestInfo> testInfos = new HashSet<>();
    //存放在不同过程中记录到的需要被查找变量名、方法名、类名等
    protected Set<String> variableNamesToSearch = new HashSet<>();
    //存放需要查找的方法，Pair的第一个元素为方法名，第二个元素为方法的参数个数
    protected Set<Pair<String,Integer>> methodNamesToSearch = new HashSet<>();
    protected Set<File> classesToSearch = new HashSet<>();

    //存放查找到的类和类注释、方法和变量的定义和注释
    protected List<ClassInfo> classAnnotations = new ArrayList<>();
    protected List<MethodInfo> methodInfos = new ArrayList<>();
    protected List<VariableInfo> variableInfos = new ArrayList<>();

    //存放本类所以来的类对应的java文件路径，包括本类所在包内的类以及import导入的该项目内的类
    protected List<File> dependedFiles = new ArrayList<>();

    //识别patch对应代码的上下文中所有的节点类型，以进行匹配；
    List<Integer> contextInfo = new ArrayList<>();
    @Getter
    String nodeString = null;

    public AbstractSearcher(File javaFile , CompilationUnit unit, Integer lineNum){
        this.javaFile = javaFile;
        this.unit = unit;
        this.lineNum = lineNum;
        this.classesToSearch.add(javaFile);
        Pair<ITree, CompilationUnit> ast = CodeParser.parseCode(javaFile);
        this.rootTree = ast.getFirst();

        TypeDeclaration typeDeclaration = null;
        for(ITree children: this.rootTree.getChildren()){
            if(Checker.isTypeDeclaration(children.getType())){

                typeDeclaration =  (TypeDeclaration) children.getASTNode();
                Type type = typeDeclaration.getSuperclassType();
                if(type != null){
                    this.superClassName = type.toString();
                }
            }
        }

    }

    public void searchTriggerTests(String bugid) throws IOException {
        String pid = bugid.split("-")[0];
        String bid = bugid.split("-")[1];
        FileHelper fileHelper = new FileHelper();
        String filePath = Configuration.projectBaseDir+"trigger_tests/"+pid+"/"+bid;
        String cont = fileHelper.readFileContent(filePath);
        for(String line : cont.split("\n")){
            if(line.startsWith("--- ")){
                String[] tmpStr = line.substring(4).split("::");
                String testFilePath = tmpStr[0].replace(".","/");
                String testMethod = tmpStr[1];
                String testPre =Configuration.testDir;
                if((pid.equals("Math") && Integer.parseInt(bid) > 84)){
                    testPre = "src/test/";
                }else if(pid.equals("Chart")) {
                    testPre = "tests/";
                }
                //有subproject的要在路径上加上subproject
                if(pid.equals("Gson")){
                    testPre = "gson/"+testPre;
                }
                testFilePath = Configuration.projectBaseDir+"source_program/"+pid+"/"+pid+"_"+bid+"_buggy/"+testPre+testFilePath+".java";
                File testFile = new File(testFilePath);

                //将buggy版本代码转换成AST
                Pair<ITree, CompilationUnit> buggyAst = CodeParser.parseCode(testFile);
                ITree rootTree = buggyAst.getFirst();
                var buggyCompilationUnit = buggyAst.getSecond();

                SearchInit si = new SearchInit(testFile,buggyCompilationUnit,-1);
                List<ITree> tmp = new ArrayList<>();
                si.searchTestAst(rootTree,testMethod,tmp);
                if(tmp.size() > 0){
                    ITree testMethodTree = tmp.get(0);
                    testInfos.add(new TestInfo(testMethod,((MethodDeclaration)testMethodTree.getASTNode()).getBody().toString()));
                }else{
                    String[] dir = testFilePath.split("/");
                    String javaFile = dir[dir.length-1];
                    String dirName = testFilePath.substring(0,testFilePath.length()-javaFile.length());
                    List<File> tests = FileHelper.getAllFiles(dirName,"java");
                    for(File file : tests){
                        Pair<ITree, CompilationUnit> Ast = CodeParser.parseCode(file);
                        ITree rTree = Ast.getFirst();
                        var buggyCU = Ast.getSecond();

                        SearchInit si1 = new SearchInit(testFile,buggyCompilationUnit,-1);
                        List<ITree> tmp1 = new ArrayList<>();
                        si.searchTestAst(rTree,testMethod,tmp1);
                        if(tmp1.size() > 0) {
                            ITree testMethodTree = tmp1.get(0);
                            testInfos.add(new TestInfo(testMethod, ((MethodDeclaration) testMethodTree.getASTNode()).getBody().toString()));
                            break;
                        }
                    }
                }
            }
        }
    }

    protected boolean isLiteral(ASTNode node){
        if(node instanceof BooleanLiteral) return true;
        if(node instanceof StringLiteral){return true;}
        if(node instanceof CharacterLiteral){return true;}
        if (node instanceof NullLiteral){return true;}
        if(node instanceof NumberLiteral){return true;}
        if(node instanceof TypeLiteral){return true;}
        return false;
    }

    public void searchTestAst(ITree tree,String methodName, List<ITree> res){
        List<ITree> children = tree.getChildren();

        for (ITree child : children) {

            if(Checker.isMethodDeclaration(child.getType())){
                String testName = "";
                //TODO:为什么某些特例对于 public void methodname的情况，会忽略void，导致methodname不是第三个子节点
                if(child.getChildren().size() > 2){
                    testName = child.getChild(2).toString();
                }else if(child.getChildren().size() == 2){
                    testName = child.getChild(1).toString();
                }else{
                    continue;
                }

                if(testName.contains(":")){
                    testName = testName.split(":")[1];
                    if(testName.equals(methodName)){
                        res.add(child);
                        break;
                    }
                }

            }else{
                searchTestAst(child,methodName,res);
            }
        }
    }

    protected boolean StamentContainsCertainName(ITree tree, String name){
        return false;
    }

    //查找buggy或fixed代码对应的节点
    public void searchCodeAst(ITree tree) {
        if(this.lineNum == -1){
            return;
        }

        List<ITree> children = tree.getChildren();
        if(children.size() == 0){
            return;
        }

        ITree lastChild = children.get(children.size()-1);
        int lastLine = getAstNodeBoundary(lastChild,this.unit).getSecond();
        if(lastLine < this.lineNum){
            return;
        }

        for (ITree child : children) {
            Pair<Integer,Integer> boundary =  getAstNodeBoundary(child,this.unit);
            int startLine = boundary.getFirst();
            int endLine = boundary.getSecond();

            if (startLine <= this.lineNum && this.lineNum <= endLine) {
                if (startLine == this.lineNum || endLine == this.lineNum) {//当前节点已经包含对应的buggy代码

                    if (Checker.isBlock(child.getType())) {
                        searchCodeAst(child);
                    } else {

                        if (!isRequiredAstNode(child)) {

                            if (child == null || child.getType() < 0) continue;
                            searchCodeAst(child);
                        }
                        else{

                            Pair<ITree, String> pair = new Pair<>(child, readCodeNode(child));
                            if (!foundCodeNodes.contains(pair)) {
                                foundCodeNodes.add(pair);
                            }
                        }
                    }
                } else {
                    searchCodeAst(child);
                }
            } else if (startLine > this.lineNum) {
                break;
            }
        }
        //获取buggy代码所在方法的方法声明节点，以便缩小后续处理的AST树大小
        this.methodRootTree = traverseParentNodeToMethodDeclaration(foundCodeNodes.get(0).getFirst());

    }

    //判断buggy line是否位于某个方法体/方法头，即方法声明的一部分，否则就是类的属性声明
    public void findMethodOrFiledDeclarationParent(ITree tree) {

        List<ITree> children = tree.getChildren();
        if(children.size() == 0){
            return;
        }
        ITree lastChild = children.get(children.size()-1);
        int lastLine = getAstNodeBoundary(lastChild,this.unit).getSecond();
        ITree firstChild = children.get(0);
        int firstLine = getAstNodeBoundary(firstChild,this.unit).getFirst();
        if(firstLine > this.lineNum || lastLine < this.lineNum){
            return;
        }
        for (ITree child : children) {
            Pair<Integer,Integer> boundary =  getAstNodeBoundary(child,this.unit);
            int startLine = boundary.getFirst();
            int endLine = boundary.getSecond();

            if (startLine <= this.lineNum && this.lineNum <= endLine) {//当前节点已经包含对应的buggy代码
                 if(Checker.isMethodDeclaration(child.getType()) ){

                     if(this.nodeString == null){
                        this.nodeString = ((MethodDeclaration)(child.getASTNode())).toString();
                     }
                 }else if (Checker.isFieldDeclaration(child.getType())){
                     if(this.nodeString == null){
                         this.nodeString = ((FieldDeclaration)(child.getASTNode())).toString();
                     }
                 }else if (Checker.isSingleVariableDeclaration(child.getType())){
                     if(this.nodeString == null){
                         this.nodeString = ((SingleVariableDeclaration)(child.getASTNode())).toString();
                     }
                 }
                 else{
                     findMethodOrFiledDeclarationParent(child);
                 }
            } else if (startLine > this.lineNum ) {
                break;
            }
        }



    }

    public MethodInfo getBuggyMethodInfo(){
        if(this.methodRootTree == null){
            return new MethodInfo("111","111","111");
        }
        var methodNode = (MethodDeclaration)this.methodRootTree.getASTNode();
        int index = this.unit.firstLeadingCommentIndex(methodNode);
        //存在注释
        var commentList =  unit.getCommentList();
        String methodComment = "This method has no comment";
        String methodBody = "";
        if (methodNode.getBody() == null){
            methodBody = "no method body";
        }else{
            methodBody = methodNode.getBody().toString();
        }
        if(index != -1){
            methodComment = commentList.get(index).toString();
            methodComment = methodComment.replace("*","").replace("/","");
        }
        MethodInfo mInfo = new MethodInfo(methodNode.getName().toString(),methodBody,methodComment);
        return mInfo;
    }

    private boolean isRequiredAstNode(ITree tree) {
        int astNodeType = tree.getType();
        if (Checker.isStatement(astNodeType)||
                Checker.isFieldDeclaration(astNodeType)
                ||Checker.isMethodDeclaration(astNodeType)
                ||Checker.isTypeDeclaration(astNodeType) //
                || Checker.isMethodInvocation(astNodeType) //单行方法调用
                || Checker.isSuperMethodInvocation(astNodeType)
                || Checker.isAssignment(astNodeType) //赋值语句
                || Checker.isPrefixExpression(astNodeType)
                || Checker.isPostfixExpression(astNodeType)
                || Checker.isInfixExpression(astNodeType)
        )
        {
            return true;
        }
        return false;
    }
    public String obtainCreatedClasses(ITree tree){
        //TODO
        return "";
    }
    public Pair<Integer,Integer> getAstNodeBoundary(ITree node, CompilationUnit fileUnit){
        int startPosition = node.getPos();
        int endPosition = startPosition + node.getLength();
        int startLine = fileUnit.getLineNumber(startPosition);
        int endLine = fileUnit.getLineNumber(endPosition);
        if (endLine == -1) endLine = fileUnit.getLineNumber(endPosition - 1);
        return new Pair<>(startLine, endLine);
    }

    /*
    * 首先使用CompilationUnit的getPackage()获取其所在的包
    * 根据报名首先获取同一包内的所有类
    * 然后使用CompilationUnit的imports()获取所导入的依赖包*/
    public void searchDependedClass(String project, String id){
        //showNodes();

        List<String> packages = new ArrayList<>();
        String thisPackageName = unit.getPackage().getName().toString().replace(".","/");


        //查找方法的时候应该首先查找bug所在java类内的方法
        thisJavaFile(project, id, thisPackageName);

        if(superClassName != null){

            superJavaFile(project, id, thisPackageName);
        }

        //最后查找其他类
        otherJavaFile(project, id, thisPackageName);

    }

    private void otherJavaFile(String project, String id, String thisPackageName) {
        List<String> packages = new ArrayList<>();
        String packagePath = FileHelper.pathToPackage("f", project, id, thisPackageName);
        packages.add(packagePath);
        String packagePrefix = null;
        if(thisPackageName.split("/").length > 1){
            packagePrefix = thisPackageName.split("/")[0] + "/" + thisPackageName.split("/")[1];
        }else{
            packagePrefix = thisPackageName.split("/")[0];
        }


        List imports = unit.imports();
        for (Object anImport : imports) {
            String imp = anImport.toString().substring(7,anImport.toString().length()-2).replace(".","/");
            if(!imp.startsWith(packagePrefix)){
                continue;
            }
            if(imp.endsWith("/*")){
                packages.add(imp.substring(0, imp.length()-2));
            }else{
                String filePath = FileHelper.pathToPackage("b", project, id,imp + ".java" );
                File file = new File(filePath);
                dependedFiles.add( file );
            }
        }
        for (String aPackage : packages) {

            List<File> files= FileHelper.getAllFiles(aPackage,"java");
            if (files != null) {
                dependedFiles.addAll(files);
            }
        }
    }

    private void thisJavaFile(String project, String id,  String thisPackageName) {
        String[] tmp = Configuration.buggyFilePath.split("/");
        String thisJavaClassName = tmp[tmp.length-1];
        String thisJavaFile = thisPackageName +"/"+thisJavaClassName;
        String thisJavaFilePath = FileHelper.pathToPackage("f", project, id,thisJavaFile );
        File file = new File(thisJavaFilePath);
        dependedFiles.add( file );
    }

    private void superJavaFile(String project, String id,  String thisPackageName) {


        List<String> packages = new ArrayList<>();
        String packagePath = FileHelper.pathToPackage("f", project,id,thisPackageName );
        packages.add(packagePath);

        List imports = unit.imports();
        boolean found = false;
        for (Object anImport : imports) {
            String imp = anImport.toString().substring(7,anImport.toString().length()-2).replace(".","/");
//            if(!imp.startsWith(packagePrefix)){
//                continue;
//            }
            //TODO:除了 1.在子类与父类在同一包 2.用import xxx.SuperClass的方式精准导入的方式外，还有3.包含在import xxx.*的包里
            if(imp.endsWith("/"+this.superClassName)){//情况2
                String thisJavaFilePath = FileHelper.pathToPackage("f", project, id,imp );
                File file = new File(thisJavaFilePath);
                dependedFiles.add( file );

                found = true;
                break;
            }else if(imp.endsWith("/*")){
                if(imp.contains("/java/")){ //有可能是java官方的库
                    continue;
                }
                String packageP = FileHelper.pathToPackage("f", project,id,imp.substring(0, imp.length()-2) );
                packages.add(packageP);
            }
        }
        if(!found){//没有2的情况出现，那么就是1或者3，在import导入的包里或者子类所在包里
            for (String aPackage : packages) {
                List<File> files= FileHelper.getAllFiles(aPackage,"java");
                if(files == null){
                    continue;
                }

                for (File file : files) {
                    if (file.getName().equals(this.superClassName+".java")) {

                        dependedFiles.add(file);
                    }
                }
            }
        }



    }


   /* //沿着树结构向上回溯,直到找到Block的起始位置
    protected ITree traverseParentNode(ITree tree) {
        ITree parent = tree.getParent();
        if (parent == null) return null;
        if (!isRequiredAstNode(parent)) {
            parent = traverseParentNode(parent);
        }
        return parent;
    }*/

    //沿着树结构向上回溯,知道找到方法的起始位置,即方法声明处
    public ITree traverseParentNodeToMethodDeclaration(ITree tree) {
        ITree parent = tree.getParent();
        if (parent == null) return null;
        if (!Checker.isMethodDeclaration(parent.getType())) {
            parent = traverseParentNodeToMethodDeclaration(parent);
        }
        return parent;
    }

    protected String readCodeNode(ITree codeNode) {
        String javaFileContent = FileHelper.readFile(this.javaFile);
        int startPos = codeNode.getPos();
        int endPos = startPos + codeNode.getLength();
        return javaFileContent.substring(startPos, endPos);
    }

    public void  searchClassComments(){

        String classComment = "This class has no comment\n";
        for (File classToSearch : classesToSearch) {
            int index = -1;
            Pair<ITree,CompilationUnit> res = CodeParser.parseCode(classToSearch);
            ITree rootTree = res.getFirst();
            CompilationUnit fileUnit = res.getSecond();
            var commentList = fileUnit.getCommentList();
            for (ITree child : rootTree.getChildren()) {
                if(Checker.isTypeDeclaration(child.getType())){

                    String[] parts = classToSearch.toString().split("\\\\");
                    String className = parts[parts.length - 1];

                    TypeDeclaration typeDecl = (TypeDeclaration) child.getASTNode();
                    index = fileUnit.firstLeadingCommentIndex(typeDecl);
                    //方法没有注释
                    if(index != -1){
                        classComment = commentList.get(index).toString();
                    }
                    ClassInfo cInfo = new ClassInfo(className,classComment);
                    this.classAnnotations.add(cInfo);
                }
            }
        }
    }


    public void searchMethodDefinitionAndComments() {
        for (Pair<String,Integer> method : methodNamesToSearch) {

            for (File dependedFile : dependedFiles) {
                if(!dependedFile.exists()){
                    continue;
                }
                Pair<ITree,CompilationUnit> res = CodeParser.parseCode(dependedFile);
                ITree rootTree = res.getFirst();
                CompilationUnit fileUnit = res.getSecond();
                String className = dependedFile.getName().replace(".java","");
                findMethodDeclarationAndGetCodeAndComments(rootTree,method,fileUnit,dependedFile,className);
            }
        }
    }

    private void findMethodDeclarationAndGetCodeAndComments(ITree tree,Pair<String,Integer> method, CompilationUnit fileUnit, File dependedFile,String className){
        var commentList =  fileUnit.getCommentList();
        String methodComment = "This method has no comment";
        String methodBody = "";
        Integer index = -1;
        for (ITree child : tree.getChildren()) {
            index = -1;
            if(Checker.isIfStatement(child.getType())){
                continue;
            }
            if(Checker.isMethodDeclaration(child.getType())){
                var methodNode = (MethodDeclaration)child.getASTNode();
                Integer argNum = methodNode.parameters().size();
                if( methodNode.getName().toString().equals(method.getFirst()) && argNum.equals(method.getSecond())  ){

                    classesToSearch.add(dependedFile);
                    if (methodNode.getBody() == null){
                        continue;
                    }else{
                        methodBody = methodNode.getBody().toString();
                    }

                    index = fileUnit.firstLeadingCommentIndex(methodNode);
                    //存在注释
                    if(index != -1){
                        methodComment = commentList.get(index).toString();
                        methodComment = methodComment.replace("*","").replace("/","");
                    }
                    MethodInfo mInfo = new MethodInfo(method.getFirst(),methodBody,methodComment,className);
                    if(!this.methodInfos.contains(mInfo)){
                        this.methodInfos.add(mInfo);
                    }
                }
            }else{
                findMethodDeclarationAndGetCodeAndComments(child,method,fileUnit,dependedFile,className);
            }

        }
    }
    public void searchVariableDefinitionAndComments() {

        while(findVariableDefinitionAndCommentsInBuggyMethod(this.getMethodRootTree(),this.getLineNum(),this.getUnit())){

        }

        findVariableDefinitionAndCommentsCodeAstInArguments(this.getMethodRootTree(),this.getUnit());
        findVariableDefinitionAndCommentsCodeAstInClassFields(this.getRootTree(),this.getUnit());
    }

    private void findVariableDefinitionAndCommentsCodeAstInArguments(ITree tree,  CompilationUnit fileUnit){
        if(tree == null){
            return ;
        }
        MethodDeclaration methodDeclaration = (MethodDeclaration) tree.getASTNode();
        List<Object> params = methodDeclaration.parameters();
        List<String> paramStrings = new ArrayList<>();

        for(Object param : params){
            String[] tmp = param.toString().split(" ");
            paramStrings.add(tmp[tmp.length-1]);
        }
        var methodNode = (MethodDeclaration)tree.getASTNode();
        int index = fileUnit.firstLeadingCommentIndex(methodNode);
        String methodAnnotation = "";
        var commentList =  fileUnit.getCommentList();
        //存在注释
        if(index != -1){
            methodAnnotation = commentList.get(index).toString();
            methodAnnotation = methodAnnotation.replace("/*","").replace("*/","").replace("*","");

        }

        String[] tmp = methodAnnotation.split("\n");
        Object[] varNames = variableNamesToSearch.toArray();
        for(Object varNameO: varNames){
            String varName = (String) varNameO;
            if(paramStrings.contains(varName)){

                for(String s : tmp){
                    if(s.contains("@param") && s.contains(varName)){

                        methodAnnotation = s;
                    }
                }
                VariableInfo vInfo = new VariableInfo(varName,"this variable is one of the arguments of buggy method",methodAnnotation);
                if(!this.variableInfos.contains(vInfo)){
                    this.variableInfos.add(vInfo);
                }

            }
        }
    }

    private void findVariableDefinitionAndCommentsCodeAstInClassFields(ITree tree,CompilationUnit fileUnit){

        for(ITree child : tree.getChildren()){

            if(Checker.isFieldDeclaration(child.getType())){

                for(ITree childChild : child.getChildren()){

                    if(Checker.isVariableDeclarationFragment(childChild.getType())){

                        SimpleName name = (SimpleName) childChild.getChild(0).getASTNode();
                        String varName = name.toString();

                        if(variableNamesToSearch.contains(varName)){

                            FieldDeclaration fd = (FieldDeclaration)(child.getASTNode());
                            var frags = fd.fragments();


                            int index = fileUnit.firstLeadingCommentIndex(fd);
                            String varComment = "";
                            var commentList =  fileUnit.getCommentList();

                            if(index != -1){
                                varComment = commentList.get(index).toString();
                                varComment = varComment.replace("/*","").replace("*/","").replace("*","");
                            }

                            VariableInfo vInfo = new VariableInfo(varName,child.getASTNode().toString(),varComment);
                            if(!this.variableInfos.contains(vInfo)){
                                this.variableInfos.add(vInfo);
                            }
                            variableNamesToSearch.remove(varName);
                        }
                    }
                }


            }
            else if(Checker.isTypeDeclaration(child.getType())) {
                findVariableDefinitionAndCommentsCodeAstInClassFields(child,fileUnit);
            }
        }
    }


    private boolean findVariableDefinitionAndCommentsInBuggyMethod(ITree tree, Integer borderLineNum, CompilationUnit fileUnit){
        if(tree == null){

            return false;
        }
        var commentList =  fileUnit.getCommentList();
        Set<String> oldSet = new HashSet<>();
        oldSet.addAll(variableNamesToSearch);


        for (ITree child : tree.getChildren()) {
            Pair<Integer,Integer> boundary =  getAstNodeBoundary(child,this.unit);
            int firstLine = boundary.getFirst();
            String variableComment = "This variable has no comment\n";
            Integer index = -1;
            if(firstLine <= borderLineNum){
                if (Checker.isVariableDeclarationStatement(child.getType())) {
                    VariableDeclarationStatement stmt = (VariableDeclarationStatement) child.getASTNode();

                    ITree frag =  child.getChild(child.getChildren().size()-1);
                    ITree leftValue = frag.getChild(0);


                    String variableName = "";
                    if(Checker.isThisExpression(leftValue.getType())){
                        variableName = leftValue.getChild(1).getASTNode().toString();

                    }else if (Checker.isSimpleName(leftValue.getType())){
                        variableName = leftValue.getASTNode().toString();

                    }else if(Checker.isQualifiedName(leftValue.getType())){
                        variableName = leftValue.getChild(1).getASTNode().toString();

                    }

                    //当前变量是需要查找的变量
                    if(  variableNamesToSearch.contains(variableName)  ){

                        index = fileUnit.firstLeadingCommentIndex(stmt);
                        //方法没有注释
                        if(index != -1){
                            variableComment = commentList.get(index).toString();
                            variableComment = variableComment.replace("*","").replace("/","");
                        }
                        VariableInfo vInfo = new VariableInfo(variableName,stmt.toString(),variableComment);
                        if(!this.variableInfos.contains(vInfo)){
                            this.variableInfos.add(vInfo);
                        }
                        //已经找到，从待查找集合中删除
                        variableNamesToSearch.remove(variableName);
                        //查找变量声明语句，如果同时有初始化，则将等号右侧的变量或者方法调用分别加入到待查找集合
                        if(frag.getChildren().size() > 1){
                            ITree rightValue = frag.getChild(1);

                            processChild(rightValue);
                        }

                    }
                }else{
                    findVariableDefinitionAndCommentsInBuggyMethod(child, borderLineNum,unit);
                }
            }
        }
        //如果原本的集合包含现在所有结合的所有元素,那么说明经过这个这个查找过程，集合中没有加入新的元素，即不需要查找新的变量
        return !oldSet.containsAll(variableNamesToSearch);
    }

    protected void processForStatement(ITree tree){
        ITree infix = tree.getChild(1);
        processInfixExpression(infix);
    }

    protected void processWhileStatement(ITree tree){
        processChild(tree.getChild(0)); //第一个子节点就是while的循环条件

    }

    protected void processVariableDeclarationStatement(ITree tree){
        ITree frag =  tree.getChild(tree.getChildren().size()-1);
        if(frag.getChildren().size() > 1){
            ITree rightValue = frag.getChild(1);

            processChild(rightValue);
        }
    }
    protected void processClassInstanceCreation(ITree tree){
        ClassInstanceCreation cic = (ClassInstanceCreation) tree.getASTNode();
        Integer argNum = cic.arguments().size();
        String constructorName = tree.getChild(1).getASTNode().toString();
        this.methodNamesToSearch.add(new Pair<>(constructorName,argNum));
        for(int i = 2; i <  tree.getChildren().size(); i++){
            ITree child = tree.getChild(i);
            processChild(child);
        }
    }
    protected void processLiteral(ASTNode node){
        //Nothing to do
        return;
    }
    protected void processExpressionStatement(ITree tree){
        //TODO:
        for(ITree child : tree.getChildren()){
            processChild(child);
        }
    }
    protected void processIfStatementChildren(ITree tree) {
        for (ITree child : tree.getChildren()) {

            if(Checker.isInfixExpression(child.getType())){
                processInfixExpression(child);
            }else if(Checker.isMethodInvocation(child.getType())){
                processMethodInvocation(child);
            }
        }

    }

    protected void processCoditionalExpression(ITree tree){
        ConditionalExpression ce = (ConditionalExpression) tree.getASTNode();
        List<Expression> exs = new ArrayList<>();
        exs.add(ce.getExpression());
        exs.add( ce.getThenExpression());
        exs.add( ce.getElseExpression());
        for (int i = 0; i < exs.size(); i++) {
            Expression ex = exs.get(i);
            if(ex instanceof InfixExpression){
                processInfixExpression(tree.getChild( i));
            }else if(ex instanceof MethodInvocation){
                processMethodInvocation(tree.getChild(i));
            }else if(ex instanceof Name){
                processName(ex);
            }else{
                //TODO:补充完善条件
            }
        }

    }

    protected void processFieldAccess(ITree tree){
        for(ITree child : tree.getChildren()){
            processChild(child);
        }
    }

    protected void processArrayAccess(ITree tree) {
        for(ITree child : tree.getChildren()){
            processChild(child);
        }
    }

    protected void processSwitchCase(ITree tree){
        for(ITree child : tree.getChildren()){
            processChild(child);
        }
    }

    public void processReturnStatement(ITree tree){

        for(ITree ch : tree.getChildren()){
            processChild(ch);
        }

    }


    protected void processInfixExpression(ITree tree){
        processChild(tree.getChild(0));
        processChild(tree.getChild(tree.getChildren().size()-1));
    }
    /*protected void processInfixExpression(ITree tree) {
        InfixExpression infix = (InfixExpression) tree.getASTNode();
        InfixExpression.Operator op= infix.getOperator();
        List<Expression> operands = new ArrayList<>();
        operands.add(infix.getLeftOperand());
        operands.add(infix.getRightOperand());
        for (int i = 0; i < operands.size(); i++) {

            Expression operand = operands.get(i);

            if(operand instanceof InfixExpression){
                processInfixExpression(tree.getChild( i==1 ? 2 : i ));
            }else if(operand instanceof MethodInvocation){
                processMethodInvocation(tree.getChild(i==1 ? 2 : i));
            }else if(operand instanceof Name){
                processName(operand);
            }else if(operand instanceof FieldAccess){
                processFieldAccess(tree.getChild(i==1 ? 2 : i));
            }
            else if(operand instanceof ArrayAccess){
                processArrayAccess(tree.getChild(i==1 ? 2 : i));
            }
            else if(operand instanceof PrefixExpression){
                processPrefixExpression(tree.getChild(i==1 ? 2 : i));
            }else if(operand instanceof ConditionalExpression){
                processConditionalExpression(tree.getChild(i==1 ? 2 : i));
            }else if(operand instanceof )
            else{//需要判断该结构的子节点是不是infix或者method
                processIfStatementChildren(tree.getChild(i==1 ? 2 : i));
            }
        }
    }*/

    public void processThrowStatement(ITree tree){
        for(ITree ch : tree.getChildren()){
            processChild(ch);
        }
    }

    protected void processConditionalExpression(ITree tree){
        for(ITree child : tree.getChildren()){
            processChild(child);
        }
    }

    protected void processPrefixExpression(ITree tree) {
        ITree ex = tree.getChild(1);
        processChild(ex);
    }
    protected void processSwitchStatement(ITree tree)
    {
        for(ITree child : tree.getChildren()){
            processChild(child);
        }
    }

    protected void processBlock(ITree tree)
    {
        for(ITree child : tree.getChildren()){
            processChild(child);
        }
    }
    protected void processChild(ITree child) {
        if(Checker.isPrefixExpression(child.getType())){
            processPrefixExpression(child);
        }else if(Checker.isSimpleName(child.getType())){
            processName(child.getASTNode());
        }else if(Checker.isQualifiedName(child.getType())){
            processName(child.getASTNode());
        }else if(Checker.isFieldAccess(child.getType())){
            processFieldAccess(child);
        }else if(Checker.isInfixExpression(child.getType())){
            processInfixExpression(child);
        }else if(Checker.isArrayAccess(child.getType())){
            processArrayAccess(child);
        }else if(Checker.isMethodInvocation(child.getType() ) ){
            processMethodInvocation(child);
        }else if (Checker.isSuperMethodInvocation(child.getType())){
            processSuperMethodInvocation(child);
        }else if(Checker.isClassInstanceCreation(child.getType())){
            processClassInstanceCreation(child);
        }else if(Checker.isConditionalExpression(child.getType())){
            processConditionalExpression(child);
        }else if(Checker.isParenthesizedExpression(child.getType())){
            processParenthesizedExpression(child);
        }else if(Checker.isInstanceofExpression(child.getType())){
            processInstanceOfExpression(child);
        }else if (Checker.isArrayCreation(child.getType())){
            processArrayCreation(child);
        }else if(Checker.isCastExpression(child.getType())){
            processCastExpression(child);
        }else if(Checker.isSuperConstructorInvocation(child.getType())){
            processSuperConstructorInvocation(child);
        }else if(Checker.isBlock(child.getType())){
            processBlock(child);
        }
        else if(Checker.isLiteral(child.getType())){
            return;//不对字面量做任何操作，此处添加分支是为了避免抛异常
        }else if(Checker.isNohandle(child.getType())){
            return;
        }else if(Checker.isAssignment(child.getType())){
            processAssignment(child);
        }else if(Checker.isPostfixExpression(child.getType())){
            processPostfixExpression(child);
        }else if(Checker.isVariableDeclarationStatement(child.getType())){
            processVariableDeclarationStatement(child);
        }
        else if(Checker.isWhileStatement(child.getType())){
            processWhileStatement(child);
        } else if (Checker.isForStatement(child.getType())) {
            processForStatement(child);
        } else if (Checker.isIfStatement(child.getType())) {
            processIfStatementChildren(child);
        } else if (Checker.isExpressionStatement(child.getType())){
            processExpressionStatement(child);
        } else if (Checker.isReturnStatement(child.getType())){
            processReturnStatement(child);
        } else if (Checker.isThrowStatement(child.getType())) {
            processThrowStatement(child);
        } else{
            for(ITree childfren : child.getChildren()){
                processChild(childfren);
            }
        }
    }

    protected void processPostfixExpression(ITree tree) {
        ITree ex = tree.getChild(0);
        processChild(ex);
    }

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
            }
            else{
                throw new RuntimeException("Unsupported Node Type: " + side.getClass().getName());
            }
        }
    }
    protected void processInstanceOfExpression(ITree tree){
        //checkChildren(tree);
        processChild(tree.getChild(0));
        processChild(tree.getChild(2));

    }



    protected void processArgument(String arg){
        String argName = arg;
        variableNamesToSearch.add(argName);
    }

    protected void checkChildren(ITree tree){
//        System.out.println("children info of node:");
//        System.out.println(tree.getLabel());
//        for(ITree child : tree.getChildren()){
//            System.out.println(child.getType());
//            System.out.println(child.getLabel());
//        }
    }

    protected void processCastExpression(ITree tree){
        processChild(tree.getChild(1));
    }

    protected void processParenthesizedExpression(ITree tree){
        processChild(tree.getChild(0));
    }


    protected void processArrayCreation(ITree tree){
        for(int i = 1; i < tree.getChildren().size(); i++){
            ITree child = tree.getChild(i);
            processChild(child);
        }
    }

    protected void processSuperConstructorInvocation(ITree tree){
        SuperConstructorInvocation method = (SuperConstructorInvocation) tree.getASTNode();
        Integer argNum = method.arguments().size();
        this.methodNamesToSearch.add( new Pair<>(this.superClassName.toString(),argNum));
        List<?> arguments = method.arguments();
        if (arguments != null) {
            for (Object argument : arguments) {
                processArgument(argument.toString());
            }
        }
    }



    protected void processSuperMethodInvocation(ITree tree){
        SuperMethodInvocation method = (SuperMethodInvocation) tree.getASTNode();
        SimpleName methodName = method.getName();

        Integer argNum = method.arguments().size();
        this.methodNamesToSearch.add( new Pair<>(methodName.toString(), argNum));
        List<?> arguments = method.arguments();
        if (arguments != null) {
            for (Object argument : arguments) {
                processArgument(argument.toString());
            }
        }
    }

    protected void processMethodInvocation(ITree tree) {
        MethodInvocation method = (MethodInvocation) tree.getASTNode();
        Expression exp = method.getExpression();
//		List<?> typeArguments = node.typeArguments();
        //方法名加入列表，等待查找定义和注释
        SimpleName methodName = method.getName();
        //参数需要分类处理
        List<?> arguments = method.arguments();
        Integer argNum = 0;
        if (arguments != null) {
            argNum = arguments.size();
            for (Object argument : arguments) {
                processArgument(argument.toString());
            }
        }
        methodNamesToSearch.add(new Pair<String,Integer>(methodName.toString(),argNum));
        if(exp == null){

        }else if (exp instanceof MethodInvocation ) {

            for(ITree ch : tree.getChildren()){
                if(Checker.isMethodInvocation(ch.getType())){
                    processMethodInvocation(ch);
                }
            }

        }else if(exp instanceof Name){
            processName((Name)exp);
        }
    }

    protected void processName(ASTNode node) {
        Name name = (Name) node;
        //是简单名字
        if(name.isSimpleName()){
            //并且不是true或者false等固定字符
            if(!name.toString().equals("true") && !name.toString().equals("false")){
                variableNamesToSearch.add( ((SimpleName) name).toString() );
            }
            //变量前有修饰，如a.b的形式
        }else if (name.isQualifiedName()) {
            variableNamesToSearch.add( ((QualifiedName) name).getQualifier().toString() );
            variableNamesToSearch.add( ((QualifiedName) name).getName().toString() );
        }
    }


}
