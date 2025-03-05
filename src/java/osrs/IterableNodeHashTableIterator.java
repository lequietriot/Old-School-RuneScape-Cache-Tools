package osrs;

import java.util.Iterator;

public class IterableNodeHashTableIterator implements Iterator {

	IterableNodeHashTable hashTable;
	Node head;
	int index;
	Node last;

    public IterableNodeHashTableIterator(IterableNodeHashTable var1) {
		this.last = null; // L: 10
		this.hashTable = var1; // L: 13
		this.start(); // L: 14
	} // L: 15

	void start() {
		this.head = this.hashTable.buckets[0].previous; // L: 18
		this.index = 1; // L: 19
		this.last = null; // L: 20
	} // L: 21

	public Object next() {
		Node var1;
		if (this.hashTable.buckets[this.index - 1] != this.head) { // L: 24
			var1 = this.head; // L: 25
			this.head = var1.previous; // L: 26
			this.last = var1; // L: 27
			return var1; // L: 28
		} else {
			do {
				if (this.index >= this.hashTable.size) { // L: 30
					return null; // L: 38
				}

				var1 = this.hashTable.buckets[this.index++].previous; // L: 31
			} while(var1 == this.hashTable.buckets[this.index - 1]); // L: 32

			this.head = var1.previous; // L: 33
			this.last = var1; // L: 34
			return var1; // L: 35
		}
	}

	public boolean hasNext() {
		if (this.hashTable.buckets[this.index - 1] != this.head) { // L: 42
			return true;
		} else {
			while (this.index < this.hashTable.size) { // L: 43
				if (this.hashTable.buckets[this.index++].previous != this.hashTable.buckets[this.index - 1]) { // L: 44
					this.head = this.hashTable.buckets[this.index - 1].previous; // L: 45
					return true; // L: 46
				}

				this.head = this.hashTable.buckets[this.index - 1]; // L: 49
			}

			return false; // L: 52
		}
	}

	public void remove() {
		if (this.last == null) { // L: 56
			throw new IllegalStateException();
		} else {
			this.last.remove(); // L: 57
			this.last = null; // L: 58
		}
	} // L: 59
}
