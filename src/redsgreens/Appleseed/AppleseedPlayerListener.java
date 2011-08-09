package redsgreens.Appleseed;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;

/**
 * Handle onPlayerInteract event
 * 
 * @author redsgreens
 */
public class AppleseedPlayerListener extends PlayerListener {

	@Override
    public void onPlayerInteract(PlayerInteractEvent event)
    // catch player right-click events
    {
    	// return if the event is already cancelled, or if it's not a right-click event
		if(event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Block block = event.getClickedBlock();
		Material blockType = block.getType();
		
		// return if the player didn't right click on farmland or tree
		if(blockType != Material.SOIL && blockType != Material.LOG)
			return;

		Player player = event.getPlayer();
		ItemStack iStack = player.getItemInHand();

		if(iStack == null)
			return;

		if(blockType == Material.SOIL)
			// player is trying to plant something
			handlePlantEvent(event, player, iStack, block);
		
		else if(blockType == Material.LOG && iStack.getType() == Appleseed.Config.FertilizerItem.getMaterial() && iStack.getDurability() == Appleseed.Config.FertilizerItem.getDurability())
			// player is trying to fertilize a tree
			handleFertilzeEvent(event, player, iStack, block);
		
		else if(blockType == Material.LOG && iStack.getType() == Appleseed.Config.WandItem.getMaterial())
			// player used the wand on a tree
			handleWandEvent(event, player, iStack, block);
    }
	
	private void handlePlantEvent(PlayerInteractEvent event, Player player, ItemStack iStack, Block block)
	{
		// they might have planted something, do some more checks

		// try to get the type of the tree they are planting
		AppleseedTreeType treeType = null;
		AppleseedItemStack aiStack = new AppleseedItemStack(iStack);
		if(Appleseed.Config.TreeTypes.containsKey(aiStack))
			treeType = Appleseed.Config.TreeTypes.get(aiStack);
		
		// return if they don't have an allowed item in hand
		if(treeType == null)
			return;
		
		// return if the block above is not air
		Block blockRoot = block.getRelative(BlockFace.UP);
		if(blockRoot.getType() != Material.AIR)
			return;

		// return if they don't have permission
		if(!Appleseed.PlayerManager.hasPermission(player, "plant." + AppleseedItemStack.getItemStackName(aiStack)) || !Appleseed.PlayerManager.canBuild(player, blockRoot))
		{
			if(Appleseed.Config.ShowErrorsInClient)
				player.sendMessage("�cErr: You don't have permission to plant this tree.");
			event.setCancelled(true);
			return;
		}
		// return if they have already planted their share of trees
		if(Appleseed.Config.MaxTreesPerPlayer != -1 && !Appleseed.PlayerManager.hasPermission(player, "infinite.cap"))
		{
			String playerName = player.getName();
			String worldName = player.getLocation().getWorld().getName();
			if(!Appleseed.PlayerManager.CapAddTree(playerName, worldName))
			{
				if(Appleseed.Config.ShowErrorsInClient)
				{
					if(Appleseed.Config.MaxIsPerWorld)
						player.sendMessage("�cErr: You are not allowed to plant more trees in this world. (" + Appleseed.PlayerManager.getTreeCount(playerName, worldName) + "/" + Appleseed.Config.MaxTreesPerPlayer.toString() + ")");
					else
						player.sendMessage("�cErr: You are not allowed to plant more trees. (" + Appleseed.PlayerManager.getTreeCount(playerName) + "/" + Appleseed.Config.MaxTreesPerPlayer.toString() + ")");
				}
				event.setCancelled(true);
				return;
			}
		}
		
		if(Appleseed.Config.MinimumTreeDistance != -1)
		{
			// MinimumTreeDistance is set, make sure this tree won't be too close to another
			if(Appleseed.TreeManager.IsNewTreeTooClose(blockRoot.getLocation()))
			{
				if(Appleseed.Config.ShowErrorsInClient)
					player.sendMessage("�cErr: Too close to another tree.");
				event.setCancelled(true);
				return;
			}
		}

		// all tests satisfied, proceed
		
		// cancel the event so we're the only one processing it
		event.setCancelled(true);
		
		// add the root location and type to the list of trees
		if(Appleseed.PlayerManager.hasPermission(player, "infinite.plant"))
			Appleseed.TreeManager.AddTree(new AppleseedLocation(blockRoot.getLocation()), aiStack, AppleseedCountMode.Infinite, -1, -1, -1, player.getName());
		else
			Appleseed.TreeManager.AddTree(new AppleseedLocation(blockRoot.getLocation()), aiStack, player.getName());
		
		// set the clicked block to dirt
		block.setType(Material.DIRT);
		
		// plant a sapling
		blockRoot.setType(Material.SAPLING);
		blockRoot.setData(treeType.getSaplingData());
		
		// take the item from the player
		if(iStack.getAmount() == 1)
			player.setItemInHand(null);
		else
		{
			iStack.setAmount(iStack.getAmount() - 1);
			player.setItemInHand(iStack);			
		}
	}
	
	private void handleFertilzeEvent(PlayerInteractEvent event, Player player, ItemStack iStack, Block block)
	{
		// they might be fertilizing a tree
		
		Location loc = block.getLocation();
		if(!Appleseed.TreeManager.isTree(loc))
			return;

		// cancel the event so we're the only one processing it
		event.setCancelled(true);

		AppleseedTreeData tree = Appleseed.TreeManager.GetTree(new AppleseedLocation(loc));
		
		Boolean treesUpdated = false;
		if(Appleseed.PlayerManager.hasPermission(player, "infinite.fertilizer"))
		{
			tree.setInfinite();
			treesUpdated = true;
		}
		else
		{
			if(tree.Fertilize())
			{
				tree.ResetDropCount();
				treesUpdated = true;
			}
			else
			{
				if(Appleseed.Config.ShowErrorsInClient)
					player.sendMessage("�cErr: This tree cannot be fertilized.");
				event.setCancelled(true);
				return;
			}
		}

		if(treesUpdated == true)
			if(tree.hasSign())
				Appleseed.TreeManager.updateSign(tree);

		// take the item from the player
		if(iStack.getAmount() == 1)
			player.setItemInHand(null);
		else
		{
			iStack.setAmount(iStack.getAmount() - 1);
			player.setItemInHand(iStack);			
		}
	}
	
	private void handleWandEvent(PlayerInteractEvent event, Player player, ItemStack iStack, Block block)
	{
		// they clicked with the wand
		if(!Appleseed.PlayerManager.hasPermission(player, "wand"))
		{
			if(Appleseed.Config.ShowErrorsInClient)
				player.sendMessage("�cErr: You don't have permission to do this.");
			event.setCancelled(true);
			return;
		}

		// cancel the event so we're the only one processing it
		event.setCancelled(true);

		Location loc = block.getLocation();
		if(!Appleseed.TreeManager.isTree(loc))
		{
			player.sendMessage("�cErr: This is not an Appleseed tree.");
			return;
		}
		else
		{
			AppleseedTreeData tree = Appleseed.TreeManager.GetTree(new AppleseedLocation(loc));
			AppleseedItemStack treeIS = tree.getItemStack();

			player.sendMessage("�cAppleseed: Type=" + AppleseedItemStack.getItemStackName(treeIS) + ", NeedsFertilizer=" + tree.needsFertilizer().toString());				
		}
	}


}

