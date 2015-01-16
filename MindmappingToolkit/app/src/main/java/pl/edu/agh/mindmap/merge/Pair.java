package pl.edu.agh.mindmap.merge;

public class Pair<T> {
    private T left, right;

    public Pair() {
    }

    public Pair(T left, T right){
        this.left = left;
        this.right = right;
    }

    public Pair<T> setLeft(T left) {
        this.left = left;
        return this;
    }

    public Pair<T> setRight(T right) {
        this.right = right;
        return this;
    }

    public boolean isFull(){
        return left!=null && right!=null;
    }

    public T getLeft() {
        return left;
    }

    public T getRight() {
        return right;
    }
}
