package edu.csupomona.cs.cs420.project3;

import java.util.Random;
import java.util.Scanner;

public class Main {
	private static enum Heuristics implements Heuristic {
		DEFAULT() {
			@Override
			public int evaluate(final State state, final int i, final int j) {
				int count = 0;
				for (int x = Math.max(0, i-3); x < i; x++) {
					if (state.get(x, j) == State.X) {
						count++;
					}
				}

				for (int x = Math.min(i+3, 7); i < x; x--) {
					if (state.get(x, j) == State.X) {
						count++;
					}
				}

				for (int y = Math.max(0, j-3); y < j; y++) {
					if (state.get(i, y) == State.X) {
						count++;
					}
				}

				for (int y = Math.min(j+3, 7); j < y; y--) {
					if (state.get(i, y) == State.X) {
						count++;
					}
				}

				return state.get(i, j) == State.O ? count : -count;
			}
		},
		TAKE_TWO() {
			@Override
			public int evaluate(final State state, final int i, final int j) {
				int player = state.get(i, j);
				int enemy = player == State.O ? State.X : State.O;

				// "the three"
				int threes = 0;
				int count;
				for (int x = 0; x < 8; x++) {
					for (int y = 0; y < 4; y++) {
						count = 0;
						for (int z = y; z < y+4; z++) {
							if (state.get(x, z) == enemy) {
								count++;
							}
						}

						if (count == 3) {
							threes++;
						}
					}
				}

				for (int y = 0; y < 8; y++) {
					for (int x = 0; x < 4; x++) {
						count = 0;
						for (int z = x; z < x+4; z++) {
							if (state.get(z, y) == enemy) {
								count++;
							}
						}

						if (count == 3) {
							threes++;
						}
					}
				}

				return player == State.O ? threes : -threes;
			}
		},
	};

	private static final Heuristic HEURISTIC = Heuristics.TAKE_TWO;

	private static Move[] moves;
	private static int move;

	private static boolean playerFirst;
	private static int ai_time;

	public static void main(String[] args) {
		for (int z = 0; z < 64; z++) {
			int i = (z >> 3);
			int j = (z & 7);
			System.out.format("%d, %d%n", i, j);
		}

		moves = new Move[64];
		move = 0;

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

		ai_time = SCAN.nextInt();

		int i, j;
		Move last = null;
		State current = new State();
		while (move < moves.length) {
			System.out.format("%nTurn %d:%n", move + 1);
			System.out.format("%s%n", current);

			if (last != null && current.getWinner(last.i, last.j) != State.BLANK) {
				System.out.format("%c's have won!%n", resolvePlayer(move - 1));
				SCAN.close();
				System.exit(0);
			}

			if (resolvePlayer(move) == 'O') {
				System.out.format("Hmm...%n");
				if (move == 0) {
					i = 3 + RAND.nextInt(2);
					j = 3 + RAND.nextInt(2);
					last = new Move(i, j);
				} else {
					last = getNextBestMove(current);
				}

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

			check(current, last.i, last.j);

			moves[move++] = last;
		}

		System.out.format("No more moves can be made, the game is a tie%n");
		SCAN.close();
	}

	private static void check(State state, final int i, final int j) {
		int player = state.get(i, j);
		int enemy = player == State.O ? State.X : State.O;

		int threes = 0;
		int count;
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 4; y++) {
				count = 0;
				for (int z = y; z < y+4; z++) {
					if (state.get(x, z) == enemy) {
						count++;
					}
				}

				if (count == 3) {
					threes++;
				}
			}
		}

		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 4; x++) {
				count = 0;
				for (int z = x; z < x+4; z++) {
					if (state.get(z, y) == enemy) {
						count++;
					}
				}

				if (count == 3) {
					threes++;
				}
			}
		}

		System.out.format("%d threes in this state!%n", threes);
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

		for (int turn = 1, i = 0; true; turn++) {
			System.out.format("%3s ", String.format("%d.", turn));
			if (i < move && moves[i] != null) {
				System.out.format("%s ", moves[i++]);
			} else {
				break;
			}

			if (i < move && moves[i] != null) {
				System.out.format("%s", moves[i++]);
			} else {
				break;
			}

			System.out.format("%n");
		}
	}

	private static Move chooseMove(State current, int val) {
		int best_score = 0;
		if (val == State.O) {
			best_score = Integer.MIN_VALUE;
		} else {
			best_score = Integer.MAX_VALUE;
		}

		int score = 0;
		State best_successor = null;
		State[] successors = current.getSuccessors(move, val);
		for (State successor : successors) {
			score = minimax_ab(current, successor, successors.length, val == State.O ? State.X : State.O, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (val == State.O) {
				if (best_score < score) {
					best_score = score;
					best_successor = successor;
				}
			} else {
				if (score < best_score) {
					best_score = score;
					best_successor = successor;
				}
			}
		}

		long newBit = best_successor.O_LOCS^current.O_LOCS;
		int shift = Long.numberOfTrailingZeros(newBit);
		return Move.decode(shift);
	}

	private static int minimax_ab(State current, State successor, int moves, int val, int alpha, int beta) {
		long newBit = successor.O_LOCS^current.O_LOCS;
		int shift = Long.numberOfTrailingZeros(newBit);
		Move m = Move.decode(shift);

		int winner = successor.getWinner(m.i, m.j);
		if (winner == val) {
			return Integer.MAX_VALUE;
		} else if (winner != State.BLANK) {
			return Integer.MIN_VALUE;
		} else if (moves == 64) {
			return 0;
		}

		int score;
		State[] successors = successor.getSuccessors(moves+1, val == State.O ? State.X : State.O);
		if (val == State.O) {
			for (State successor1 : successors) {
				score = minimax_ab(successor, successor1, successors.length, val == State.O ? State.X : State.O, alpha, beta);
				if (alpha < score) {
					alpha = score;
					if (beta <= alpha) {
						break;
					}
				}
			}

			return alpha;
		} else {
			for (State successor1 : successors) {
				score = minimax_ab(successor, successor1, successors.length, val == State.O ? State.X : State.O, alpha, beta);
				if (score < beta) {
					beta = score;
					if (beta <= alpha) {
						break;
					}
				}
			}

			return beta;
		}
	}

	private static Move getNextBestMove(State current) {
		int best_score = Integer.MIN_VALUE;

		int score = 0;
		State best_successor;
		State[] successors = current.getSuccessors(move, State.O);
		if (successors.length == 0) {
			best_successor = null;
			long newBit = best_successor.O_LOCS^current.O_LOCS;
			int shift = newBit == 0L ? 0 : Long.numberOfTrailingZeros(newBit);
			return Move.decode(shift);
		}

		best_successor = successors[0];
		for (State successor : successors) {
			score = alphabeta(current, successor, move+1, 4, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
			if (best_score < score) {
				best_score = score;
				best_successor = successor;
			}
		}


		long newBit = best_successor.O_LOCS^current.O_LOCS;
		int shift = newBit == 0L ? 0 : Long.numberOfTrailingZeros(newBit);
		return Move.decode(shift);
	}

	private static int alphabeta(State predecessor, State current, int moves, int depth, int alpha, int beta, boolean maximizing) {
		if (depth == 0 || moves == 64 || current.getWinner() != State.BLANK) {
			long newBit = predecessor.O_LOCS^current.O_LOCS;
			int shift = newBit == 0L ? 0 : Long.numberOfTrailingZeros(newBit);
			return HEURISTIC.evaluate(current, shift / 8, shift % 8);
		}

		State[] successors = current.getSuccessors(moves, maximizing ? State.X : State.O);
		if (maximizing) {
			for (State successor : successors) {
				alpha = Math.max(alpha, alphabeta(current, successor, moves+1, depth-1, alpha, beta, false));
				if (beta <= alpha) {
					break;
				}
			}

			return alpha;
		} else {
			for (State successor : successors) {
				beta = Math.min(beta, alphabeta(current, successor, moves+1, depth-1, alpha, beta, true));
				if (beta <= alpha) {
					break;
				}
			}

			return beta;
		}
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
		int evaluate(final State s, final int i, final int j);
	}

	private static class State {
		static final int BLANK = 0;
		static final int O = 1 << 0;
		static final int X = 1 << 1;

		final long BLANKS; //0xFFFF_FFFF_FFFF_FFFFL

		final long O_LOCS;
		final long X_LOCS;

		State() {
			this(0L, 0L);
		}

		State(long oLocs, long xLocs) {
			this.O_LOCS = oLocs;
			this.X_LOCS = xLocs;
			this.BLANKS = ~(O_LOCS|X_LOCS);
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
			//blanks &= ~flag; //todo optimize
			switch (val) {
				case O: return new State(O_LOCS|flag, X_LOCS);
				case X: return new State(O_LOCS, X_LOCS|flag);
				default: throw new IllegalArgumentException();
			}
		}

		State[] getSuccessors(int moves, int val) {
			State[] successors = new State[64-moves];

			int id = 0;
			switch (val) {
				case O:
					int j = 0;
					for (long i = (1L << 63); i != 0L; i >>>= 1) {
						if ((i&BLANKS) != 0) {
							successors[id++] = new State(O_LOCS|i, X_LOCS);
						}
					}

					break;
				case X:
					for (long i = (1L << 63); i != 0L; i >>>= 1) {
						if ((i&BLANKS) != 0) {
							successors[id++] = new State(O_LOCS, X_LOCS|i);
						}
					}

					break;
			}

			if (id != successors.length) {
				throw new IllegalStateException(String.format("successors length is not correct. size=%d; should be=%d", id, successors.length));
			}

			return successors;
		}

		int getWinner() {
			int winner;
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					winner = getWinner(i, j);
					if (winner != BLANK) {
						return winner;
					}
				}
			}

			return BLANK;
		}

		int getWinner(int i, int j) {
			int val = get(i, j);
			if (val == O) {
				int countRow = 0;
				int countColumn = 0;
				for (int x = 0; x < 8; x++) {
					if (get(x, j) == O) {
						countRow++;
						if (countRow == 4) {
							return O;
						}
					} else {
						countRow = 0;
					}

					if (get(i, x) == O) {
						countColumn++;
						if (countColumn == 4) {
							return O;
						}
					} else {
						countColumn = 0;
					}
				}

				return BLANK;
			} else if (val == X) {
				int countRow = 0;
				int countColumn = 0;
				for (int x = 0; x < 8; x++) {
					if (get(x, j) == X) {
						countRow++;
						if (countRow == 4) {
							return X;
						}
					} else {
						countRow = 0;
					}

					if (get(i, x) == X) {
						countColumn++;
						if (countColumn == 4) {
							return X;
						}
					} else {
						countColumn = 0;
					}
				}

				return BLANK;
			} else {
				return BLANK;
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

			return sb.substring(0, sb.length() - 1);
		}
	}
}
