package runescape;

import java.util.Iterator;

public class IterableNodeDequeDescendingIterator implements Iterator {

	IterableNodeDeque deque;
	Node field3813;
	Node last;

    IterableNodeDequeDescendingIterator(IterableNodeDeque var1) {
		this.last = null; // L: 9
		this.setDeque(var1); // L: 12
	} // L: 13

	void setDeque(IterableNodeDeque var1) {
		this.deque = var1; // L: 16
		this.start(); // L: 17
	} // L: 18

	void start() {
		this.field3813 = this.deque != null ? this.deque.sentinel.previous : null; // L: 21
		this.last = null; // L: 22
	} // L: 23

	public Object next() {
		Node var1 = this.field3813; // L: 26
		if (var1 == this.deque.sentinel) { // L: 27
			var1 = null; // L: 28
			this.field3813 = null; // L: 29
		} else {
			this.field3813 = var1.previous; // L: 31
		}

		this.last = var1; // L: 32
		return var1; // L: 33
	}

	public boolean hasNext() {
		return this.deque.sentinel != this.field3813 && this.field3813 != null; // L: 37
	}

	public void remove() {
		if (this.last == null) { // L: 41
			throw new IllegalStateException();
		} else {
			this.last.remove(); // L: 42
			this.last = null; // L: 43
		}
	} // L: 44
}
