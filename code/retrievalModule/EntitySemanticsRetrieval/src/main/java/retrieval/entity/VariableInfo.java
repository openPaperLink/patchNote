package retrieval.entity;

import lombok.Data;

import java.util.Objects;

@Data
public class VariableInfo {
    private String variableName;
    private String variableDeclaration;
    private String variableAnnotation;

    public VariableInfo(String name, String declaration, String comment) {
        this.variableName = name;
        this.variableDeclaration = declaration;
        this.variableAnnotation = comment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName,variableDeclaration,variableAnnotation);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableInfo info = (VariableInfo) o;
        return Objects.equals(variableName, info.variableName) &&
                Objects.equals(variableAnnotation, info.variableAnnotation) &&
                Objects.equals(variableDeclaration, info.variableDeclaration);
    }
}
