package no.uib.bjo013.mspacman.map;

/*    
 * A* algorithm implementation.
 * Copyright (C) 2007, 2009 Giuseppe Scrivano <gscrivano@gnu.org>

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
			
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

import java.util.*;

/**
 * A* algorithm implementation using the method design pattern.
 * 
 * @author Giuseppe Scrivano
 */
public abstract class AStar<T>
{
		private class Path implements Comparable<T>{
				public T point;
				public Double f;
				public Double g;
				public Path parent;
				
				/**
				 * Default c'tor.
				 */
				public Path(){
						parent = null;
						point = null;
						g = f = 0.0;
				}

				/**
				 * C'tor by copy another object.
				 * 
				 * @param p The path object to clone.
				 */
				public Path(Path p){
						this();
						parent = p;
						g = p.g;
						f = p.f;
				}

				/**
				 * Compare to another object using the total cost f.
				 *
				 * @param o The object to compare to.
				 * @see       Comparable#compareTo()
				 * @return <code>less than 0</code> This object is smaller
				 * than <code>0</code>;
				 *        <code>0</code> Object are the same.
				 *        <code>bigger than 0</code> This object is bigger
				 * than o.
				 */
				public int compareTo(Object o){
						@SuppressWarnings("unchecked")
						Path p = (Path)o;
						return (int)(f - p.f);
				}

				/**
				 * Get the last point on the path.
				 *
				 * @return The last point visited by the path.
				 */
				public T getPoint(){
						return point;
				}

				/**
				 * Set the 
				 */
				public void setPoint(T p){
						point = p;
				}
		}

		/**
		 * Check if the current node is a goal for the problem.
		 *
		 * @param node The node to check.
		 * @return <code>true</code> if it is a goal, <code>false</else> otherwise.
		 */
		protected abstract boolean isGoal(T node);

		/**
		 * Cost for the operation to go to <code>to</code> from
		 * <code>from</from>.
		 *
		 * @param from The node we are leaving.
		 * @param to The node we are reaching.
		 * @return The cost of the operation.
		 */
		protected abstract Double g(T from, T to);

		/**
		 * Estimated cost to reach a goal node.
		 * An admissible heuristic never gives a cost bigger than the real
		 * one.
		 * <code>from</from>.
		 *
		 * @param from The node we are leaving.
		 * @param to The node we are reaching.
		 * @return The estimated cost to reach an object.
		 */
		protected abstract Double h(T from, T to);


		/**
		 * Generate the successors for a given node.
		 *
		 * @param node The node we want to expand.
		 * @return A list of possible next steps.
		 */
		protected abstract List<T> generateSuccessors(T node);


		private  PriorityQueue<Path> paths;
		private  HashMap<T, Double> mindists;
		private static Double lastCost;
		private static int expandedCounter;

		/**
		 * Check how many times a node was expanded.
		 *
		 * @return A counter of how many times a node was expanded.
		 */
		public int getExpandedCounter(){
				return expandedCounter;
		}

		/**
		 * Default c'tor.
		 */
		public AStar(){
				paths = new PriorityQueue<Path>();
				mindists = new HashMap<T, Double>();
				expandedCounter = 0;
				lastCost = 0.0;
		}


		/**
		 * Total cost function to reach the node <code>to</code> from
		 * <code>from</code>.
		 *  
		 * The total cost is defined as: f(x) = g(x) + h(x).
		 * @param from The node we are leaving.
		 * @param to The node we are reaching.
		 * @return The total cost.
		 */
		protected Double f(Path p, T from, T to){
				Double g =  g(from, to) + ((p.parent != null) ? p.parent.g : 0.0);
				Double h = h(from, to);

				p.g = g;
				p.f = g + h;

				return p.f;
		}

		/**
		 * Expand a path.
		 *
		 * @param path The path to expand.
		 */
		private void expand(Path path){
				T p = path.getPoint();
				Double min = mindists.get(path.getPoint());

				/*
				 * If a better path passing for this point already exists then
				 * don't expand it.
				 */
				if(min == null || min.doubleValue() > path.f.doubleValue())
						mindists.put(path.getPoint(), path.f);
				else
						return;

				List<T> successors = generateSuccessors(p);

				for(T t : successors){
						Path newPath = new Path(path);
						newPath.setPoint(t);
						f(newPath, path.getPoint(), t);
						paths.offer(newPath);
				}

				expandedCounter++;
		}

		/**
		 * Get the cost to reach the last node in the path.
		 *
		 * @return The cost for the found path.
		 */
		public Double getCost(){
				return lastCost;
		}


		/**
		 * Find the shortest path to a goal starting from
		 * <code>start</code>.
		 *
		 * @param start The initial node.
		 * @return A list of nodes from the initial point to a goal,
		 * <code>null</code> if a path doesn't exist.
		 */
		public List<T> compute(T start){
			paths.clear();
			mindists.clear();
			expandedCounter = 0;
			lastCost = 0.0;
				try{
						Path root = new Path();
						root.setPoint(start);

						/* Needed if the initial point has a cost.  */
						f(root, start, start);

						expand(root);

						for(;;){
								Path p = paths.poll();

								if(p == null){
										lastCost = Double.MAX_VALUE;
										return new LinkedList<T>();
								}

								T last = p.getPoint();

								lastCost = p.g;

								if(isGoal(last)){
										LinkedList<T> retPath = new LinkedList<T>();

										for(Path i = p; i != null; i = i.parent){
												retPath.addFirst(i.getPoint());
										}

										return retPath;
								}
								expand(p);
						}
				}
				catch(Exception e){
						e.printStackTrace();
				}
				return null;
						
		}
}