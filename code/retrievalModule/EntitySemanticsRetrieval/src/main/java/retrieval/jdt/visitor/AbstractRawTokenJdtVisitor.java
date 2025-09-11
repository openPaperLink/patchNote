package retrieval.jdt.visitor;


import org.eclipse.jdt.core.dom.ASTNode;


public abstract class AbstractRawTokenJdtVisitor extends AbstractJdtVisitor {

    public AbstractRawTokenJdtVisitor() {
        super();
    }

    @Override
    protected void pushNode(ASTNode n, String label) {
        push(null,0, "", label, n.getStartPosition(), n.getLength());
    }

}
