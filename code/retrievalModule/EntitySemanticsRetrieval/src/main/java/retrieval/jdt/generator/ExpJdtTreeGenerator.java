package retrieval.jdt.generator;

import retrieval.jdt.visitor.AbstractJdtVisitor;
import retrieval.jdt.visitor.ExpJdtVisitor;

@Register(id = "java-jdt-exp")
public class ExpJdtTreeGenerator extends AbstractJdtTreeGenerator {
    @Override
    protected AbstractJdtVisitor createVisitor() {
        return new ExpJdtVisitor();
    }
}
