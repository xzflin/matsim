/* *********************************************************************** *
 * project: org.matsim.*
 * FuzzyTravelTimeEstimatorFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.router.util;

import org.matsim.core.router.util.PersonalizableTravelTimeFactory;

public class PenaltyTravelTimeFactory implements PersonalizableTravelTimeFactory {

	private final PersonalizableTravelTimeFactory timeFactory;
	private final PenaltyCalculator penaltyCalculator;
	
	public PenaltyTravelTimeFactory(PersonalizableTravelTimeFactory timeFactory, PenaltyCalculator penaltyCalculator) {
		this.timeFactory = timeFactory;
		this.penaltyCalculator = penaltyCalculator;
	}
	
	@Override
	public PenaltyTravelTime createTravelTime() {
		return new PenaltyTravelTime(timeFactory.createTravelTime(), penaltyCalculator);
	}
	
}