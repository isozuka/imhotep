package com.indeed.imhotep.shardmaster.utils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author kornerup
 */

public class IntervalTree<K extends Comparable<? super K>, V> {
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Set<V> values = new HashSet<>();

    public Set<V> getAllValues() {
        try {
            lock.readLock().lock();
            return values;
        } finally {
            lock.readLock().unlock();
        }
    }

    private class Interval implements Comparable <Interval> {
        final K start;
        final K end;
        final Set<V> values;
        final double priority;

        public Interval(K start, K end, Set<V> value){
            this.start = start;
            this.end = end;
            this.values = value;
            this.priority = Math.random();
        }

        @Override
        public int compareTo(Interval o) {
            int startCompare = this.start.compareTo(o.start);
            return startCompare == 0 ? this.end.compareTo(o.end) : startCompare;
        }
    }

    private class Node {
        K largestToTheRight;
        Interval interval;
        Node left;
        Node right;
    }

    private Node root;

    public void addInterval(K start, K end, V value){
        try {
            lock.writeLock().lock();
            values.add(value);
            if (root == null) {
                root = new Node();
                Set<V> values = new HashSet<>();
                values.add(value);
                root.interval = new Interval(start, end, values);
                root.largestToTheRight = end;
                return;
            }

            Set<V> values = new HashSet<>();
            values.add(value);
            Interval interval = new Interval(start, end, values);
            addInterval(root, interval, null, false);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<V> getValuesInRange(K start, K end){
        try {
            lock.readLock().lock();
            if (root == null) {
                return new HashSet<>();
            }
            return valuesInRange(root, start, end);
        } finally {
            lock.readLock().unlock();
        }
    }

    private K max(K first, K second, K third){
        return max(max(first, second), third);
    }

    private K max(K first, K second){
        return first.compareTo(second) > 0 ? first : second;
    }

    private Set<V> valuesInRange(Node current, K start, K end) {
        if(current == null) {
            return new HashSet<>();
        }
        if (current.interval.start.compareTo(end) > 0){
            return valuesInRange(current.left, start, end);
        }
        if (current.largestToTheRight.compareTo(start) < 0){
            return new HashSet<>();
        }
        Set<V> toReturn = valuesInRange(current.left, start, end);
        toReturn.addAll(valuesInRange(current.right, start, end));
        if(shouldTakeThisInterval(current.interval, start, end)) {
            toReturn.addAll(current.interval.values);
        }
        return toReturn;
    }

    private boolean shouldTakeThisInterval(Interval interval, K start, K end) {
        return interval.start.compareTo(end) <= 0 && interval.end.compareTo(start) >= 0;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void addInterval(Node current, Interval interval, Node previous, boolean prevMoveWasLeft) {
        if (current.interval.compareTo(interval) > 0) {
            if (current.left != null) {
                addInterval(current.left, interval, current, true);
            } else {
                Node temp = new Node();
                temp.interval = interval;
                temp.largestToTheRight = interval.end;
                current.left = temp;
            }
            if(current.left.interval.priority > current.interval.priority) {
                Node newParent = current.left;
                Node oldParent = current;
                Node grandparent = previous;
                Node newParentRight = newParent.right;
                if(oldParent == root) {
                    root = newParent;
                } else if (prevMoveWasLeft) {
                    grandparent.left = newParent;
                } else {
                    grandparent.right = newParent;
                }
                oldParent.left = newParentRight;
                newParent.right = oldParent;
                updateLargestToTheRight(oldParent);
                updateLargestToTheRight(newParent);
            }
        } else if (current.interval.compareTo(interval) < 0) {
            if (current.right != null) {
                addInterval(current.right, interval, current, false);
            } else {
                Node temp = new Node();
                temp.interval = interval;
                temp.largestToTheRight = interval.end;
                current.right = temp;
            }
            if(current.right.interval.priority > current.interval.priority) {
                Node newParent = current.right;
                Node oldParent = current;
                Node grandparent = previous;
                Node newParentLeft = newParent.left;
                if(oldParent == root){
                    root = newParent;
                } else if (prevMoveWasLeft) {
                    grandparent.left = newParent;
                } else {
                    grandparent.right = newParent;
                }
                oldParent.right = newParentLeft;
                newParent.left = oldParent;
                updateLargestToTheRight(oldParent);
                updateLargestToTheRight(newParent);
            }
        } else {
            current.interval.values.addAll(interval.values);
        }
        updateLargestToTheRight(current);
    }

    private void updateLargestToTheRight(Node current) {
        if(current.left == null && current.right == null) {
            current.largestToTheRight = current.interval.end;
        } else if (current.left == null) {
            current.largestToTheRight = max(current.interval.end, current.right.largestToTheRight);
        } else if (current.right == null) {
            current.largestToTheRight = max(current.interval.end, current.left.largestToTheRight);
        } else {
            current.largestToTheRight = max(current.interval.end, current.left.largestToTheRight, current.right.largestToTheRight);
        }
    }
}
