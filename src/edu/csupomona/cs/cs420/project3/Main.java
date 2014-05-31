package edu.csupomona.cs.cs420.project3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
	private static final Heuristic adjacentOnly = new Heuristic() {
		@Override
		public int evaluate(long oLocs, long xLocs, int i, int j) {
			return 1;
		}
	};

	private static List<Move> moves;
	private static boolean playerFirst;
	private static int AI_TIME;

	public static void main(String[] args) {
		for (int i = 0; i < 64; i++) {
			//System.out.println(Arrays.toString(State.unShift(Long.numberOfTrailingZeros(1L<<i))));
		}
		
		moves = new ArrayList<>(16);

		final Random RAND = new Random();
		
		final Scanner SCAN = new Scanner(System.in);

		System.out.format("Who is going first (O or X)? ");
		while (!SCAN.hasNext("[oxOX]")) {
			SCAN.next();
		}

		playerFirst = Character.toUpperCase(SCAN.next().charAt(0)) != 'O';

		System.out.format("How long does the AI player have to choose a move (seconds)? ");
		while (!SCAN.hasNextInt()) {
			SCAN.next();
		}

		AI_TIME = SCAN.nextInt();

		int i, j;
		Move last = null;
		State current = new State();
		while (moves.size() < 64) {
			System.out.format("%nTurn %d:%n", moves.size() + 1);
			System.out.format("%s%n", current);

			if (last != null && State.isWin(current, last.i, last.j)) {
				System.out.format("%c's have won!%n", resolvePlayer(moves.size()-1));
				SCAN.close();
				System.exit(0);
			}

			if (resolvePlayer(moves.size()) == 'O') {
				System.out.format("Hmm...%n");
				State successor = current.getSuccessors(moves.size(), State.O)[RAND.nextInt(64-moves.size())];
				long newBit = successor.O_LOCS^current.O_LOCS;
				int shift = Long.numberOfTrailingZeros(newBit);
				last = Move.decode(shift);
				current = current.set(last.i, last.j, State.O); //todo optimize
			} else {
				displayMoves();
				do {
					while (!SCAN.hasNext("[a-hA-H][1-8]")) {
						SCAN.next();
					}

					String next = SCAN.next();
					i = Character.toUpperCase(next.charAt(0)) - 'A';
					j = next.charAt(1) - '0' - 1;
				} while (current.get(i, j) != 0);

				current = current.set(i, j, State.X);
				last = new Move(i, j);
			}
			
			moves.add(last);
		}

		System.out.format("No more moves can be made, the game is a tie%n");
		SCAN.close();
	}

	private static char resolvePlayer(int i) {
		if (playerFirst) {
			if ((i & 1) != 0) {
				return 'O';
			} else {
				return 'X';
			}
		} else {
			if ((i & 1) == 0) {
				return 'O';
			} else {
				return 'X';
			}
		}
	}

	private static void displayMoves() {
		if (moves == null) {
			throw new IllegalStateException();
		}

		System.out.format("Move List:%n");
		if (playerFirst) {
			System.out.format("    X  0%n");
		} else {
			System.out.format("    O  X%n");
		}

		Iterator<Move> it = moves.iterator();
		for (int i = 0; true; i++) {
			System.out.format("%3s ", String.format("%d.", i + 1));
			if (it.hasNext()) {
				System.out.format("%s ", it.next());
			} else {
				break;
			}

			if (it.hasNext()) {
				System.out.format("%s", it.next());
			} else {
				break;
			}

			System.out.format("%n");
		}
	}

	private static Move search(State current) {
		return null;
	}
	
	private static int max(int alpha, int beta) {
		return 0;
	}
	
	private static int min(int alpha, int beta) {
		return 0;
	}
	
	private static class Move {
		final int i;
		final int j;

		Move(int i, int j) {
			assert 0 <= i && i < 8 : String.format("i=%d;j=%d", i, j);
			assert 0 <= j && j < 8 : String.format("i=%d;j=%d", i, j);
			this.i = i;
			this.j = j;
		}
		
		@Override
		public String toString() {
			return String.format("%c%d", 'A' + i, j + 1);
		}
		
		static Move decode(int flags) {
			int[] move = State.unShift(flags);
			return new Move(move[0], move[1]);
		}
	}

	private interface Heuristic {
		int evaluate(long oLocs, long xLocs, int i, int j);
	}

	private static class State {
		static final int BLANK = 0;
		static final int O = 1 << 0;
		static final int X = 1 << 1;
		
		static long blanks = 0xFFFFFFFFFFFFFFFFL;

		final long O_LOCS;
		final long X_LOCS;

		State() {
			this(0L, 0L);
		}

		State(long oLocs, long xLocs) {
			this.O_LOCS = oLocs;
			this.X_LOCS = xLocs;
		}

		static int getShift(int i, int j) {
			return ((i << 3) + j);
		}
		
		static int[] unShift(int loc) {
			int i = loc / 8;
			int j = loc % 8;
			return new int[] {i, j};
		}

		int get(int i, int j) {
			long mod = 1L << getShift(i, j);
			if ((O_LOCS&mod) != 0) {
				return O;
			} else if ((X_LOCS&mod) != 0) {
				return X;
			} else {
				return BLANK;
			}
		}

		State set(int i, int j, int val) {
			assert get(i, j) == BLANK : String.format("i=%d;j=%d", i, j);
			long flag = (1L << getShift(i, j));
			blanks &= ~flag; //todo optimize
			//System.out.println(Long.toBinaryString(blanks));
			switch (val) {
				case O: return new State(O_LOCS|flag, X_LOCS);
				case X: return new State(O_LOCS, X_LOCS|flag);
				default: throw new IllegalArgumentException();
			}
		}
		
		State[] getSuccessors(int moves, int val) {
			int id = 0;
			State[] successors = new State[64-moves];
			
			switch (val) {
				case O:
					long i;
					for (int j = 0; j < 64; j++) {
						i = 1L<<j;
						if ((i&blanks) != 0) {
							successors[id] = new State(O_LOCS|i, X_LOCS);
							id++;
						}
					}
					
					break;
				case X:
					throw new IllegalArgumentException();
			}
			
			if (id != successors.length) {
				throw new IllegalStateException(String.format("id=%d;len=%d", id, successors.length));
			}
			
			return successors;
		}

		static boolean isWin(State s, int i, int j) {
			int val = s.get(i, j);
			if (val == O) {
				int countRow = 0;
				int countColumn = 0;
				for (int x = 0; x < 8; x++) {
					if (s.get(x, j) == O) {
						countRow++;
						if (countRow == 4) {
							return true;
						}
					} else {
						countRow = 0;
					}

					if (s.get(i, x) == O) {
						countColumn++;
						if (countColumn == 4) {
							return true;
						}
					} else {
						countColumn = 0;
					}
				}

				return false;
			} else if (val == X) {
				int countRow = 0;
				int countColumn = 0;
				for (int x = 0; x < 8; x++) {
					if (s.get(x, j) == X) {
						countRow++;
						if (countRow == 4) {
							return true;
						}
					} else {
						countRow = 0;
					}

					if (s.get(i, x) == X) {
						countColumn++;
						if (countColumn == 4) {
							return true;
						}
					} else {
						countColumn = 0;
					}
				}

				return false;
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(172);
			sb.append("  1 2 3 4 5 6 7 8 \n");

			long val;
			for (int i = 0; i < 8; i++) {
				sb.append(((char)('A' + i)));
				sb.append(' ');

				for (int j = 0; j < 8; j++) {
					val = get(i, j);
					if (val == 0) {
						sb.append("- ");
					} else if (val == O) {
						sb.append("O ");
					} else if (val == X) {
						sb.append("X ");
					} else {
						throw new IllegalStateException();
					}
				}

				sb.append('\n');
			}

			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
	}
}
