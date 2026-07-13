package com.janboerman.invsee.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.Map.Entry;

public class UsernameTrie<V> {

    private final Node<V> root;

    public UsernameTrie(V rootValue) {
        this.root = new Node<>(new char[0], Maybe.just(rootValue), null);
    }

    public UsernameTrie() {
        this.root = new Node<>(new char[0], Maybe.nothing(), null);
    }

    public Maybe<V> insert(String username, V value) {
        return insert(username.toCharArray(), value);
    }

    public synchronized Maybe<V> insert(char[] username, V value) {
        Node<V> node = root.lookup(username);
        Maybe<V> oldValue = node.value;
        node.value = Maybe.just(value);
        return oldValue;
    }

    public Maybe<V> delete(String username) {
        return delete(username.toCharArray());
    }

    public synchronized Maybe<V> delete(char[] username) {
        Node<V> node = root.lookup(username);
        Maybe<V> oldValue = node.value;
        node.value = Maybe.nothing();
        node.cleanUp();
        return oldValue;
    }

    public Maybe<V> get(String username) {
        return get(username.toCharArray());
    }

    public synchronized Maybe<V> get(char[] username) {
        Node<V> node = root.lookup(username);
        Maybe<V> value = node.value;
        node.cleanUp();
        return value;
    }

    public void traverse(String prefix, BiConsumer<String, ? super V> consumer) {
        traverse(prefix.toCharArray(), (chars, v) -> consumer.accept(new String(chars), v));
    }

    public synchronized void traverse(char[] prefix, BiConsumer<char[], ? super V> consumer) {
        Node<V> node = root.lookup(prefix);
        node.traverse(consumer);
        node.cleanUp();
    }

    private static class Node<V> {

        private static final Comparator<Character> CHAR_COMPARATOR = (Character character1, Character character2) -> {
            char c1 = character1.charValue(), c2 = character2.charValue();
            if (Character.isLetter(c1) && Character.isLetter(c2)) {

                char l1 = Character.toLowerCase(c1), l2 = Character.toLowerCase(c2);
                if (l1 == l2) {

                    return Character.compare(c1, c2);
                } else {

                    return Character.compare(l1, l2);
                }
            } else if (Character.isLetter(c1)) {

                return 1;
            } else if (Character.isLetter(c2)) {

                return -1;
            } else {

                return Character.compare(c1, c2);
            }
        };

        private final char[] segment;

        private Maybe<V> value;

        private TreeMap<Character, Node<V>> children;

        private Node<V> parent;

        private Node(char[] segment, Maybe<V> value, Node<V> parent) {
            this(segment, value, null, parent);
        }

        private Node(char[] segment, Maybe<V> value, TreeMap<Character, Node<V>> children, Node<V> parent) {
            assert segment != null : "segment cannot be null";
            assert value != null : "value cannot be null";

            this.segment = segment;
            this.value = value;
            this.children = children;
            this.parent = parent;
        }

        private boolean isEmpty() {

            if (value.isPresent())
                return false;

            if (children == null)
                return true;
            for (Node<V> child : children.values())
                if (child != null && !child.isEmpty())
                    return false;

            return true;
        }

        private void cleanUp() {
            if (parent != null) {
                if (isEmpty()) {

                    parent.children.remove(segment[0]);
                    parent.cleanUp();
                } else if (!value.isPresent()) {

                    for (Character sibling : parent.children.keySet()) {
                        if (!sibling.equals(segment[0])) return;
                    }

                    if (children == null || children.isEmpty()) return;
                    if (children.size() > 1) return;

                    final Entry<Character, Node<V>> entry = children.firstEntry();
                    final Node<V> theNode = entry.getValue();

                    final char[] newSegment = ArrayHelper.concat(segment, theNode.segment);
                    final Node<V> longNode = new Node<>(newSegment, theNode.value, theNode.children, parent);
                    parent.children.put(newSegment[0], longNode);
                    parent.cleanUp();
                }

            }

        }

        private void ensureChildrenNotNull() {
            if (children == null) children = new TreeMap<Character, Node<V>>(CHAR_COMPARATOR);
        }

        private Node<V> lookup(final char[] segment) {
            assert segment != null : "lookup segment cannot be null";

            if (segment.length == 0) return this;

            ensureChildrenNotNull();

            final char childKey = segment[0];
            Node<V> child = children.get(childKey);
            if (child == null) {

                child = new Node<>(segment, Maybe.nothing(), this);
                this.children.put(childKey, child);
                return child;
            } else {

                final char[] childSegment = child.segment;
                int i;
                for (i = 0; i < childSegment.length && i < segment.length && childSegment[i] == segment[i]; i += 1);

                if (i == Math.min(childSegment.length, segment.length)) {

                    if (childSegment.length == segment.length) {

                        return child;
                    } else if (childSegment.length < segment.length) {

                        char[] suffix = Arrays.copyOfRange(segment, childSegment.length, segment.length);
                        return child.lookup(suffix);
                    } else {
                        assert childSegment.length > segment.length;

                        char[] suffix = Arrays.copyOfRange(childSegment, segment.length, childSegment.length);
                        Node<V> replacingChild = new Node<>(segment, Maybe.nothing(), this);
                        Node<V> grandChild = new Node<>(suffix, child.value, child.children, replacingChild);
                        replacingChild.ensureChildrenNotNull();
                        replacingChild.children.put(suffix[0], grandChild);
                        this.children.put(childKey, replacingChild);

                        return replacingChild;
                    }
                } else {

                    final char[] commonPrefix = Arrays.copyOfRange(childSegment, 0, i);
                    final char[] suffixChildSegment = Arrays.copyOfRange(childSegment, i, childSegment.length);
                    final char[] suffixSegment = Arrays.copyOfRange(segment, i, segment.length);

                    final Node<V> replacingChild = new Node<>(commonPrefix, Maybe.nothing(), new TreeMap<>(CHAR_COMPARATOR) , this);
                    final Node<V> grandChild1 = new Node<>(suffixChildSegment, child.value, child.children, replacingChild);
                    final Node<V> grandChild2 = new Node<>(suffixSegment, Maybe.nothing(), null, replacingChild);
                    replacingChild.children.put(suffixChildSegment[0], grandChild1);
                    replacingChild.children.put(suffixSegment[0], grandChild2);

                    this.children.put(childKey, replacingChild);
                    return grandChild2;
                }
            }
        }

        private int length() {
            return (parent == null ? 0 : parent.length()) + segment.length;
        }

        private char[] fullString() {
            int last = length();
            final char[] result = new char[last];
            Node<V> node = this;
            while (node != null) {
                final char[] segment = node.segment;
                final int segmentLength = segment.length;
                System.arraycopy(segment, 0, result, last - segmentLength, segmentLength);
                last -= segmentLength;
                node = node.parent;
            }
            return result;
        }

        private void traverse(BiConsumer<char[], ? super V> consumer) {

            if (value.isPresent()) {
                consumer.accept(fullString(), value.get());
            }

            if (children != null) {
                for (Node<V> child : children.values()) {
                    if (child != null) {
                        child.traverse(consumer);
                    }
                }
            }
        }
    }

}
