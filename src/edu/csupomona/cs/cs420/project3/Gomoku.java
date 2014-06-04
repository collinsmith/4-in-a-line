package edu.csupomona.cs.cs420.project3;

import java.util.Arrays;

public class Gomoku {
	private static final byte BLANK = 0;
	private static final byte O = 1 << 0;
	private static final byte X = 1 << 1;

	private static char toChar(byte piece) {
		switch (piece) {
			case BLANK:	return '-';
			case O:	return 'O';
			case X:	return 'X';
			default:	throw new IllegalArgumentException();
		}
	}

	public static void main(String[] args) {
		//testMove();
		//testNode();
		testActions();
	}

	public Gomoku() {
		//...
	}

	private static class Move {
		static final int I_MASK = 0xF0;
		static final int J_MASK = 0x0F;

		static final int unmaski(int compacted) {
			return compacted >>> 4;
		}

		static final int unmaskj(int compacted) {
			return compacted & J_MASK;
		}

		static int index(int i, int j) {
			return ((i << 3) + j);
		}

		static int compact(int indexed) {
			int i = ((indexed >>> 3) << 4);
			int j = (indexed & 7);
			return i|j;
		}

		static char toLetter(int i) {
			return (char)('A' + i);
		}

		static int toNumber(int j) {
			return j + 1;
		}
	}

	private static class Node {
		final long O_LOCS;
		final long X_LOCS;
		final long BLANK_LOCS;

		Node(long oLocs, long xLocs) {
			assert (oLocs&xLocs) == 0L : "oLocs cannot have matching bits within xLocs";
			this.O_LOCS = oLocs;
			this.X_LOCS = xLocs;
			this.BLANK_LOCS = ~(O_LOCS|X_LOCS);
		}

		byte get(int i, int j) {
			long flag = 1L << Move.index(i, j);
			if ((O_LOCS&flag) != 0) {
				return O;
			} else if ((X_LOCS&flag) != 0) {
				return X;
			}

			return BLANK;
		}

		Node set(int i, int j, byte piece) {
			assert get(i, j) == BLANK : String.format("piece at %c%d must be blank!", Move.toLetter(i), Move.toNumber(j));
			long flag = 1L << Move.index(i, j);
			switch (piece) {
				case O: return new Node(O_LOCS|flag, X_LOCS);
				case X: return new Node(O_LOCS, X_LOCS|flag);
				default: throw new IllegalArgumentException(String.format("val should be one of O (%d) or X (%d)", O, X));
			}
		}

		// returns array of compacted indeces
		int[] getActions() {
			int i = 0;
			int[] actions = new int[Long.SIZE];
			for (int indexed = 0; indexed < Long.SIZE; indexed++) {
				if ((BLANK_LOCS&(1L<<indexed)) != 0) {
					actions[i++] = Move.compact(indexed);
				}
			}

			Arrays.fill(actions, i, actions.length, -1);
			return actions;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(172);
			sb.append("  ");
			for (int i = 0; i < 8; i++) {
				sb.append(Move.toNumber(i));
				sb.append(' ');
			}
			sb.append('\n');

			byte piece;
			for (int i = 0; i < 8; i++) {
				sb.append(Move.toLetter(i));
				sb.append(' ');
				for (int j = 0; j < 8; j++) {
					piece = get(i, j);
					sb.append(toChar(piece));
					sb.append(' ');
				}

				sb.append('\n');
			}

			return sb.substring(0, sb.length()-1);
		}
	}

	private static void testMove() {
		for (int z = 0; z < Long.SIZE; z++) {
			int compacted = Move.compact(z);
			int i = Move.unmaski(compacted);
			int j = Move.unmaskj(compacted);
			int indexed = Move.index(i, j);
			if (z != indexed) {
				throw new IllegalStateException();
			}

			System.out.format("%d => %c, %d <= %d%n", z, Move.toLetter(i), Move.toNumber(j), indexed);
		}
	}

	private static void testNode() {
		Node n;
		long flags = 0x0101_0101_0101_0101L;
		System.out.format("%s%n", n = new Node(flags, 0L));
		flags |= 0x0000_0000_0000_00FFL;
		System.out.format("%s%n", n = new Node(flags, 0L));
		flags |= 0xFF00_0000_0000_0000L;
		System.out.format("%s%n", n = new Node(flags, 0L));
		flags |= 0x8080_8080_8080_8080L;
		System.out.format("%s%n", n = new Node(flags, 0L));

		String blanks = Long.toBinaryString(n.BLANK_LOCS);
		System.out.format("%s%n", blanks);
		System.out.format("1's count: %d =? %d%n", Long.bitCount(n.BLANK_LOCS), 36);

		flags = 0L;
		n = new Node(flags, flags);
		for (int j = 0; j < 8; j++) {
			char before = toChar(n.get(0, j));
			String blanksBefore = Long.toBinaryString(n.BLANK_LOCS);
			n = n.set(0, j, X);
			char after = toChar(n.get(0, j));
			String blanksAfter = Long.toBinaryString(n.BLANK_LOCS);
			System.out.format("%c => %c%n", before, after);
			System.out.format("%s%n", blanksBefore);
			System.out.format("%s%n", blanksAfter);
		}

		System.out.format("%s%n", n);
	}

	private static void testActions() {
		long flags = 0x0101_0101_0101_0101L;
		flags |= 0x0000_0000_0000_00FFL;
		flags |= 0xFF00_0000_0000_0000L;
		flags |= 0x8080_8080_8080_8080L;
		Node n = new Node(flags, 0L);
		n = n.set(3, 3, X);
		n = n.set(3, 4, X);
		n = n.set(4, 3, X);
		n = n.set(4, 4, X);
		System.out.format("%s%n", n);

		int count = 0;
		int[] actions = n.getActions();
		for (int action : actions) {
			if (action == -1) {
				break;
			}

			int i = Move.unmaski(action);
			int j = Move.unmaskj(action);
			System.out.format("%c, %d%n", Move.toLetter(i), Move.toNumber(j));
			count++;
		}

		System.out.format("%d blanks available%n", count);
	}
}
