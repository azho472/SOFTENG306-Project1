package scheduler;

import java.util.ArrayList;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import scheduler.Greedy.QueueItem;

import org.graphstream.graph.Edge;

public class ScheduleHelper {


	//Find the root nodes 
	public static ArrayList<Integer> findRootNodes(Graph g) {

		ArrayList<Integer> rootNodes = new ArrayList<Integer>();
		int i = 0;

		for (Node n:g) {
			if (n.getInDegree() == 0) {
				rootNodes.add(i);
			}
			i++;
		}

		return rootNodes;
	}
	
	public static int getNodeWeight(Graph g, int nodeIndex){
		return Integer.parseInt(g.getNode(nodeIndex).getAttribute("Weight").toString());
	}
	

	// After a node has been processed, call this function to return all new nodes that can be processed
	public static ArrayList<Integer> processableNodes(Graph g, int nodeIndex) {

		ArrayList<Integer> processableNodes = new ArrayList<Integer>();

		// Get all the leaving edges of the node just processed
		Iterable<Edge> ite = g.getNode(nodeIndex).getEachLeavingEdge();

		boolean nodeProcessable;
		// Get all the child nodes of the node that was just processed, for each child node, check whether all it's parent nodes have been processed
		for (Edge e: ite) {
			Node n = e.getNode1();
			nodeProcessable = true;

			// Check all of child node's parent nodes have been processed
			Iterable<Edge> childIte = g.getNode(n.getId()).getEachEnteringEdge();
			for (Edge childEdge: childIte) {
				Node parentNode = childEdge.getNode0();
				
				if (Integer.parseInt(parentNode.getAttribute("processorID").toString()) == -1) {
					nodeProcessable = false;
					break;
				}
			}

			if (nodeProcessable == true) {
				processableNodes.add(n.getIndex());
			}
		}

		return processableNodes;
	}

	//needs to return the cost of putting the queue item into the processor
	public static int scheduleNode(Schedule schedule, QueueItem q, Graph g) {
		
		int nodeWeight = getNodeWeight(g, q.nodeIndex);
		int scheduleIncrease = 0;
		ArrayList<Integer> parentNodeCosts = new ArrayList<Integer>(); // This stores the cost of putting the queue item into the specified pid when coming from each parent node
		ArrayList<Node> parentNodes = new ArrayList<Node>(); // Stores the parent node queue item comes from
		
		//Get the post-processed processorLength of the queueitem from each of the parent nodes
		for (Edge e:g.getNode(q.nodeIndex).getEachEnteringEdge()) {
			Node parentNode = e.getNode0();
			int parentProcessor = Integer.parseInt(parentNode.getAttribute("processorID").toString());
			
			//if parent node was processed on the same processor than the queue item can be added with just nodeWeight
			if (q.processorID == parentProcessor) {
				parentNodeCosts.add(schedule.procLengths[q.processorID] + nodeWeight);
				parentNodes.add(parentNode);
			} else {
				//parent node was not processed on the same processor
				
				//need to find when the parent node finished processing
				int parentNodeFinishedProcessing = Integer.parseInt(parentNode.getAttribute("Start").toString()) + getNodeWeight(g, parentNode.getIndex());
				
				//if the parent node finished processing longer than the weight of the edge to the child then can add automatically to the processor
				if (schedule.scheduleLength - parentNodeFinishedProcessing >= Integer.parseInt(e.getAttribute("Weight").toString())){
					parentNodeCosts.add(schedule.procLengths[q.processorID] + nodeWeight);
					parentNodes.add(parentNode);
				} else {
					//find out how long need to wait before can add to processor
					
					//time left to wait
					int timeToWait = Integer.parseInt(e.getAttribute("Weight").toString()) - (schedule.scheduleLength - parentNodeFinishedProcessing);
					
					parentNodeCosts.add(schedule.procLengths[q.processorID] + nodeWeight + timeToWait);
					parentNodes.add(parentNode);
				}
			}
			
		}
		
		int minimumProcLength = parentNodeCosts.get(0);
		for(int i: parentNodeCosts) {
			if (i < minimumProcLength) {
				minimumProcLength = i;
			}
		}
		
		return minimumProcLength;
				
	}
}
