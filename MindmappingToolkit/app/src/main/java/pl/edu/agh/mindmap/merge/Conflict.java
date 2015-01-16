package pl.edu.agh.mindmap.merge;


public class Conflict<T> {
    private T source, target, result;

    Conflict() {
    }

    Conflict(T source, T target, T result) {
        this.source = source;
        this.target = target;
        this.result = result;
    }

    /**
     * Returns element from source workbook. Do not modify this element while merging!!!
     */
    public T getSource() {
        return source;
    }

    /**
     * Returns element from target workbook. Do not modify this element while merging!!!
     */
    public T getTarget() {
        return target;
    }

    /**
     * Returns corresponding element in the merge result workbook. All changes should be applied to this element.
     */
    public T getResult() {
        return result;
    }

    Conflict<T> setSource(T source) {
        this.source = source;
        return this;
    }

    Conflict<T> setTarget(T target) {
        this.target = target;
        return this;
    }

    Conflict<T> setResult(T result) {
        this.result = result;
        return this;
    }
}
