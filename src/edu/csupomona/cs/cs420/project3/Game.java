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

/**
 * This class represents the game of 4-in-a-Line, a game where two users face
 * off in an epic struggle taking turns placing pieces until one of them can
 * manage to get 4 of their own pieces in a line. This class also maintains an
 * AI solution which will play against the player.
 * 
 * @author Collin Smith <strong>collinsmith@csupomona.edu</strong>
 */
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

	/**
	 * Main method executed at program start which will initialize this game
	 * and start it.
	 * 
	 * @param args program arguments; none expected
	 */
	public static void main(String[] args) {
		//testMove();
		//testNode();
		//testActions();
		//testLocalWinConditions();

		Game g = new Game(Heuristics.DEFAULT);
		g.run();
	}

	/**
	 * Constants representing different pieces (players)
	 */
	private static final byte BLANK = 0;
	private static final byte O = 1 << 0;
	private static final byte X = 1 << 1;

	/**
	 * Returns the character value of a piece.
	 * 
	 * @param piece piece to return char value of
	 * @return char value of the piece
	 */
	private static char toChar(byte piece) {
		switch (piece) {
			case BLANK:	return '-';
			case O:	return 'O';
			case X:	return 'X';
			default:	throw new IllegalArgumentException("Invalid piece specified: " + piece);
		}
	}

	/**
	 * WinCondition constants.
	 */
	
	/** Represents a state in which the winner cannot be determined */
	private static final byte UNKNOWN	= -1;
	/** Represents a state in which the game is a draw (no winner, and no more moves can be made */
	private static final byte DRAW	= 0;

	/**
	 * Represents the value of an action that represents no action (a null move)
	 */
	private static final byte NULL_ACTION = -1;

	/** Represents the heuristic used with this instance of Game */
	private final Heuristic HEURISTIC;
	/** Represents the Scanner used to take input from the console */
	private final Scanner SCAN;

	/** Represents whether or not the player is making the first move (X's) */
	private final boolean PLAYER_FIRST;
	/** Represents the amount of time that the AI has to search the game tree to find a move */
	private final long AI_TIME;

	/** Array which stores each action made, indexed by move id */
	private final int[] MOVES;
	/** Current move number */
	private int move;

	//private final long[][] TRANSPOSITION;

	/** Represents the current State of the game board */
	private Node currentState;

	/**
	 * Constructs a Game passing a specified heuristic which will be used to
	 * determine the objective value of each successor.
	 * 
	 * @param heuristic evaluation function to use when determining the order
	 *	of successors
	 */
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

	/**
	 * Starts the Game and manage the game loop. Terminates when one player
	 * wins, or there is a critical error within the search tree algorithm.
	 */
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

	/**
	 * Resolves the player id (O, X or BLANK) at the current value of moves.
	 * 
	 * @return current moving player
	 */
	private byte resolvePlayer() {
		return resolvePlayer(move);
	}

	/**
	 * Resolves which player is active at a specified number of moves.
	 * 
	 * @param move value of moves to evaluate
	 * 
	 * @return player making the action on that move
	 */
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

	/**
	 * Displays the list of moves made so far without a line terminator on
	 * the final line, allowing players to make easier interaction with the
	 * move interface.
	 */
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

	/**
	 * This class represents a heuristic function, which is simple a function
	 * that evaluates a given state and returns the utility value of that
	 * state relative to the last moved piece.
	 */
	private static interface Heuristic {
		/**
		 * Examines a given state and returns the utility value of that
		 * state relative to the last moved piece.
		 * 
		 * @param state state to be examined
		 * @param piece piece of which the utility should be determined for
		 * @param x x-location of the piece
		 * @param y y-location of the piece
		 * 
		 * @return utility value of this board for piece
		 */
		int evaluate(Node state, byte piece, int x, int y);
	}

	/**
	 * This class represents an iterative-deepening search with alpha-beta
	 * pruning.
	 */
	private static class IDSABSearch implements Callable<Integer> {
		/** Game to perform the search on */
		final Game GAME;
		/** Initial state of the search (root node) */
		final Node INITIAL_STATE;
		/** Evaluation function to use to determine the utility values of nodes */
		final Heuristic HEURISTIC;

		/** current depth limit of the search */
		volatile int depthLimit;
		/** best action this search has found so far */
		volatile int bestAction;
		/** whether or not the search has finished */
		volatile boolean searched;
		/** whether or not the search is searching still */
		volatile boolean searching;

		/** the maximum depth this search has reached */
		int maxDepthSearched;
		/** the number of nodes expanded */
		int nodesExpanded;
		/** the total time this algorithm has spent searching before termination */
		long timeElapsed;

		/**
		 * Constructs a searcher using the given parameters as initial state
		 * 
		 * @param game game to search
		 * @param initialState initial state of the search
		 * @param heuristic  evaluation function used to determine the
		 *	utility value of each state
		 */
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

		/**
		 * Performs the search and returns the best action of the search.
		 * This method is designed to be interrupted or return the current
		 * best action found at any given time.
		 * 
		 * @return best action possible
		 * @throws Exception propagated exceptions from the search
		 */
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

		/**
		 * Performs a min search on a given node and returns the utility
		 * 
		 * @param currentState current state to perform on
		 * @param i last move i location
		 * @param j last move j location
		 * @param piece piece moved
		 * @param alpha high value of the search
		 * @param beta low value of the search
		 * @param depth depth of the search
		 * @return floor of the search
		 */
		private int min(Node currentState, int i, int j, byte piece, int alpha, int beta, int depth) {
			nodesExpanded++;
			maxDepthSearched = Math.max(maxDepthSearched, depth);

			Node successor = currentState.set(i, j, piece);
			byte winner = successor.getLocalWinner(i, j);
			if (winner != UNKNOWN || depthLimit <= depth) {
				return HEURISTIC.evaluate(successor, piece, i, j);
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

		/**
		 * Performs a max search on a given node and returns the utility
		 * 
		 * @param currentState current state to perform on
		 * @param i last move i location
		 * @param j last move j location
		 * @param piece piece moved
		 * @param alpha high value of the search
		 * @param beta low value of the search
		 * @param depth depth of the search
		 * @return floor of the search
		 */
		int max(Node currentState, int i, int j, byte piece, int alpha, int beta, int depth) {
			nodesExpanded++;
			maxDepthSearched = Math.max(maxDepthSearched, depth);

			Node successor = currentState.set(i, j, piece);
			byte winner = successor.getLocalWinner(i, j);
			if (winner != UNKNOWN || depthLimit <= depth) {
				return HEURISTIC.evaluate(successor, piece, i, j);
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

		/**
		 * Returns statistics for this search.
		 * 
		 * @return the elapsed time, number of nodes expanded and max depth
		 *	reached
		 */
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

	/**
	 * This class represents methods used to determine moves, as well as
	 * compact and index them to the board representation.
	 */
	private static class Move {
		/** mask of a byte used to store i values */
		static final int I_MASK = 0xF0;
		/** mask of a byte used to store j values */
		static final int J_MASK = 0x0F;

		/**
		 * Returns an unmasked i value.
		 * 
		 * @param compacted value to extract i from
		 * 
		 * @return the value of i in the compacted value
		 */
		static final int unmaski(int compacted) {
			return compacted >>> 4;
		}

		/**
		 * Returns an unmasked j value.
		 * 
		 * @param compacted value to extract j from
		 * 
		 * @return the value of j in the compacted value
		 */
		static final int unmaskj(int compacted) {
			return compacted & J_MASK;
		}

		/**
		 * Indexes a given i, j pair into a 0-63 range (inclusive).
		 * 
		 * @param i i value of a space
		 * @param j j value of a space
		 * @return indexed value of that space
		 */
		static int index(int i, int j) {
			return ((i << 3) + j);
		}

		/**
		 * Returns the i value of an indexed value.
		 * 
		 * @param indexed indexed value to extract i from
		 * 
		 * @return i value
		 */
		static int unindexi(int indexed) {
			return (indexed >>> 3);
		}

		/**
		 * Returns the j value of an indexed value.
		 * 
		 * @param indexed indexed value to extract j from
		 * 
		 * @return j value
		 */
		static int unindexj(int indexed) {
			return (indexed & 7);
		}

		/**
		 * Compacts a given indexed value into a single byte.
		 * 
		 * @param indexed indexed value to extract and then compact
		 * 
		 * @return a single byte containing both the i,j values
		 */
		static int compact(int indexed) {
			int i = (unindexi(indexed) << 4);
			int j = unindexj(indexed);
			return i|j;
		}

		/**
		 * Returns the letter value of an i location.
		 * 
		 * @param i value to convert
		 * 
		 * @return letter value of an i location
		 */
		static char toLetter(int i) {
			return (char)('A' + i);
		}

		/**
		 * Returns the number value of a j location.
		 * 
		 * @param j value to convert
		 * 
		 * @return numeric value of a j location
		 */
		static int toNumber(int j) {
			return j + 1;
		}
	}

	/**
	 * This class represents a Node which stores all state information about
	 * a Game board.
	 */
	private static class Node {
		/** long which stores the location of all indexed O's */
		final long O_LOCS;
		/** long which stores the location of all indexed X's */
		final long X_LOCS;
		/** long which stores the location of all blank spaces ~(O_LOCS|X_LOCS) */
		final long BLANK_LOCS;

		/** hashed value of this Node */
		final int HASH;

		/**
		 * Constructs a Node using the specified board params.
		 * 
		 * @param oLocs long storing flags for the position of this O's are
		 * @param xLocs long storing flags for the position of this X's are 
		 */
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

		/**
		 * Returns the piece at a given (i, j)
		 * 
		 * @return O, X or BLANK of that position
		 */
		byte get(int i, int j) {
			long flag = 1L << Move.index(i, j);
			if ((O_LOCS&flag) != 0) {
				return O;
			} else if ((X_LOCS&flag) != 0) {
				return X;
			}

			return BLANK;
		}

		/**
		 * Returns the new Node created with the additional (i,j) set to the
		 * given piece.
		 * 
		 * @param i i location to set
		 * @param j j location to set
		 * @param piece value of the piece to set at that location
		 * 
		 * @return a new Node with the new position set to the piece
		 *	specified
		 */
		Node set(int i, int j, byte piece) {
			assert get(i, j) == BLANK : String.format("piece at %c%d must be blank!", Move.toLetter(i), Move.toNumber(j));
			long flag = 1L << Move.index(i, j);
			switch (piece) {
				case O: return new Node(O_LOCS|flag, X_LOCS);
				case X: return new Node(O_LOCS, X_LOCS|flag);
				default: throw new IllegalArgumentException(String.format("val should be one of O (%d) or X (%d)", O, X));
			}
		}

		/**
		 * Returns a list of possible actions that can be taken from this
		 * state (blank locations).
		 * 
		 * @return list of possible <strong>compacted</strong> actions
		 *	remaining
		 * 
		 * @see Move#compact(int)
		 */
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

		/**
		 * Returns the winner of this state.
		 * 
		 * @return the winner value of this state or UNKNOWN if unable to
		 *	determine.
		 */
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

		/**
		 * Returns the winner only checking the positions relative to the
		 * specified location.
		 * 
		 * @param i i location to check
		 * @param j j location to check
		 * 
		 * @return the winner of this Node searched locally
		 */
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

		/**
		 * {@inheritDoc} 
		 */
		@Override
		public int hashCode() {
			return HASH;
		}

		/**
		 * {@inheritDoc} 
		 */
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

		/**
		 * Returns a String representation of this Node
		 * 
		 * @return String representation of this board that can be printed
		 */
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

	/**
	 * This class represents the methods that are used to determine winners
	 * of a given state using longs to represents who owns that state.
	 */
	private static class WinConditions {
		/**
		 * Checks the given flags to see if there are 4 in a row
		 * 
		 * @param locs the location flags to check
		 * 
		 * @return {@code true} of the owners of locs has won, otherwise
		 *	{@code false}
		 */
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

		/**
		 * Checks the given flags to see if there are 4 in a row locally
		 * 
		 * @param locs the location flags to check
		 * @param i i location to check
		 * @param j j location to check
		 * 
		 * @return {@code true} of the owners of locs has won, otherwise
		 *	{@code false}
		 */
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

		/**
		 * Checks the given flags to see if there are 4 in a column
		 * 
		 * @param locs the location flags to check
		 * 
		 * @return {@code true} of the owners of locs has won, otherwise
		 *	{@code false}
		 */
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

		/**
		 * Checks the given flags to see if there are 4 in a column locally
		 * 
		 * @param locs the location flags to check
		 * @param i i location to check
		 * @param j j location to check
		 * 
		 * @return {@code true} of the owners of locs has won, otherwise
		 *	{@code false}
		 */
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

		/**
		 * Returns whether or not the given flags are all blank.
		 * 
		 * @param locs location flags to check
		 * 
		 * @return {@code true} if no more moves can be made, otherwise
		 *	{@code false}
		 */
		static boolean isDraw(long locs) {
			return locs == 0L;
		}
	}

	/**
	 * Method used for testing purposes
	 */
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

	/**
	 * Method used for testing purposes
	 */
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

	/**
	 * Method used for testing purposes
	 */
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

	/**
	 * Method used for testing purposes
	 */
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
