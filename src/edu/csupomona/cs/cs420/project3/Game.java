package edu.csupomona.cs.cs420.project3;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Game implements Runnable {
	private static final Heuristic DEFAULT = new Heuristic() {
		@Override
		public int evaluate(State state, byte val) {
			assert val == O : "val should be O";

			switch (state.getWinner()) {
				case O: return Integer.MIN_VALUE;
				case X: return Integer.MAX_VALUE;
			}

			int count = 0;
			for (int i = 0; i < 8; i++) {
				for (int j = 0; j < 8; j++) {
					for (int x = Math.max(0, i-3); x < i; x++) {
						if (state.get(x, j) == X) {
							count++;
						}
					}

					for (int x = Math.min(i+3, 7); i < x; x--) {
						if (state.get(x, j) == X) {
							count++;
						}
					}

					for (int y = Math.max(0, j-3); y < j; y++) {
						if (state.get(i, y) == X) {
							count++;
						}
					}

					for (int y = Math.min(j+3, 7); j < y; y--) {
						if (state.get(i, y) == X) {
							count++;
						}
					}
				}
			}

			return count;
		}
	};

	private static final byte BLANK	= 0;
	private static final byte O		= 1 << 0;
	private static final byte X		= 1 << 1;

	private static char toChar(byte val) {
		switch (val) {
			case BLANK:	return '-';
			case O:	return 'O';
			case X:	return 'X';
			default: throw new IllegalArgumentException("val should be a valid space state");
		}
	}

	private static final byte UNKNOWN	= -1;
	private static final byte DRAW	= BLANK;

	private final Scanner SCAN;

	private final Heuristic HEURISTIC;

	private final boolean PLAYER_FIRST;
	private final long AI_TIME;

	private final int[] MOVES;
	private int move;

	private State board;

	public static void main(String[] args) {
		Game g = new Game(DEFAULT);
		g.run();
	}

	Game(Heuristic heuristic) {
		this.HEURISTIC = heuristic;

		this.SCAN = new Scanner(System.in);

		System.out.format("Who is going first (O or X)? ");
		while (!SCAN.hasNext("[oxOX]")) {
			SCAN.next();
		}

		this.PLAYER_FIRST = Character.toUpperCase(SCAN.next().charAt(0)) != toChar(O);

		System.out.format("How long does the AI player have to choose a move (seconds)? ");
		while (!SCAN.hasNextInt()) {
			SCAN.next();
		}

		this.AI_TIME = SCAN.nextLong();

		this.board = new State();

		this.MOVES = new int[64];
		Arrays.fill(MOVES, -1);
		this.move = 0;
	}

	@Override
	public void run() {
		final Random RAND = new Random();

		int i, j;
		byte winner;
		int lastMove = -1;
		String playerMove;
		byte currentPlayer = BLANK;
		while (move < MOVES.length) {
			System.out.format("%nTurn %d:%n", move+1);
			System.out.format("%s%n", board);

			if (currentPlayer != 0 && lastMove != -1 && (winner = board.getWinner()) != UNKNOWN) {
				int unshift = Move.unShift(lastMove);
				i = Move.unmaski(unshift);
				j = Move.unmaskj(unshift);
				assert winner == board.getLocalWinner(i, j) : String.format("local winner (%d) should match global winner (%d)", board.getLocalWinner(i, j), winner);

				SCAN.close();
				if (winner == DRAW) {
					System.out.format("The game is a draw!%n");
				} else {
					System.out.format("%c's have won!%n", toChar(currentPlayer));
				}

				return;
			}

			if ((currentPlayer = resolvePlayer()) == O) {
				System.out.format("Hmm...%n");
				if (move == 0) {
					i = 3 + RAND.nextInt(2);
					j = 3 + RAND.nextInt(2);
				} else {
					IDSABSearch searcher = new IDSABSearch(this, board, move, HEURISTIC);
					FutureTask<Integer> searcherTask = new FutureTask<>(searcher);

					ExecutorService pool = Executors.newSingleThreadExecutor();
					pool.execute(searcherTask);

					int bestMove;
					try {
						/*long currentTime = System.currentTimeMillis();
						long quitTime = currentTime + TimeUnit.SECONDS.toMillis(AI_TIME);
						do {
							if (searcher.searched) {
								bestMove = searcher.bestAction;
								break;
							}

							currentTime = System.currentTimeMillis();
						} while (quitTime < currentTime);
						bestMove = searcher.bestAction;*/

						bestMove = searcherTask.get(AI_TIME, TimeUnit.SECONDS);
					} catch (InterruptedException|ExecutionException|TimeoutException e) {
						bestMove = -1;
						e.printStackTrace();
					} finally {
						pool.shutdownNow();
					}

					if (bestMove == -1) {
						System.out.println("There was a problem choosing the successors");
						System.exit(0);
					}

					System.out.format("%s%n", searcher);

					int unshift = Move.unShift(bestMove);
					i = Move.unmaski(unshift);
					j = Move.unmaskj(unshift);
					System.out.format("AI chose %d, %d%n", i, j);
				}
			} else {
				displayMoves();
				do {
					while (!SCAN.hasNext("[a-hA-H][1-8]")) {
						SCAN.next();
					}

					playerMove = SCAN.next();
					i = Character.toUpperCase(playerMove.charAt(0)) - 'A';
					j = playerMove.charAt(1) - '0' - 1;
				} while (board.get(i, j) != BLANK);
			}

			board = board.set(i, j, currentPlayer);
			lastMove = Move.shift(i, j);
			MOVES[move++] = lastMove;
		}
	}

	private byte resolvePlayer() {
		return resolvePlayer(move);
	}

	private byte resolvePlayer(int move) {
		if (PLAYER_FIRST) {
			if ((move&1) != 0) {
				return O;
			}

			return X;
		}

		if ((move&1) == 0) {
			return O;
		}

		return X;
	}

	private void displayMoves() {
		System.out.format("Move List:%n");
		if (PLAYER_FIRST) {
			System.out.format("    %c  %c%n", toChar(X), toChar(O));
		} else {
			System.out.format("    %c  %c%n", toChar(X), toChar(O));
		}

		for (int turn = 1, pos = 0; true; turn++) {
			System.out.format("%3s ", String.format("%d.", turn));
			if (pos < move && MOVES[pos] != -1) {
				int unshift = Move.unShift(MOVES[pos++]);
				int i = Move.unmaski(unshift);
				int j = Move.unmaskj(unshift);
				System.out.format("%c%d ", Move.getRow(i), Move.getColumn(j));
			} else {
				break;
			}

			if (pos < move && MOVES[pos] != -1) {
				int unshift = Move.unShift(MOVES[pos++]);
				int i = Move.unmaski(unshift);
				int j = Move.unmaskj(unshift);
				System.out.format("%c%d", Move.getRow(i), Move.getColumn(j));
			} else {
				break;
			}

			System.out.format("%n");
		}
	}

	private static interface Heuristic {
		int evaluate(State state, byte val);
	}

	private static class IDSABSearch implements Callable<Integer> {
		private final Game GAME;
		private final State INITIAL_STATE;
		private final int MOVES;
		private final Heuristic HEURISTIC;

		private volatile boolean searched;
		private volatile boolean searching;
		private volatile boolean cancelled;

		private volatile int bestAction;

		private int depthLimit;

		private int maxDepth;
		private int expandedNodes;
		private long elapsedTime;

		IDSABSearch(Game game, State initialState, int moves, Heuristic heuristic) {
			this.GAME = game;
			this.INITIAL_STATE = initialState;
			this.MOVES = moves;
			this.HEURISTIC = heuristic;

			this.searched = false;
			this.searching = false;
		}

		@Override
		public Integer call() throws Exception {
			if (searched || cancelled) {
				return -1;
			}

			depthLimit = 0;
			bestAction = -1;
			searching = true;

			int moves = MOVES;
			//int previousBestAction = -1;
			State currentState = INITIAL_STATE;

			int score;
			int bestScore = Integer.MIN_VALUE;

			maxDepth = 0;
			expandedNodes = 0;

			int i, j;
			byte val = GAME.resolvePlayer(moves);
			int[] actions;
			final long startTime = System.currentTimeMillis();
			IDS: do {
				depthLimit++;
				actions = currentState.getSuccessorMoves(moves);
				for (int action : actions) {
					i = Move.unmaski(action);
					j = Move.unmaskj(action);
				}

				for (int action : actions) {
					if (!searching || cancelled) {
						break IDS;
					}

					score = abMin(
						currentState,
						moves,
						Move.unmaski(action),
						Move.unmaskj(action),
						GAME.resolvePlayer(moves),
						Integer.MIN_VALUE,
						Integer.MAX_VALUE,
						1
					);

					if (bestScore < score) {
						bestScore = score;
						bestAction = action;
					}
				}

				// TODO remove
				if (bestAction == -1) {
					throw new IllegalStateException("bestAction == -1");
				}

				//if (previousBestAction == bestAction) {
				//	break;
				//}

				i = Move.unmaski(bestAction);
				j = Move.unmaskj(bestAction);
				System.out.format("AI choosing %d, %d%n", i, j);
				currentState = currentState.set(i, j, val);
				moves++;

				//previousBestAction = bestAction;
			} while (searching);
			searched = true;
			elapsedTime = System.currentTimeMillis()-startTime;
			return bestAction;
		}

		private int abMin(State currentState, int moves, int i, int j, byte val, int alpha, int beta, int depth) {
			expandedNodes++;
			maxDepth = Math.max(maxDepth, depth);
			State node = currentState.set(i, j, val);
			if (node.getLocalWinner(i, j) != UNKNOWN || depthLimit <= depth) {
				return HEURISTIC.evaluate(node, val);
			}

			int[] actions = node.getSuccessorMoves(moves+1);
			for (int action : actions) {
				beta = Math.min(beta, abMax(
					node,
					moves+1,
					Move.unmaski(action),
					Move.unmaskj(action),
					val == O ? X : O,
					alpha,
					beta,
					depth+1
				));

				if (beta <= alpha) {
					break;
				}
			}

			return beta;
		}

		private int abMax(State currentState, int moves, int i, int j, byte val, int alpha, int beta, int depth) {
			expandedNodes++;
			maxDepth = Math.max(maxDepth, depth);
			State node = currentState.set(i, j, val);
			if (node.getLocalWinner(i, j) != UNKNOWN || depthLimit <= depth) {
				return HEURISTIC.evaluate(node, val);
			}

			int[] actions = node.getSuccessorMoves(moves+1);
			for (int action : actions) {
				alpha = Math.max(alpha, abMin(
					node,
					moves+1,
					Move.unmaski(action),
					Move.unmaskj(action),
					val == O ? X : O,
					alpha,
					beta,
					depth+1
				));

				if (beta <= alpha) {
					break;
				}
			}

			return alpha;
		}

		/*@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (isDone() || isCancelled()) {
				return false;
			}

			if (mayInterruptIfRunning && searching) {
				searching = false;
				cancelled = true;
				return true;
			}

			return false;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return searched;
		}

		@Override
		public Integer get() throws InterruptedException, ExecutionException {
			while (!searched) {
				//...
			}

			return bestAction;
		}

		@Override
		public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			long currentTime = System.currentTimeMillis();
			long quitTime = currentTime + unit.toMillis(timeout);
			do {
				if (searched) {
					return bestAction;
				}

				currentTime = System.currentTimeMillis();
			} while (quitTime < currentTime);
			return bestAction;
		}*/

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (searched) {
				sb.append(String.format("Search terminated after %dms%n", elapsedTime));
			} else {
				sb.append(String.format("Search terminated before completion%n"));
			}

			sb.append(String.format("Nodes expanded: %d%n", expandedNodes));
			sb.append(String.format("Max depth: %d", depthLimit));
			return sb.toString();
		}
	}

	private static class Move {
		static final int I_MASK = 0xF0;
		static final int J_MASK = 0x0F;

		static int unmaski(int unshifted) {
			return unshifted>>>4;
		}

		static int unmaskj(int unshifted) {
			return unshifted&J_MASK;
		}

		static int shift(int i, int j) {
			return ((i << 3) + j);
		}

		static int unShift(int shift) {
			int i = ((shift >>> 3) << 4);
			int j = ((shift &   7) << 0);
			return i|j;
		}

		static char getRow(int i) {
			return (char)('A' + i);
		}

		static int getColumn(int j) {
			return j+1;
		}
	}

	private static class State {
		static boolean checkRows(long locs) {
			long row;
			long mask;
			for (int i = 0; i < 64; i+=8) {
				mask = 0xFFL << i;
				row = (locs&mask) >>> i;
				for (int j = 0xF0; j != 0b0111; j >>>= 1) {
					if ((row&j) == j) {
						return true;
					}
				}
			}

			return false;
		}

		static boolean checkLocalRow(long locs, final int i, final int j) {
			int shift = i << 3;
			long mask = 0xFFL << shift;
			long row = (locs&mask) >>> shift;

			int jPos = 1<<j;
			for (int winCondition = 0xF0; winCondition != 0b0111; winCondition >>>= 1) {
				if ((winCondition&jPos) != 0 && (row&winCondition) == winCondition) {
					return true;
				}
			}

			return false;
		}

		static boolean checkColumns(long locs) {
			long column;
			long mask;
			for (int j = 0; j < 8; j++) {
				mask = 0x0101_0101_0101_0101L << j;
				column = (locs&mask);
				for (long i = 0x8080_8080_0000_0000L; i != 0x0080_8080; i >>>= 1) {
					if ((column&i) == i) {
						return true;
					}
				}
			}

			return false;
		}

		static boolean checkLocalColumn(long locs, final int i, final int j) {
			long mask = 0x0101_0101_0101_0101L << j;
			long column = (locs&mask);

			long iPos = 1L << (i << 3);
			for (long winCondition = 0x8080_8080_0000_0000L; winCondition != 0x0080_8080; winCondition >>>= 1) {
				if ((winCondition&iPos) != 0 && (column&winCondition) == winCondition) {
					return true;
				}
			}

			return false;
		}

		final long O_LOCS;
		final long X_LOCS;
		final long BLANK_LOCS;

		State() {
			this(0L, 0L);
		}

		State(long oLocs, long xLocs) {
			this(oLocs, xLocs, ~(oLocs|xLocs));
		}

		State(long oLocs, long xLocs, long blankLocs) {
			assert (oLocs&xLocs) == 0 : "oLocs cannot have matching bits within xLocs";
			assert ~(oLocs|xLocs) == blankLocs : "Blank bits should only be set if bit is not set in oLocs and xLocs";
			this.O_LOCS = oLocs;
			this.X_LOCS = xLocs;
			this.BLANK_LOCS = blankLocs;
		}

		byte get(int i, int j) {
			long flag = 1L << Move.shift(i, j);
			if ((O_LOCS&flag) != 0) {
				return O;
			} else if ((X_LOCS&flag) != 0) {
				return X;
			}

			return BLANK;
		}

		State set(int i, int j, byte val) {
			assert get(i, j) == BLANK : String.format("Piece at %c%d must be blank!", Move.getRow(i), Move.getColumn(j));
			assert Integer.bitCount(val) <= 1 : String.format("val (%d) should be a flag with a single bit set", val);
			long flag = (1L << Move.shift(i, j));
			switch (val) {
				case O: return new State(O_LOCS|flag, X_LOCS);
				case X: return new State(O_LOCS, X_LOCS|flag);
				default: throw new IllegalArgumentException(String.format("val should be one of O (%d) or X (%d)", O, X));
			}
		}

		byte getWinner() {
			if (checkRows(O_LOCS) || checkColumns(O_LOCS)) {
				return O;
			} else if (checkRows(X_LOCS) || checkColumns(X_LOCS)) {
				return X;
			} else if (BLANK_LOCS == 0L) {
				return DRAW;
			}

			return UNKNOWN;
		}

		byte getLocalWinner(final int i, final int j) {
			byte val = get(i, j);
			assert val != BLANK : "This is a pointless call";
			if (checkLocalRow(O_LOCS, i, j) || checkLocalColumn(O_LOCS, i, j)) {
				return O;
			} else if (checkLocalRow(X_LOCS, i, j) || checkLocalColumn(X_LOCS, i, j)) {
				return X;
			} else if (BLANK_LOCS == 0L) {
				return DRAW;
			}

			return UNKNOWN;
		}

		int[] getSuccessorMoves(int moves) {
			if (moves < 0) {
				throw new IllegalArgumentException("moves must be >= 0");
			} else if (64 < moves) {
				throw new IllegalArgumentException("moves must be <= 64");
			}

			long flag;
			int id = 0;
			int[] actions = new int[64-moves];
			for (int i = 0; i < 64; i++) {
				flag = 1L << i;
				if ((BLANK_LOCS&flag) != 0) {
					actions[id++] = Move.unShift(i);
				}
			}

			assert id == actions.length : String.format("actions length is not correct. size=%d, should be=%d", id, actions.length);
			return actions;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(172);
			sb.append("  1 2 3 4 5 6 7 8 \n");

			byte val;
			for (int i = 0; i < 8; i++) {
				sb.append(Move.getRow(i));
				sb.append(' ');
				for (int j = 0; j < 8; j++) {
					val = get(i, j);
					sb.append(toChar(val));
					sb.append(' ');
				}

				sb.append('\n');
			}

			return sb.substring(0, sb.length()-1);
		}
	}
}
