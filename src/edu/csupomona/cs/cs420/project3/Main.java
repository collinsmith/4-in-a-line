package edu.csupomona.cs.cs420.project3;

public class Main {
	public static void main(String[] args) {
		Board b = new Board(0L, 0L);
		b = b.set(0, 0, Board.O);
		b = b.set(1, 1, Board.X);
		b = b.set(2, 2, Board.O);
		b = b.set(1, 3, Board.X);
		b = b.set(0, 4, Board.O);
		b = b.set(5, 0, Board.X);
		System.out.println(b);
	}
	
	private static class Board {
		static final byte O = 1<<0;
		static final byte X = 1<<1;
		
		final long BOARD_O, BOARD_X;
		
		Board(long board_o, long board_x) {
			this.BOARD_O = board_o;
			this.BOARD_X = board_x;
		}
		
		byte get(int i, int j) {
			long mod = (long)1<<((i<<3)+j);
			long o = BOARD_O&mod;
			long x = BOARD_X&mod;
			return (byte)((o != 0 ? 0b01 : 0)|(x != 0 ? 0b10 : 0));
		}
		
		Board set(int i, int j, byte val) {
			int mod = ((i<<3)+j);
			long o = (BOARD_O&~((long)1<<mod))|(((val&Board.O)>>0)<<mod);
			long x = (BOARD_X&~((long)1<<mod))|(((val&Board.X)>>1)<<mod);
			return new Board(o, x);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(172);
			sb.append("  1 2 3 4 5 6 7 8 \n");
			for (int i = 0; i < 8; i++) {
				sb.append(((char)(i+65)));
				sb.append(' ');
				
				for (int j = 0; j < 8; j++) {
					if (get(i, j) != 0) {
						System.out.format("%d, %d%n", i, j);
					}
					switch (get(i, j)) {
						case 0b00:
							sb.append("- ");
							break;
						case 0b01:
							sb.append("O ");
							break;
						case 0b10:
							sb.append("X ");
							break;
						default:
							throw new IllegalStateException(String.format("Location (%d, %d) has an invalid state (%d)", i, j, get(i, j)));
					}
				}
				
				sb.append('\n');
			}

			sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
	}
}
