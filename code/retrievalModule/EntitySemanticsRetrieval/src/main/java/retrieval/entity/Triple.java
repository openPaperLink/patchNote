package retrieval.entity;

public class Triple<E1, E2, E3> {

    public final E1 firstElement;

    public final E2 secondElement;

    public final E3 thirdElement;
    public Triple(E1 e1, E2 e2, E3 e3) {
        this.firstElement = e1;
        this.secondElement = e2;
        this.thirdElement = e3;
    }

    public E1 getFirst() {
        return firstElement;
    }

    public E2 getSecond() {
        return secondElement;
    }

    public E3 getThird() {
        return thirdElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple<?, ?, ?> pair = (Triple<?, ?, ?>) o;

        if (!firstElement.equals(pair.firstElement)) return false;
        if (!secondElement.equals(pair.secondElement)) return false;
        return thirdElement.equals(pair.thirdElement);

    }

    @Override
    public int hashCode() {
        int result = firstElement.hashCode();
        result = 31 * result + secondElement.hashCode();
        result = 31 * result + thirdElement.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + getFirst().toString() + "," + getSecond().toString() + "," + getThird().toString() + ")";
    }

}
