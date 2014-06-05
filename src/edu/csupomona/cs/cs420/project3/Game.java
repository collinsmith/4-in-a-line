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
	private static enum Heuristics implements Heuristic {
		DEFAULT() {
			@Override
			public int evaluate(Node state, byte piece, final int i, final int j) {
				byte val;
				byte enemy = piece == O ? X : O;

				byte winner = state.getLocalWinner(i, j);
				if (winner == piece) {
					return Integer.MAX_VALUE;
				} else if (winner == enemy) {
					return Integer.MIN_VALUE;
				} else if (winner == DRAW) {
					return 0;
				}

				int count = 0;
				for (int k = 0; k < 8; k++) {
					for (int l = 0; l < 8; l++) {
						for (int x = Math.max(0, k-3); x < k; x++) {
							if (state.get(x, l) == piece) {
								count++;
							}
						}

						for (int x = Math.min(k+3, 7); k < x; x--) {
							if (state.get(x, l) == piece) {
								count++;
							}
						}

						for (int y = Math.max(0, l-3); y < l; y++) {
							if (state.get(k, y) == piece) {
								count++;
							}
						}

						for (int y = Math.min(l+3, 7); l < y; y--) {
							if (state.get(k, y) == piece) {
								count++;
							}
						}
					}
				}

				return count;
			}
		},
		MARK2() {
			@Override
			public int evaluate(Node state, byte piece, final int i, final int j) {
				byte val;
				byte enemy = piece == O ? X : O;

				byte winner = state.getLocalWinner(i, j);
				if (winner == piece) {
					return Integer.MAX_VALUE;
				} else if (winner == enemy) {
					return Integer.MIN_VALUE;
				} else if (winner == DRAW) {
					return 0;
				}

				int count;

				int straightThrees = 0;
				for (int x = 0; x < 8; x++) {
					for (int y = 0; y < 5; y++) {
						count = 0;
						for (int k = y; k < y+4; k++) {
							val = state.get(x, y);
							if (val == piece) {
								count++;
							} else if (val == enemy) {
								count = 0;
							}
						}

						if (3 <= count) {
							straightThrees++;
						}
					}
				}

				int straightTwos = 0;
				for (int x = 0; x < 8; x++) {
					for (int y = 0; y < 6; y++) {
						count = 0;
						for (int k = y; k < y+3; k++) {
							val = state.get(x, y);
							if (val == piece) {
								count++;
							} else if (val == enemy) {
								count = 0;
							}
						}

						if (2 <= count) {
							straightThrees++;
						}
					}
				}

				int eval = (straightThrees*100) + (straightTwos*10);
				return piece == O ? eval : -eval;
			}
		},
	}

	public static void main(String[] args) {
		//testMove();
		//testNode();
		//testActions();
		//testLocalWinConditions();

		Game g = new Game(Heuristics.DEFAULT);
		g.run();
	}

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

	private static final byte UNKNOWN	= -1;
	private static final byte DRAW	= 0;

	private static final byte NULL_ACTION = -1;

	private final Heuristic HEURISTIC;
	private final Scanner SCAN;

	private final boolean PLAYER_FIRST;
	private final long AI_TIME;

	private final int[] MOVES;
	private int move;

	//private final long[][] TRANSPOSITION;

	private Node currentState;

	private Game(Heuristic heuristic) {
		this.HEURISTIC = heuristic;

		//this.TRANSPOSITION = new long[Long.SIZE][3];
		//initZobrist();

		this.SCAN = new Scanner(System.in);

		System.out.format("Who is going first (O or X)? ");
		while (!SCAN.hasNext("[oxOX]")) {
			SCAN.next();
		}

		this.PLAYER_FIRST = Character.toUpperCase(SCAN.next().charAt(0)) != toChar(O);

		System.out.format("How long does the AI player have to choose a move (ms)? ");
		while (!SCAN.hasNextLong()) {
			SCAN.next();
		}

		this.AI_TIME = SCAN.nextLong();

		currentState = new Node(0L, 0L);

		this.MOVES = new int[Long.SIZE];
		Arrays.fill(MOVES, NULL_ACTION);
		this.move = 0;
	}

	/*private void initZobrist() {
		final Random RAND = new Random();
		for (int i = 0; i < TRANSPOSITION.length; i++) {
			for (int j = 0; j < TRANSPOSITION[i].length; j++) {
				TRANSPOSITION[i][j] = RAND.nextLong();
			}
		}
	}

	private int createKey(Node n) {
		int key = 0;
		for (int indexed = 0; indexed < Long.SIZE; indexed++) {
			key ^= TRANSPOSITION[indexed][n.get(Move.unindexi(indexed), Move.unindexj(indexed))];
		}

		return key;
	}*/

	@Override
	public void run() {
		final Random RAND = new Random();

		int i, j;
		byte winner;
		String playerMove;
		int lastMove = NULL_ACTION;
		byte currentPlayer = BLANK;
		while (true) {
			System.out.format("%nTurn %d:%n", move+1);
			System.out.format("%s%n", currentState);

			if ((currentPlayer&(O|X)) != 0 && lastMove != NULL_ACTION && (winner = currentState.getWinner()) != UNKNOWN) {
				i = Move.unmaski(lastMove);
				j = Move.unmaskj(lastMove);
				byte check;
				assert winner == (check = currentState.getLocalWinner(i, j)) : String.format("local winner (%d) should match global winner (%d)", check, winner);

				SCAN.close();
				if (winner == DRAW) {
					System.out.format("The game is a draw!%n");
				} else {
					System.out.format("%c's have won!%n", toChar(currentPlayer));
				}

				return;
			}

			currentPlayer = resolvePlayer();
			if (currentPlayer == O) {
				System.out.format("Hmm...%n");
				if (move == 0) {
					i = 3 + RAND.nextInt(2);
					j = 3 + RAND.nextInt(2);
				} else {
					IDSABSearch searcher = new IDSABSearch(this, currentState, HEURISTIC);
					FutureTask<Integer> searcherTask = new FutureTask<>(searcher);

					ExecutorService pool = Executors.newSingleThreadExecutor();
					pool.execute(searcherTask);

					int bestAction;
					try {
						bestAction = searcherTask.get(AI_TIME, TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) {
						searcher.searching = false;
						try {
							bestAction = searcherTask.get();
						} catch (ExecutionException|InterruptedException e2) {
							e.printStackTrace();
							bestAction = NULL_ACTION;
						}
					} catch (InterruptedException|ExecutionException e) {
						e.printStackTrace();
						bestAction = NULL_ACTION;
					} finally {
						pool.shutdownNow();
					}

					if (bestAction == NULL_ACTION) {
						System.out.format("There was a problem choosing the successors%n");
						System.exit(0);
					}

					i = Move.unmaski(bestAction);
					j = Move.unmaskj(bestAction);
					System.out.format("I think I'll go with %c%d%n", Move.toLetter(i), Move.toNumber(j));

					System.out.format("%s%n", searcher);
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
				} while (currentState.get(i, j) != BLANK);
			}

			currentState = currentState.set(i, j, currentPlayer);
			lastMove = Move.compact(Move.index(i, j));
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
			System.out.format("    %c  %c%n", toChar(O), toChar(X));
		}

		for (int turn = 1, pos = 0; true; turn++) {
			System.out.format("%3s ", String.format("%d.", turn));
			if (pos < move && MOVES[pos] != NULL_ACTION) {
				int compacted = MOVES[pos++];
				int i = Move.unmaski(compacted);
				int j = Move.unmaskj(compacted);
				System.out.format("%c%d ", Move.toLetter(i), Move.toNumber(j));
			} else {
				break;
			}

			if (pos < move && MOVES[pos] != NULL_ACTION) {
				int compacted = MOVES[pos++];
				int i = Move.unmaski(compacted);
				int j = Move.unmaskj(compacted);
				System.out.format("%c%d", Move.toLetter(i), Move.toNumber(j));
			} else {
				break;
			}

			System.out.format("%n");
		}
	}

	private static interface Heuristic {
		int evaluate(Node state, byte piece, int x, int y);
	}

	private static class IDSABSearch implements Callable<Integer> {
		final Game GAME;
		final Node INITIAL_STATE;
		final Heuristic HEURISTIC;

		volatile int depthLimit;
		volatile int bestAction;
		volatile boolean searched;
		volatile boolean searching;

		int maxDepthSearched;
		int nodesExpanded;
		long timeElapsed;

		public IDSABSearch(Game game, Node initialState, Heuristic heuristic) {
			this.GAME = game;
			this.INITIAL_STATE = initialState;
			this.HEURISTIC = heuristic;

			this.depthLimit = 0;
			this.bestAction = NULL_ACTION;
			this.searched = false;
			this.searching = false;

			this.timeElapsed = 0;
			this.nodesExpanded = 0;
			this.maxDepthSearched = 0;
		}

		@Override
		public Integer call() throws Exception {
			if (searched) {
				return bestAction;
			}

			final Random RAND = new Random();

			int score;
			int bestScore = Integer.MIN_VALUE;
			Node bestState = INITIAL_STATE;

			int i, j;
			int[] actions;
			byte piece = GAME.resolvePlayer();
			long startTime = System.currentTimeMillis();

			searching = true;
			IDS: while (searching) {
				depthLimit++;
				actions = bestState.getActions();
				for (int action : actions) {
					if (!searching) {
						break IDS;
					}

					if (action == NULL_ACTION) {
						break;
					}

					score = min(
						  bestState,
						  Move.unmaski(action),
						  Move.unmaskj(action),
						  piece == O ? X : O,
						  Integer.MIN_VALUE,
						  Integer.MAX_VALUE,
						  1
					);

					if (bestScore == score && RAND.nextBoolean()) {
						//bestAction = action;
					} else if (bestScore < score) {
						bestScore = score;
						bestAction = action;
					}
				}

				i = Move.unmaski(bestAction);
				j = Move.unmaskj(bestAction);
				//System.out.format("Choosing %c%d%n", Move.toLetter(i), Move.toNumber(j));

				if (bestState.get(i, j) == piece) {
					//System.out.format("Skipping %c%d, already picked this one%n", Move.toLetter(i), Move.toNumber(j));
					continue IDS;
				}

				bestState = bestState.set(i, j, piece);
			}

			searched = true;
			timeElapsed = System.currentTimeMillis() - startTime;
			return bestAction;
		}

		private int min(Node currentState, int i, int j, byte piece, int alpha, int beta, int depth) {
			nodesExpanded++;
			maxDepthSearched = Math.max(maxDepthSearched, depth);

			Node successor = currentState.set(i, j, piece);
			byte winner = successor.getLocalWinner(i, j);
			if (winner != UNKNOWN || depthLimit <= depth) {
				return HEURISTIC.evaluate(successor, piece, i, j);
				/*if (winner == piece) {
					return 10000;
				} else if (winner == DRAW) {
					return 0;
				}

				return -10000;*/
			}

			int[] actions = successor.getActions();
			for (int action : actions) {
				if (action == NULL_ACTION || !searching) {
					break;
				}

				beta = Math.min(beta, max(
					  successor,
					  Move.unmaski(action),
					  Move.unmaskj(action),
					  piece == O ? X : O,
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

		int max(Node currentState, int i, int j, byte piece, int alpha, int beta, int depth) {
			nodesExpanded++;
			maxDepthSearched = Math.max(maxDepthSearched, depth);

			Node successor = currentState.set(i, j, piece);
			byte winner = successor.getLocalWinner(i, j);
			if (winner != UNKNOWN || depthLimit <= depth) {
				return HEURISTIC.evaluate(successor, piece, i, j);
				/*if (winner == piece) {
					return 10000;
				} else if (winner == DRAW) {
					return 0;
				}

				return -10000;*/
			}

			int[] actions = successor.getActions();
			for (int action : actions) {
				if (action == NULL_ACTION || !searching) {
					break;
				}

				alpha = Math.max(alpha, min(
					  successor,
					  Move.unmaski(action),
					  Move.unmaskj(action),
					  piece == O ? X : O,
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

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (searching) {
				sb.append(String.format("Still searching...%n"));
			} else {
				sb.append(String.format("Search terminated after %dms%n", timeElapsed));
			}

			sb.append(String.format("Nodes expanded: %d%n", nodesExpanded));
			sb.append(String.format("Max depth: %d", maxDepthSearched));
			return sb.toString();
		}
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

		static int unindexi(int indexed) {
			return (indexed >>> 3);
		}

		static int unindexj(int indexed) {
			return (indexed & 7);
		}

		static int compact(int indexed) {
			int i = (unindexi(indexed) << 4);
			int j = unindexj(indexed);
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

		final int HASH;

		Node(long oLocs, long xLocs) {
			assert (oLocs&xLocs) == 0L : "oLocs cannot have matching bits within xLocs";
			this.O_LOCS = oLocs;
			this.X_LOCS = xLocs;
			this.BLANK_LOCS = ~(O_LOCS|X_LOCS);

			int hash = 1;
			hash = 31 * hash + (int)(O_LOCS ^ (O_LOCS >>> 32));
			hash = 31 * hash + (int)(X_LOCS ^ (X_LOCS >>> 32));
			this.HASH = hash;
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

			Arrays.fill(actions, i, actions.length, NULL_ACTION);
			return actions;
		}

		// returns O, X if winner, Draw if no more moves avail, Unknown if nondeterminate
		byte getWinner() {
			if (WinConditions.checkRows(O_LOCS) || WinConditions.checkColumns(O_LOCS)) {
				return O;
			} else if (WinConditions.checkRows(X_LOCS) || WinConditions.checkColumns(X_LOCS)) {
				return X;
			} else if (WinConditions.isDraw(BLANK_LOCS)) {
				return DRAW;
			}

			return UNKNOWN;
		}

		// returns O or X if winner (dependant on (i, j)), Draw if no more moves avail, Unknown if nondeterminate
		byte getLocalWinner(final int i, final int j) {
			byte piece = get(i, j);
			switch (piece) {
				case O:
					if (WinConditions.checkLocalRow(O_LOCS, i, j) || WinConditions.checkLocalColumn(O_LOCS, i, j)) {
						return O;
					}

					break;
				case X:
					if (WinConditions.checkLocalRow(X_LOCS, i, j) || WinConditions.checkLocalColumn(X_LOCS, i, j)) {
						return X;
					}

					break;
				default:
					throw new IllegalStateException("Location of \"get\" did not return a valid piece");
			}

			if (WinConditions.isDraw(BLANK_LOCS)) {
				return DRAW;
			}

			return UNKNOWN;
		}

		@Override
		public int hashCode() {
			return HASH;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}

			if (!(obj instanceof Node)) {
				return false;
			}

			Node n = (Node)obj;
			return this.O_LOCS == n.O_LOCS && this.X_LOCS == n.X_LOCS;
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

	private static class WinConditions {
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
			long iPos = 1L << Move.index(i, j);
			for (long winCondition = 0x8080_8080_0000_0000L; winCondition != 0x0080_8080; winCondition >>>= 1) {
				if ((winCondition&iPos) != 0 && (column&winCondition) == winCondition) {
					return true;
				}
			}

			return false;
		}

		static boolean isDraw(long locs) {
			return locs == 0L;
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

	private static void testLocalWinConditions() {
		Node n = new Node(0L, 0L);
		n = n.set(3, 3, X);
		n = n.set(3, 4, X);
		n = n.set(3, 5, X);
		n = n.set(3, 6, X);
		System.out.format("%s%n", n);
		byte winner = n.getLocalWinner(3, 5);
		String result = null;
		switch (winner) {
			case UNKNOWN:	result = "UNKNOWN"; break;
			case DRAW:		result = "DRAW"; break;
			case O:		result = "O"; break;
			case X:		result = "X"; break;
		}

		System.out.format("Winner: %s%n", result);
	}
}
