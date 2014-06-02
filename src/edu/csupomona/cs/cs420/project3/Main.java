package edu.csupomona.cs.cs420.project3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
	private static enum Heuristics implements Heuristic {
		DEFAULT() {
			@Override
			public int evaluate(State state, Move move) {
				int count = 0;
				for (int i = Math.max(0, move.i-3); i < move.i; i++) {
					if (state.get(i, move.j) == State.X) {
						count++;
					}
				}

				for (int i = Math.min(move.i+3, 7); move.i < i; i--) {
					if (state.get(i, move.j) == State.X) {
						count++;
					}
				}

				for (int j = Math.max(0, move.j-3); j < move.j; j++) {
					if (state.get(move.i, j) == State.X) {
						count++;
					}
				}

				for (int j = Math.min(move.j+3, 7); move.j < j; j--) {
					if (state.get(move.i, j) == State.X) {
						count++;
					}
				}

				//return state.get(move.i, move.j) == State.O ? count : -count;
				return count;
			}
		};
	};

	private static final Heuristic HEURISTIC = Heuristics.DEFAULT;

	private static List<Move> moves;
	private static boolean playerFirst;
	private static int ai_time;

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

		ai_time = SCAN.nextInt();

		int i, j;
		Move last = null;
		State current = new State();
		while (moves.size() < 64) {
			System.out.format("%nTurn %d:%n", moves.size() + 1);
			System.out.format("%s%n", current);

			if (last != null && current.getWinner(last.i, last.j) != State.BLANK) {
				System.out.format("%c's have won!%n", resolvePlayer(moves.size()-1));
				SCAN.close();
				System.exit(0);
			}

			if (resolvePlayer(moves.size()) == 'O') {
				System.out.format("Hmm...%n");
				if (moves.isEmpty()) {
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

	private static Move chooseMove(State current, int val) {
		int best_score = 0;
		if (val == State.O) {
			best_score = Integer.MIN_VALUE;
		} else {
			best_score = Integer.MAX_VALUE;
		}

		int score = 0;
		State best_successor = null;
		State[] successors = current.getSuccessors(moves.size(), val);
		for (int i = 0; i < successors.length; i++) {
			score = minimax_ab(current, successors[i], successors.length, val == State.O ? State.X : State.O, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (val == State.O) {
				if (best_score < score) {
					best_score = score;
					best_successor = successors[i];
				}
			} else {
				if (score < best_score) {
					best_score = score;
					best_successor = successors[i];
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
			for (int i = 0; i < successors.length; i++) {
				score = minimax_ab(successor, successors[i], successors.length, val == State.O ? State.X : State.O, alpha, beta);
				if (alpha < score) {
					alpha = score;
					if (beta <= alpha) {
						break;
					}
				}
			}

			return alpha;
		} else {
			for (int i = 0; i < successors.length; i++) {
				score = minimax_ab(successor, successors[i], successors.length, val == State.O ? State.X : State.O, alpha, beta);
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
		State best_successor = null;
		State[] successors = current.getSuccessors(moves.size(), State.O);
		for (State successor : successors) {
			score = alphabeta(current, successor, 64-successors.length, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
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
			return HEURISTIC.evaluate(current, Move.decode(shift));
		}

		State[] successors = current.getSuccessors(moves, maximizing ? State.O : State.X);
		if (maximizing) {
			for (State successor : successors) {
				alpha = Math.max(alpha, alphabeta(current, successor, 64-successors.length, depth-1, alpha, beta, false));
				if (beta <= alpha) {
					break;
				}
			}

			return alpha;
		} else {
			for (State successor : successors) {
				beta = Math.min(beta, alphabeta(current, successor, 64-successors.length, depth-1, alpha, beta, true));
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
		int evaluate(State s, Move m);
	}

	private static class State {
		static final int BLANK = 0;
		static final int O = 1 << 0;
		static final int X = 1 << 1;

		static long blanks = 0xFFFF_FFFF_FFFF_FFFFL;

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

			long i;
			switch (val) {
				case O:
					for (int j = 0; j < 64; j++) {
						i = 1L<<j;
						if ((i&blanks) != 0) {
							successors[id] = new State(O_LOCS|i, X_LOCS);
							id++;
						}
					}

					break;
				case X:
					for (int j = 0; j < 64; j++) {
						i = 1L<<j;
						if ((i&blanks) != 0) {
							successors[id] = new State(O_LOCS, X_LOCS|i);
							id++;
						}
					}

					break;
			}

			if (id != successors.length) {
				throw new IllegalStateException(String.format("id=%d;len=%d", id, successors.length));
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

			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
	}
}
