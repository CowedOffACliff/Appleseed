package redsgreens.Appleseed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.Yaml;

public class AppleseedTreeManager {

    // hashmap of tree locations and types
    private static HashMap<Location, AppleseedTreeData> Trees = new HashMap<Location, AppleseedTreeData>();
    
    private Random rand = new Random();

    final int treeId = Material.LOG.getId();
    final int leafId = Material.LEAVES.getId();

	public AppleseedTreeManager()
	{
		loadTrees();
	}
	
    // loop through the list of trees and drop items around them, then schedule the next run
    public synchronized void ProcessTrees(){
    	Boolean treesRemoved = false;
    	Boolean treesUpdated = false;
    	
    	if(Trees.size() != 0){
        	Set<Location> locations = Trees.keySet();
        	Iterator<Location> itr = locations.iterator();
        	while(itr.hasNext()){
        		Location loc = itr.next();
        		if(loc == null)
        			continue;
       		
        		World world = loc.getWorld();
        		if(world == null)
        			continue;
        		
        		Block block = world.getBlockAt(loc);
        		if(block == null)
        			continue;
        		
        		Chunk chunk = world.getChunkAt(block);
        		if(chunk == null)
        			continue;
        		
        		if(world.isChunkLoaded(chunk)){
            		if(isTree(loc)){
            			ItemStack iStack = Trees.get(loc).getItemStack();
            			if(iStack != null)
            			{
            				AppleseedTreeType treeType = Appleseed.Config.TreeTypes.get(iStack);

            				if(treeType != null)
            				{
                				AppleseedTreeData tree = Trees.get(loc);
                    			Integer dropCount = tree.getDropCount(); 
                    			Integer fertilizerCount = tree.getFertilizerCount();

                    			if(rand.nextInt((Integer)(100 / treeType.getDropLikelihood())) == 0 && (dropCount > 0 || dropCount == -1))
                    			{
                    				loc.getWorld().dropItemNaturally(loc, tree.getItemStack());

                    				if(dropCount != -1)
                    				{
                    					tree.setDropCount(dropCount - 1);
                    					treesUpdated = true;
                    				}
                    			}
                    			else if(dropCount == 0 && fertilizerCount == 0)
                    			{
                    				KillTree(loc);
                    				treesUpdated = true;
                    			}
            				}
            				else
            					System.out.println("Appleseed: No TreeTypes in config.yml for \"" + iStack.getType().name().toLowerCase() + "\"");
            			}
            		}
            		else if(world.getBlockAt(loc).getType() != Material.SAPLING)
            		{
            			itr.remove();
            			treesRemoved = true;
            		}
        		}
        	}
        	if(treesRemoved || treesUpdated)
        	{
        		asyncSaveTrees();
        	}
        }

    	// reprocess the list every interval
		Appleseed.Plugin.getServer().getScheduler().scheduleSyncDelayedTask(Appleseed.Plugin, new Runnable() {
		    public void run() {
		    	ProcessTrees();
		    }
		}, Appleseed.Config.DropInterval*20);
    }

    // add a tree to the hashmap and save to disk
    public synchronized void AddTree(Location loc, ItemStack iStack, String player)
    {
    	Trees.put(loc, new AppleseedTreeData(loc, iStack, player));
    	
    	asyncSaveTrees();
    }

    // add a tree to the hashmap and save to disk
    public synchronized void AddTree(Location loc, ItemStack iStack, Integer dropcount, Integer fertilizercount,  String player)
    {
    	Trees.put(loc, new AppleseedTreeData(loc, iStack, dropcount, fertilizercount, player));
    	
    	asyncSaveTrees();
    }

    public synchronized AppleseedTreeData GetTree(Location loc)
    {
    	if(Trees.containsKey(loc))
    		return Trees.get(loc);
    	else 
    	{

    		World world = loc.getWorld();    		
        	Block block = world.getBlockAt(loc);
        	int treeCount = 0;
        	while(treeCount < 15 && block.getTypeId() == treeId && !Trees.containsKey(block.getLocation()))
        	{
        		Block blockDown = block.getFace(BlockFace.DOWN);
        		if(blockDown.getTypeId() == treeId)
        			block = blockDown;
        		else
        			break;
        		
        		treeCount++;
        	}

        	Location retval = block.getLocation();
        	if(!Trees.containsKey(retval))
        		return null;
        	else
        		return Trees.get(retval);
    	}
    }

    public synchronized Boolean IsNewTreeTooClose(Location loc)
    {
    	Set<Location> locations = Trees.keySet();
    	Iterator<Location> itr = locations.iterator();
    	
    	while(itr.hasNext())
    		if(calcDistanceSquared(itr.next(), loc) < (Appleseed.Config.MinimumTreeDistance * Appleseed.Config.MinimumTreeDistance))
    			return true;
    	
    	return false;
    }
    
    public synchronized void KillTree(Location loc)
    {
    	if(!Trees.containsKey(loc))
    		return;
    	
    	World world = loc.getWorld();
    	Block block = world.getBlockAt(loc);
    	
    	if(block.getType() != Material.LOG)
    		return;
    	
    	int i = 0;
    	while(block.getType() == Material.LOG && i < 16)
    	{
    		block.setType(Material.AIR);
    		
    		Block neighbor = block.getFace(BlockFace.EAST);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		neighbor = block.getFace(BlockFace.NORTH);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		neighbor = block.getFace(BlockFace.NORTH_EAST);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		neighbor = block.getFace(BlockFace.NORTH_WEST);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		neighbor = block.getFace(BlockFace.SOUTH);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		neighbor = block.getFace(BlockFace.SOUTH_EAST);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		neighbor = block.getFace(BlockFace.SOUTH_WEST);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		neighbor = block.getFace(BlockFace.WEST);
    		if(neighbor.getType() == Material.LOG)
    			neighbor.setType(Material.AIR);

    		block = block.getFace(BlockFace.UP);
    		i++;
    	}
    }
    
    // load trees from disk
    @SuppressWarnings("unchecked")
	private synchronized void loadTrees()
    {
    	try
    	{
            Yaml yaml = new Yaml();
            File inFile = new File(Appleseed.Plugin.getDataFolder(), "trees.yml");
            if (inFile.exists()){
                FileInputStream fis = new FileInputStream(inFile);
                ArrayList<HashMap<String, Object>> loadData = (ArrayList<HashMap<String, Object>>)yaml.load(fis);
                
                for(int i=0; i<loadData.size(); i++)
                {
                	HashMap<String, Object> treeHash = loadData.get(i);

                	AppleseedTreeData tree = AppleseedTreeData.LoadFromHash(treeHash);
                	
                	if(tree != null)
                		Trees.put(tree.getLocation(), tree);
                }
            }
    	}
    	catch (Exception ex)
    	{
            ex.printStackTrace();
    	}
    	System.out.println("Appleseed: " + ((Integer)Trees.size()).toString() + " trees loaded.");
    }
    
    // save trees to disk

    public synchronized void saveTrees()
    {
    	ArrayList<HashMap<String, Object>> saveData = new ArrayList<HashMap<String, Object>>();
    	
    	Set<Location> locations = Trees.keySet();
    	Iterator<Location> itr = locations.iterator();
    	while(itr.hasNext()){
    		Location loc = itr.next();
    		saveData.add(Trees.get(loc).MakeHashFromTree());
    	}
    	
    	try
    	{
            Yaml yaml = new Yaml();
            File outFile = new File(Appleseed.Plugin.getDataFolder(), "trees.yml");
            FileOutputStream fos = new FileOutputStream(outFile);
            OutputStreamWriter out = new OutputStreamWriter(fos);
            out.write(yaml.dump(saveData));
            out.close();
            fos.close();
    		
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
    }

    public synchronized void asyncSaveTrees()
    {
		Appleseed.Plugin.getServer().getScheduler().scheduleAsyncDelayedTask(Appleseed.Plugin, new Runnable() {
		    public void run() {
		    	saveTrees();
		    }
		}, 10);
    }
    
    // see if the given location is the root of a tree
    public final boolean isTree(Location loc)
    {
    	Location rootLoc;
    	if(Trees.containsKey(loc))
    		rootLoc = loc;
    	else
    	{
    		AppleseedTreeData tree = GetTree(loc);
    		if(tree == null)
    			return false;
    		else rootLoc = tree.getLocation();   			
    	}
    	
        final World world = rootLoc.getWorld();

        int treeCount = 0;        	
        final int rootX = rootLoc.getBlockX();
        final int rootY = rootLoc.getBlockY();
        final int rootZ = rootLoc.getBlockZ();
       
        final int maxY = 7;
        final int radius = 3;
        
        int leafCount = 0;

        if(world.getBlockTypeIdAt(rootLoc) == treeId)
        {
            for (int y = rootY; y <= rootY+maxY; y++) {
                for (int x = rootX-radius; x <= rootX+radius; x++) {
                    for (int z = rootZ-radius; z <= rootZ+radius; z++) {
                        final int blockId = world.getBlockTypeIdAt(x, y, z);
                        if(blockId == treeId) 
                        	treeCount++;
                        else if(blockId == leafId) 
                        	leafCount++;

                        if(treeCount >= 3 && leafCount >= 8)
                        	return true;
                        
                    }
                }
            }
        }
        return false;
    }

    private double calcDistanceSquared(Location loc1, Location loc2)
    {
    	if(loc1.getWorld() != loc2.getWorld())
        	return Double.MAX_VALUE;
    	
    	double dX = loc1.getX() - loc2.getX();
    	double dY = loc1.getY() - loc2.getY();
    	double dZ = loc1.getZ() - loc2.getZ();

    	Double retval = (dX*dX) + (dY*dY) + (dZ*dZ);
    	
    	if(retval.isInfinite() || retval.isNaN())
    		return Double.MAX_VALUE;
    	else
    		return retval;
    }

}
