package edu.csupomona.cs.cs420.project3;

public class Main {
	public static void main(String[] args) {
		State b = new State();
		b = b.set(0, 0, State.O);
		b = b.set(1, 1, State.X);
		b = b.set(2, 2, State.O);
		b = b.set(1, 3, State.X);
		b = b.set(0, 4, State.O);
		b = b.set(5, 0, State.X);
		System.out.println(b);
		System.out.println(b.get(1, 0));
	}
	
	private static class State {
		static final long O = 1L<<0;
		static final long X = 1L<<1;
		
		final long O_LOCS;
		final long X_LOCS;
		
		State() {
			this(0L, 0L);
		}
		
		State(long oLocs, long xLocs) {
			this.O_LOCS = oLocs;
			this.X_LOCS = xLocs;
		}
		
		long getShift(long i, long j) {
			return ((i<<3)+j);
		}
		
		long get(long i, long j) {
			long mod = 1L<<getShift(i, j);
			long o = O_LOCS&mod;
			long x = X_LOCS&mod;
			if (o != 0) {
				return O;
			} else if (x != 0) {
				return X;
			} else {
				return 0L;
			}
			
			//return (o != 0 ? 0b01 : 0)|(x != 0 ? 0b10 : 0);
		}
		
		State set(long i, long j, long val) {
			long shift = getShift(i, j);
			long o = O_LOCS;
			long x = X_LOCS;
			if (val == O) {
				o |= 1<<shift;
			} else if (val == X) {
				x |= 1<<shift;
			} else {
				throw new IllegalArgumentException();
			}
			
			return new State(o, x);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(172);
			sb.append("  1 2 3 4 5 6 7 8 \n");
			
			long loc;
			for (int i = 0; i < 8; i++) {
				sb.append(((char)('A'+i)));
				sb.append(' ');
				
				for (int j = 0; j < 8; j++) {
					if (get(i, j) != 0) {
						System.out.format("%d, %d%n", i, j);
					}
					
					loc = get(i, j);
					if (loc == 0) {
						sb.append("- ");
					} else if (loc == O) {
						sb.append("O ");
					} else if (loc == X) {
						sb.append("X ");
					} else {
						throw new IllegalStateException();
					}
				}
				
				sb.append('\n');
			}

			sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
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
			long mod = 1L<<((i<<3)+j);
			long o = BOARD_O&mod;
			long x = BOARD_X&mod;
			return (byte)((o != 0 ? 0b01 : 0)|(x != 0 ? 0b10 : 0));
		}
		
		Board set(int i, int j, byte val) {
			int mod = ((i<<3)+j);
			long o = (BOARD_O&~(1L<<mod))|(((val&Board.O)>>0)<<mod);
			long x = (BOARD_X&~(1L<<mod))|(((val&Board.X)>>1)<<mod);
			return new Board(o, x);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(172);
			sb.append("  1 2 3 4 5 6 7 8 \n");
			for (int i = 0; i < 8; i++) {
				sb.append(((char)('A'+i)));
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
