/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmTriangles.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.contrib.socnetgen.sna.graph.mcmc;

import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;



/**
 * @author illenberger
 *
 */
public class ErgmTriangles extends ErgmTerm implements EnsembleProbability {
	
	public ErgmTriangles(double theta) {
		setTheta(theta);
	}
	
	@Override
	public <V extends Vertex> double ratio(AdjacencyMatrix<V> m, int i, int j, boolean y_ij) {
		return Math.exp(- getTheta() * m.getCommonNeighbors(i, j));
	}

}
