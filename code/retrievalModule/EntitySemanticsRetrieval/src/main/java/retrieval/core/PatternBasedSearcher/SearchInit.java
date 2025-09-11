package retrieval.core.PatternBasedSearcher;

import lombok.Data;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;


@Data
public class SearchInit extends AbstractSearcher{
    public SearchInit(File javaFile, CompilationUnit unit, Integer lineNum) {
        super(javaFile,unit, lineNum);
    }
}
