/* *********************************************************************** *
 * project: org.matsim.*
 * ReRouteLandmarks.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ReRouteLandmarks extends ReRouteDijkstra {

	private final AStarLandmarksFactory factory;
	private PlansCalcRouteConfigGroup configGroup = null;

	public ReRouteLandmarks(Config config, Network network, TravelCost costCalculator,
			TravelTime timeCalculator, TravelMinCost minCostCalculator) {
		super(config, network, costCalculator, timeCalculator);
		this.factory = new AStarLandmarksFactory(network, minCostCalculator);
		this.configGroup = config.plansCalcRoute();
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRoute(this.configGroup, 
				this.network, this.costCalculator, this.timeCalculator, this.factory);
	}

}
