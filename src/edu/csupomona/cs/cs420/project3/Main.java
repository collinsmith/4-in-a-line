package edu.csupomona.cs.cs420.project3;

public class Main {
	public static void main(String[] args) {
		Board b = new Board(0L, 0L);
		b = b.set(1, 1, 3);
		System.out.println(b.get(1, 1));
		b = b.set(0, 0, 2);
		System.out.println(b.get(0, 0));
		System.out.println(b.get(1, 1));
		b = b.set(2, 2, 1);
		System.out.println(b.get(2, 2));
		System.out.println(b.get(0, 0));
		System.out.println(b.get(1, 1));
	}
	
	private static class Board {
		final long BOARD_O, BOARD_X;
		
		Board(long board_o, long board_x) {
			this.BOARD_O = board_o;
			this.BOARD_X = board_x;
		}
		
		int get(int i, int j) {
			int mod = 1<<((i<<3)+j);
			long o = BOARD_O&mod;
			long x = BOARD_X&mod;
			return (o != 0 ? 0b01 : 0)|(x != 0 ? 0b10 : 0);
		}
		
		Board set(int i, int j, int val) {
			int mod = ((i<<3)+j);
			long o = (BOARD_O&~(1<<mod))|((val>>0)<<mod);
			long x = (BOARD_X&~(1<<mod))|((val>>1)<<mod);
			return new Board(o, x);
		}
	}
}
