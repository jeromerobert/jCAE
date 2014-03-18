/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2014, by EADS France
 */

package org.jcae.mesh.amibe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Implementation of A* algorithm
 * http://en.wikipedia.org/wiki/A*_search_algorithm
 * @author Jerome Robert
 */
public abstract class AStar<Node> {
	private static class Score<Node> implements Comparable<Score<Node>>
	{
		public Node node;
		/** the actual shortest distance traveled from initial node to current node */
		public double g;
		/** the estimated (or "heuristic") distance from current node to goal */
		public double h;
		/** the sum of g(x) and h(x) */
		public double f;

		@Override
		public int compareTo(Score<Node> o)
		{
			if (f == o.f)
			{
				int thisVal = System.identityHashCode(this);
				int anotherVal = System.identityHashCode(o);
				return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
			}
			else return Double.compare(f, o.f);
		}

		public Score(Node node)
		{
			this.node = node;
		}
		public Score(Node node, double g, double h, double f)
		{
			this.node = node;
			this.g = g;
			this.h = h;
			this.f = f;
		}

		@Override
		public String toString()
		{
			return "{ " +node.toString() + " g=" + g + " h=" + h + " f=" + f +" }";
		}
	}

	protected abstract double heuristicDistance(Node n1, Node n2);
	/** Distance between 2 neighboor nodes */
	protected abstract double distance(Node n1, Node n2);

	protected abstract Iterable<Node> neighborNodes(Node n);

	private double pathLength = Double.NaN;

	private List<Node> reconstructPath(Map<Node, Node> cameFrom, Node current)
	{
		ArrayList<Node> toReturn = new ArrayList<Node>(cameFrom.size()+1);
		while(current != null)
		{
			toReturn.add(current);
			current = cameFrom.get(current);
		}
		Collections.reverse(toReturn);
		return toReturn;
	}

	public List<Node> find(Node start, Node goal)
	{
		return find(start, goal, Integer.MAX_VALUE);
	}

	public List<Node> find(Node start, Node goal, int maxIter)
	{
		//The set of nodes already evaluated.
		Set<Node> closetSet = new HashSet<Node>();
		//The set of tentative nodes to be evaluated.
		SortedSet<Score<Node>> openSet = new TreeSet<Score<Node>>();
		Map<Node, Score<Node>> openMap = new HashMap<Node, Score<Node>>();
		Map<Node, Node> cameFrom = new HashMap<Node, Node>();
		double d = heuristicDistance(start, goal);
		Score<Node> ss = new Score<Node>(start, 0, d, d);
		openSet.add(ss);
		openMap.put(start, ss);
		int it = 0;
		Score<Node> x = null;
		while(!openSet.isEmpty())
		{
			x = openSet.first();
			if(x.node.equals(goal) || it > maxIter)
			{
				pathLength = x.g;
				return reconstructPath(cameFrom, x.node);
			}
			it++;
			openSet.remove(x);
			openMap.remove(x.node);
			closetSet.add(x.node);
			int cpt = 0;
			for(Node y:neighborNodes(x.node))
			{
				cpt++;
				if(closetSet.contains(y))
					continue;
				double tentativeGScore=x.g + distance(x.node, y);
				boolean tentativeIsBetter = false;
				Score<Node> sy = openMap.get(y);
				if(sy == null)
				{
					sy = new Score<Node>(y);
					openSet.add(sy);
					openMap.put(y, sy);
					sy.h = heuristicDistance(y, goal);
					tentativeIsBetter = true;
				}
				else if(tentativeGScore < sy.g)
					tentativeIsBetter = true;

				if(tentativeIsBetter)
				{
					cameFrom.put(y, x.node);
					sy.g = tentativeGScore;
					openSet.remove(sy);
					sy.f = sy.g + sy.h;
					openSet.add(sy);
				}
			}
		}

		if(goal == null)
		{
			pathLength = x.g;
			return reconstructPath(cameFrom, x.node);
		}
		else
		{
			pathLength = Double.NaN;
			return null;
		}
	}

	public double getPathLength() {
		return pathLength;
	}
}
