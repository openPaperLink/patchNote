package retrieval.entity;

import lombok.Data;

import java.util.Objects;

@Data
public class MethodInfo {
    private String methodName;
    private String methodBody;
    private String methodAnnotation;
    private String classBelongingTo = null;

    public MethodInfo(String name, String declaration, String comment,String classBelongingTo) {
        this.methodName = name;
        this.methodBody = declaration;
        this.methodAnnotation = comment;
        this.classBelongingTo = classBelongingTo;
    }
    public MethodInfo(String name, String declaration, String comment) {
        this.methodName = name;
        this.methodBody = declaration;
        this.methodAnnotation = comment;
    }
    @Override
    public int hashCode() {
        return Objects.hash(methodName, methodBody, methodAnnotation, classBelongingTo);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo info = (MethodInfo) o;
        return Objects.equals(methodName, info.methodName) &&
                Objects.equals(methodBody, info.methodBody) &&
                Objects.equals(methodAnnotation, info.methodAnnotation) &&
                Objects.equals(classBelongingTo, info.classBelongingTo);
    }

}
