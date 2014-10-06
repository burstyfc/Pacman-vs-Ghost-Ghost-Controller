package pacman.entries.ghosts;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Random;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;
import pacman.game.internal.*;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */
public class MyGhosts extends Controller<EnumMap<GHOST,MOVE>>
{
	private EnumMap<GHOST, MOVE> myMoves=new EnumMap<GHOST, MOVE>(GHOST.class);
	private MOVE[] allMoves=MOVE.values();
	private Random rnd=new Random();
	private final static int PILL_PROXIMITY=15;
	//Mode switch timer
	int currentTime = 0;
	// timer pause or not
	boolean pause = false;
	//top-left corner
	int topLeft = 0;
	//top-right corner
	int topRight = 78;
	//bottom-left corner
	int botLeft = 1191;
	//bottom-right corner
	int botRight = 1291;
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue)
	{
		myMoves.clear();
		// if pacman was eaten, reset chase timer
		if(game.wasPacManEaten())
		currentTime = 0;
		
		// if the ghosts are edible, pause the chase timer
		if(!pause)
		currentTime++;
		
		
		
		//Place your game logic here to play the game as the ghosts
		for(GHOST ghostType : GHOST.values())
		{
			if(game.doesGhostRequireAction(ghostType))
			{
				// if nghost is edible, run away from pacman and pause the chase timer
				if(game.getGhostEdibleTime(ghostType)>0 || closeToPower(game))	
				{
					pause = true;
					//retreat from Ms Pac-Man if edible or if Ms Pac-Man is close to power pill
					myMoves.put(ghostType,game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghostType),
							game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghostType),DM.PATH));
					return myMoves;
				}
				else pause = false;

			 // switch from chase mode to scatter mode in different time intervals
			 if (currentTime <= 190 || (currentTime > 630 && currentTime <= 820) || 
			    (currentTime > 1260 && currentTime <= 1380) || (currentTime > 1820 && currentTime <= 1940) ) 
			    {
				 scatterMode(game,ghostType,myMoves);
			    }
				else {
			// move toward pacman
			if(ghostType == GHOST.BLINKY)
			{
			 myMoves.put(ghostType,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType),
			 game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghostType),DM.PATH));
			}
			// move toward 4 tiles in front of pacman
			 if(ghostType == GHOST.PINKY)
			 {
				 int node =   game.getPacmanCurrentNodeIndex();
				 MOVE lastMove = game.getPacmanLastMoveMade();
				 // find the node which is 4 tiles(24 nodes) in 
				 // front of pacman.
				 for(int i = 0; i < 24; i++)
				 {
					
					MOVE[] m = game.getPossibleMoves(node);
					boolean hasDir = false;
					for(int j = 0; j < m.length;j++)
					{
						if(m[j] == lastMove)
						hasDir = true;
					}
					if(hasDir)
					node = game.getNeighbour(node,lastMove);
					else
					{
					int node2 = game.getNeighbouringNodes(node,lastMove)[0];
					lastMove = game.getApproximateNextMoveTowardsTarget(node,node2,lastMove,DM.PATH);
					node = node2;
					}
				 }
				 //Move ghost toward the target node.
				 myMoves.put(ghostType,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType),
						  node,game.getGhostLastMoveMade(ghostType),DM.PATH));
				 
				 
				 
			 }
			 // First find the node 2 tiles in front of pacman. 
			 // Then locate the target node based on blinky's current node
			 // and the node found before. 
			 if(ghostType == GHOST.INKY)
			 {
				 int node =   game.getPacmanCurrentNodeIndex();
				 MOVE lastMove = game.getPacmanLastMoveMade();
				 //To find the node which was 2 tiles (12nodes) in front of 
				 // pacman.
				 for(int i = 0; i < 12; i++)
				 {
					int node2 = game.getNeighbouringNodes(node,lastMove)[0];
					lastMove = game.getApproximateNextMoveTowardsTarget(node,node2,lastMove,DM.PATH);
					node = node2;				  
				 }
				 //find the x distance between blinky's current node and the node found before
				 int xDist = game.getNodeXCood(node) - game.getNodeXCood(game.getGhostCurrentNodeIndex(GHOST.BLINKY));
				 //find the y distance between blinky's current node and the node found before
				 int yDist = game.getNodeYCood(node) - game.getNodeYCood(game.getGhostCurrentNodeIndex(GHOST.BLINKY));
				 //find the target node in maze given distance above
				 int nodeI = findClosestNodeInMaze(game.getNodeXCood(node) + xDist,game.getNodeYCood(node) + yDist,game);
				 
				 //Move ghost toward target node
				 myMoves.put(ghostType,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType),
						  nodeI,game.getGhostLastMoveMade(ghostType),DM.PATH));
				
			 }
			 // If the distance between sue and pacman is greater than 8 tiles, 
			 // Sue will go toward pacman. If it is less than or equal to 8 tiles,
			 // Sue will go back to his corner which located at the bottom-left of maze.
			 if (ghostType == GHOST.SUE) {
				 
					int node = game.getGhostCurrentNodeIndex(ghostType);
					MOVE lastMove = game.getGhostLastMoveMade(ghostType);
					double dist = 0;
					int node2;
					MOVE[] m = game.getPossibleMoves(node);
					boolean hasDir = false;
					for(int j = 0; j < m.length;j++)
					{
						if(m[j] == lastMove)
							hasDir = true;
					}
					if(hasDir)
						node2 = game.getNeighbour(node,lastMove);
					else
					{
						node2 = game.getNeighbouringNodes(node,lastMove)[0];
					}
					//the distance from 1 node to another
					dist = game.getDistance(node, node2, DM.PATH);
					//Generate the distance of 8 tiles, equal to 48 nodes in this framework
					dist *= 48;

					//index of target node
					int target = 0;
					//distance between ghost's current node and pacman's current node
					double distToPac = game.getDistance(game.getGhostCurrentNodeIndex(ghostType), game.getPacmanCurrentNodeIndex(), DM.PATH);
					// if the distance to pacman is greater than 8 tiles(48 nodes), move ghost toward pacman/
					if (distToPac > dist) {
						target = game.getPacmanCurrentNodeIndex();
						myMoves.put(ghostType,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType),
								target, game.getGhostLastMoveMade(ghostType),DM.PATH));
						
					}
					// if the distance to pacman is less than or equal to 8 tiles(48 nodes), move ghost back to his corner.
					if (distToPac <= dist) {
						target = botLeft;
						myMoves.put(ghostType,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType),
								target, game.getGhostLastMoveMade(ghostType),DM.PATH));
					
					}
				}

			

			 
			 
			}
		   }
		  } 
		return myMoves;
	}
	
	// given a position on the map, find the closest node to this position
	private int findClosestNodeInMaze(int nodeX, int nodeY,Game game)
	{
		int minNode = -1;
		int minDist = 999;
		for(int i = 0; i < game.getCurrentMaze().graph.length;i++)
		{
		Node n = game.getCurrentMaze().graph[i];
		int xSpot = game.getNodeXCood(n.nodeIndex);
		int ySpot = game.getNodeYCood(n.nodeIndex);	
		 if(xSpot == nodeX && ySpot == nodeY)
			 return n.nodeIndex;
		 else
		 {
		int dist = Math.abs(xSpot - nodeX) + Math.abs(ySpot - nodeY);
			 if(dist < minDist)
			 {
				 minNode = n.nodeIndex;
				 minDist = dist;
			 }
		 }
		}
		return minNode;
	}
	
	//This helper function checks if Ms Pac-Man is close to an available power pill
	private boolean closeToPower(Game game)
	{
		int[] powerPills=game.getPowerPillIndices();

		for(int i=0;i<powerPills.length;i++)
			if(game.isPowerPillStillAvailable(i) && game.getShortestPathDistance(powerPills[i],game.getPacmanCurrentNodeIndex())<PILL_PROXIMITY)
				return true;

		return false;
	}
	// sets the ghosts to move toward there respected corners in scatter mode
	private void scatterMode(Game game, GHOST ghostType,EnumMap<GHOST, MOVE> myMoves) {
		int target= 0 ;
			if (ghostType == GHOST.BLINKY) {
				target = topRight;
			}
			if (ghostType == GHOST.PINKY) {
				target = topLeft;
			}
			if (ghostType == GHOST.INKY) {
				target = botRight;
			}
			if (ghostType == GHOST.SUE) {
				target = botLeft;
			}
			myMoves.put(ghostType,game.getNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType),
					target, game.getGhostLastMoveMade(ghostType),DM.EUCLID));
			
		
		
	}

	
	
}