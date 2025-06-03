package retrieval.entity;

import lombok.Data;

import java.util.Objects;

@Data
public class ClassInfo {
    private String className;
    private String classAnnotation;

    public ClassInfo(String name,  String comment) {
        this.className = name;
        this.classAnnotation = comment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className,classAnnotation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassInfo info = (ClassInfo) o;
        return Objects.equals(className, info.className) &&
                Objects.equals(classAnnotation, info.classAnnotation);
    }
}
