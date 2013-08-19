/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertCottbusSolution2Matsim
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.counts.CountSimComparison;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.eventsfilter.FeatureNetworkLinkCenterCoordFilter;
import playground.dgrether.analysis.simsimanalyser.CountsShapefileWriter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010CrossingSolution;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010Solution2Matsim;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010SolutionTXTParser10;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;

/**
 * @author dgrether
 * @author tthunig
 * 
 */
public class KS2010VsMatimVolumes {

	private static Map<Id, Double> loadKS2010Volumes(String directory,
			String inputFile) {
		KS2010SolutionTXTParser10 solutionParser = new KS2010SolutionTXTParser10();
		solutionParser.readFile(directory + inputFile);

		DgIdPool idPool = DgIdPool.readFromFile(directory
				+ "id_conversions.txt");
		DgIdConverter dgIdConverter = new DgIdConverter(idPool);

		Map<Integer, Double> ks2010StreetIdFlow = solutionParser
				.getStreetFlow();

		Map<Id, Double> ks2010volumes = convertKS2010Volumes(idPool,
				dgIdConverter, ks2010StreetIdFlow);
		return ks2010volumes;
	}

	private static Map<Id, Double> convertKS2010Volumes(DgIdPool idPool,
			DgIdConverter dgIdConverter, Map<Integer, Double> ks2010StreetIdFlow) {
		// convert ks2010_id to matsim_id in the unsimplified network
		Map<Id, Double> matsimLinkIdFlow = new HashMap<Id, Double>();
		for (Integer intStreetId : ks2010StreetIdFlow.keySet()) {
			String stringStreetId = idPool.getStringId(intStreetId);
			Id linkId = dgIdConverter.convertStreetId2LinkId(new IdImpl(
					stringStreetId));
			// assign the flow to all links that belongs to the simplified link
			String[] unsimplifiedLinks = linkId.toString().split("-");
			for (int i = 0; i < unsimplifiedLinks.length; i++)
				matsimLinkIdFlow.put(new IdImpl(unsimplifiedLinks[i]),
						ks2010StreetIdFlow.get(intStreetId));
		}
		return matsimLinkIdFlow;
	}

	private static Map<Id, Double> loadMatsimVolumes(Network matsimNetwork,
			Network ks2010Network, String matsimEventsFile, int startTime,
			int endTime, double scalingFactor) {

		VolumesAnalyzer va = loadVolumesFromEvents(matsimEventsFile,
				matsimNetwork);

		// convert matsim flow volumes
		Map<Id, Double> matsimVolumes = new HashMap<Id, Double>();
		for (Link l : matsimNetwork.getLinks().values()) {

			// bound matsim flow volumes to KS2010 region
			if (ks2010Network.getLinks().containsKey(l.getId())) {
				double[] volumes = va.getVolumesPerHourForLink(l.getId());

				// aggregate matsim flow volumes for the respective peak
				double aggregatedFlow = 0;
				for (int i = startTime; i < endTime; i++)
					aggregatedFlow += volumes[i];
				// scale matsim flow volumes to the KS2010 demand
				aggregatedFlow *= scalingFactor;

				matsimVolumes.put(l.getId(), aggregatedFlow);
			}
		}

		return matsimVolumes;
	}

	private static VolumesAnalyzer loadVolumesFromEvents(
			String matsimEventsFile, Network matsimNetwork) {
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600, matsimNetwork);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(va);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(matsimEventsFile);
		return va;
	}

	private static Network loadNetwork(String networkFile) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(networkFile);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		Network network = scenario.getNetwork();
		return network;
	}

	private static void writeFlowVolumesShp(Network ks2010Network, String srs,
			Map<Id, Double> ks2010Volumes, Map<Id, Double> matsimVolumes,
			String outputFile) {

		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);

//		Network filteredNetwork = applyNetworkFilter(ks2010Network, networkSrs);
//
//		new VolumesShapefileWriter(filteredNetwork, networkSrs).writeShape(
//				outputFile, ks2010Volumes, matsimVolumes);
		
		new VolumesShapefileWriter(ks2010Network, networkSrs).writeShape(
				outputFile, ks2010Volumes, matsimVolumes);
	}

	private static Network applyNetworkFilter(Network network,
			CoordinateReferenceSystem networkSrs) {
		// log.info("Filtering network...");
		// log.info("Nr links in original network: " +
		// network.getLinks().size());
		NetworkFilterManager netFilter = new NetworkFilterManager(network);
		Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple = CottbusUtils
				.loadCottbusFeature("/media/data/work/repos/shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
		FeatureNetworkLinkCenterCoordFilter filter = new FeatureNetworkLinkCenterCoordFilter(
				networkSrs, cottbusFeatureTuple.getSecond(),
				cottbusFeatureTuple.getFirst());
		netFilter.addLinkFilter(filter);
		Network fn = netFilter.applyFilters();
		// log.info("Nr of links in filtered network: " + fn.getLinks().size());
		return fn;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Tuple<String, String>> input = new ArrayList<Tuple<String, String>>();
		input.add(new Tuple<String, String>("50", "morning"));
//		input.add(new Tuple<String, String>("50", "evening"));
//		input.add(new Tuple<String, String>("10", "morning"));
//		input.add(new Tuple<String, String>("10", "evening"));

		// TODO check run number
		String runNumber = "1722";
		
		for (Tuple<String, String> i : input) {

			String ksSolutionDirectory = "C:/Users/Atany/Desktop/SHK/SVN/projects_cottbus/cb2ks2010/2013-07-31_minflow_" + i.getFirst() + "_" + i.getSecond() + "_peak/";
			String matsimRunDirectory = "C:/Users/Atany/Desktop/SHK/SVN/run" + runNumber + "/";
//			String ksSolutionDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_" + i.getFirst() + "_" + i.getSecond() + "_peak/";
//			String matsimRunDirectory = DgPaths.REPOS + "runs-svn/run" + runNumber + "/";
			
			String ksSolutionFile;
			// start and end time of the respective peak in hours
			// TODO check times
			int startTime;
			int endTime;
			
			if (i.getSecond().equals("evening")){
				ksSolutionFile = "ksm_" + i.getFirst() + "a_sol.txt";
				startTime = 13;
				endTime = 24;
			}
			else{
				ksSolutionFile = "ksm_" + i.getFirst() + "m_sol.txt";
				startTime = 0;
				endTime = 13;
			}

			// KS2010 demands proportion of the matsim demand
			double scalingFactor;
			if (i.getFirst().equals("50"))
				scalingFactor = 0.55;
			else
				scalingFactor = 0.27;

			// unsimplified networks
			String matsimNetworkFile = matsimRunDirectory + runNumber + ".output_network.xml.gz";
			String ks2010NetworkFile = ksSolutionDirectory + "network_small_clean.xml.gz";
			String matsimEventsFile = matsimRunDirectory + "ITERS/it.1000/" + runNumber + ".1000.events.xml.gz";
			String srs = TransformationFactory.WGS84_UTM33N;

			// TODO change outputDirectory?
			String outputFile = ksSolutionDirectory + "shapes/KS2010VsMatsimRun" + runNumber + "FlowVolumes";

			
			Network matsimNetwork = loadNetwork(matsimNetworkFile);
			Network ks2010Network = loadNetwork(ks2010NetworkFile);

			Map<Id, Double> ks2010Volumes = loadKS2010Volumes(ksSolutionDirectory,
					ksSolutionFile);
			Map<Id, Double> matsimVolumes = loadMatsimVolumes(matsimNetwork,
					ks2010Network, matsimEventsFile, startTime, endTime,
					scalingFactor);

			writeFlowVolumesShp(ks2010Network, srs, ks2010Volumes,
					matsimVolumes, outputFile);
		}

	}

}
