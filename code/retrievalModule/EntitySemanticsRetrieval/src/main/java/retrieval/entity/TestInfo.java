package retrieval.entity;

import lombok.Data;

import java.util.Objects;


@Data
public class TestInfo {
    private String testName;
    private String testBody;
    public TestInfo(String testName, String testBody) {
        this.testName = testName;
        this.testBody = testBody;
    }

    public boolean equals(TestInfo info) {
        if (this == info) return true;
        if (info == null || getClass() != info.getClass()) return false;
        if(this.testName.equals(info.getTestName()) && this.testBody.equals(info.getTestBody())) {
            return true;
        }
        return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(testName,testBody);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestInfo info = (TestInfo) o;
        return Objects.equals(testName, info.testName) &&
                Objects.equals(testBody, info.testBody);
    }
}
