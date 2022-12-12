package rshd;

import osrs.Node;

public class LinkedNodeList {

	static int anInt5606;

	public static byte[] method7885(CharSequence charsequence_0) {
		int i_2 = charsequence_0.length();
		byte[] bytes_3 = new byte[i_2];
		for (int i_4 = 0; i_4 < i_2; i_4++) {
			char var_5 = charsequence_0.charAt(i_4);
			if (var_5 > 0 && var_5 < 128 || var_5 >= 160 && var_5 <= 255)
				bytes_3[i_4] = (byte) var_5;
			else if (var_5 == 8364)
				bytes_3[i_4] = -128;
			else if (var_5 == 8218)
				bytes_3[i_4] = -126;
			else if (var_5 == 402)
				bytes_3[i_4] = -125;
			else if (var_5 == 8222)
				bytes_3[i_4] = -124;
			else if (var_5 == 8230)
				bytes_3[i_4] = -123;
			else if (var_5 == 8224)
				bytes_3[i_4] = -122;
			else if (var_5 == 8225)
				bytes_3[i_4] = -121;
			else if (var_5 == 710)
				bytes_3[i_4] = -120;
			else if (var_5 == 8240)
				bytes_3[i_4] = -119;
			else if (var_5 == 352)
				bytes_3[i_4] = -118;
			else if (var_5 == 8249)
				bytes_3[i_4] = -117;
			else if (var_5 == 338)
				bytes_3[i_4] = -116;
			else if (var_5 == 381)
				bytes_3[i_4] = -114;
			else if (var_5 == 8216)
				bytes_3[i_4] = -111;
			else if (var_5 == 8217)
				bytes_3[i_4] = -110;
			else if (var_5 == 8220)
				bytes_3[i_4] = -109;
			else if (var_5 == 8221)
				bytes_3[i_4] = -108;
			else if (var_5 == 8226)
				bytes_3[i_4] = -107;
			else if (var_5 == 8211)
				bytes_3[i_4] = -106;
			else if (var_5 == 8212)
				bytes_3[i_4] = -105;
			else if (var_5 == 732)
				bytes_3[i_4] = -104;
			else if (var_5 == 8482)
				bytes_3[i_4] = -103;
			else if (var_5 == 353)
				bytes_3[i_4] = -102;
			else if (var_5 == 8250)
				bytes_3[i_4] = -101;
			else if (var_5 == 339)
				bytes_3[i_4] = -100;
			else if (var_5 == 382)
				bytes_3[i_4] = -98;
			else if (var_5 == 376)
				bytes_3[i_4] = -97;
			else
				bytes_3[i_4] = 63;
		}
		return bytes_3;
	}

	public Node head = new Node();

	Node current;

	public LinkedNodeList() {
		head.next = head;
		head.previous = head;
	}

	public void clear() {
		while (true) {
			Node node_1 = head.next;
			if (node_1 == head) {
				current = null;
				return;
			}
		}
	}

	public Node getBack() {
		Node node_1 = head.next;
		if (node_1 == head) {
			current = null;
			return null;
		}
		current = node_1.next;
		return node_1;
	}

	public Node getNext() {
		Node node_1 = head.previous;
		if (node_1 == head) {
			current = null;
			return null;
		}
		current = node_1.previous;
		return node_1;
	}

	public Node getPrevious() {
		Node node_1 = current;
		if (node_1 == head) {
			current = null;
			return null;
		}
		current = node_1.next;
		return node_1;
	}

	public void insertBack(Node node_1) {
		node_1.previous = head.previous;
		node_1.next = head;
		node_1.previous.next = node_1;
		node_1.next.previous = node_1;
	}

	public void insertFront(Node node_1) {
		node_1.previous = head;
		node_1.next = head.next;
		node_1.previous.next = node_1;
		node_1.next.previous = node_1;
	}

	public boolean method7861() {
		return head.next == head;
	}

	public Node popTail() {
		Node node_1 = head.next;
		if (node_1 == head)
			return null;
		return node_1;
	}
}
