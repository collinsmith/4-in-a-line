package edu.csupomona.cs.cs420.project3;

import java.util.Scanner;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class Main	{
   private static List<Move> moves;

	public static void main(String[]	args)	{
      Main.moves = new ArrayList<>(64);
      
      final Scanner SCAN = new Scanner(System.in);

      Move last = null;      
      State current = new State();
      while (moves.size() < 64) {
         System.out.format("Turn %d:%n", moves.size()+1);
         System.out.format("%s%n", current);
         
         if (last != null && State.isWinningMove(current, last.i, last.j)) {
            System.out.format("Player %d has won!%n", (moves.size()&1) != 0 ? 1 : 2);
            break;
         } else {
            displayMoves();
         }
         
         String next = SCAN.nextLine();
         int i = Character.isUpperCase(next.charAt(0)) ? next.charAt(0)-'A' : next.charAt(0)-'a';
         int j = next.charAt(1)-'0'-1;
         current = current.set(i, j, (moves.size()&1) == 0 ? State.O : State.X);

         last = new Move(i, j);
         moves.add(last);
      }
	}
   
   private static void displayMoves() {
      if (moves == null) {
         throw new IllegalStateException();
      }
      
      System.out.format("Move List:%n");
      System.out.format("    O  X%n");

      Iterator<Move> it = moves.iterator();
      for (int i = 0; true; i++) {
         System.out.format("%2d.", i+1);
         if (it.hasNext()) {
            System.out.format(" %s", it.next());
         } else {
            break;
         }
         
         if (it.hasNext()) {
            System.out.format(" %s", it.next());
         } else {
            break;
         }
         
         System.out.format("%n");
      }
   }

   private static class Move {
      final int i;
      final int j;
   
      Move(int i, int j) {
         this.i = i;
         this.j = j;
      }
      
      @Override
      public String toString() {
         return String.format("%c%d", 'A'+i, j+1);
      }
   }
	
	private static	class	State	{
		static final int	O = 1<<0;
		static final int	X = 1<<1;
		
		final	long O_LOCS;
		final	long X_LOCS;
		
		State() {
			this(0L,	0L);
		}
		
		State(long oLocs,	long xLocs)	{
			this.O_LOCS	= oLocs;
			this.X_LOCS	= xLocs;
		}
		
		static int	getShift(int i, int j) {
			return ((i<<3)+j);
		}
		
		int get(int i, int j) {
			long mod	= 1L<<getShift(i,	j);
			long o =	O_LOCS&mod;
			long x =	X_LOCS&mod;
			if	(o	!=	0)	{
				return O;
			} else if (x != 0) {
				return X;
			} else {
				return 0;
			}
		}
		
		State	set(int i,	int j, int val)	{
			int shift = getShift(i, j);
			long o =	O_LOCS;
			long x =	X_LOCS;
			if	(val == O) {
				o |= 1L<<shift;
			} else if (val	==	X)	{
				x |= 1L<<shift;
			} else {
				throw	new IllegalArgumentException();
			}
			
			return new State(o, x);
		}
		
      static boolean isWinningMove(State s, int i, int j) {
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
			
			long loc;
			for (int	i = 0; i	< 8; i++) {
				sb.append(((char)('A'+i)));
				sb.append(' ');
				
				for (int	j = 0; j	< 8; j++) {
					loc =	get(i, j);
					if	(loc == 0) {
						sb.append("- ");
					} else if (loc	==	O)	{
						sb.append("O ");
					} else if (loc	==	X)	{
						sb.append("X ");
					} else {
						throw	new IllegalStateException();
					}
				}
				
				sb.append('\n');
			}

			sb.deleteCharAt(sb.length()-1);
			return sb.toString();
		}
	}
}
