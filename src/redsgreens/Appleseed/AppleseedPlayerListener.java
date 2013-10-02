package redsgreens.Appleseed;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handle onPlayerInteract event
 * 
 * @author redsgreens
 */
public class AppleseedPlayerListener implements Listener {
	private Appleseed pl;

	public AppleseedPlayerListener(Appleseed plugin) {
		this.pl = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		// catch player right-click events
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

		else if(blockType == Material.LOG && iStack.getType() == pl.getAppleseedConfig().FertilizerItem.getMaterial() && iStack.getDurability() == pl.getAppleseedConfig().FertilizerItem.getDurability())
			// player is trying to fertilize a tree
			handleFertilzeEvent(event, player, iStack, block);

		else if(blockType == Material.LOG && iStack.getType() == pl.getAppleseedConfig().WandItem.getMaterial())
			// player used the wand on a tree
			handleWandEvent(event, player, iStack, block);
	}

	private void handlePlantEvent(PlayerInteractEvent event, Player player, ItemStack iStack, Block block) {
		// they might have planted something, do some more checks

		// try to get the type of the tree they are planting
		AppleseedTreeType treeType = null;
		AppleseedItemStack aiStack = new AppleseedItemStack(iStack);
		if(pl.getAppleseedConfig().TreeTypes.containsKey(aiStack))
			treeType = pl.getAppleseedConfig().TreeTypes.get(aiStack);

		// return if they don't have an allowed item in hand
		if(treeType == null)
			return;

		// return if the block above is not air
		Block blockRoot = block.getRelative(BlockFace.UP);
		if(blockRoot.getType() != Material.AIR)
			return;

		// return if they don't have permission
		if(!pl.getPlayerManager().hasPermission(player, "plant." + AppleseedItemStack.getItemStackName(aiStack)) || !pl.getPlayerManager().canBuild(player, blockRoot)) {
			if(pl.getAppleseedConfig().ShowErrorsInClient)
				player.sendMessage(ChatColor.RED + "Err: You don't have permission to plant this tree.");
			event.setCancelled(true);
			return;
		}
		
		// return if they have already planted their share of trees
		if(pl.getAppleseedConfig().MaxTreesPerPlayer != -1 && !pl.getPlayerManager().hasPermission(player, "infinite.cap")) {
			String playerName = player.getName();
			String worldName = player.getLocation().getWorld().getName();
			if(!pl.getTreeManager().CanPlayerAddTree(playerName, worldName)) {
				if(pl.getAppleseedConfig().ShowErrorsInClient) {
					if(pl.getAppleseedConfig().MaxIsPerWorld)
						player.sendMessage(ChatColor.RED + "Err: You are not allowed to plant more trees in this world. (" + pl.getTreeManager().getPlayerTreeCount(playerName, worldName) + "/" + pl.getAppleseedConfig().MaxTreesPerPlayer.toString() + ")");
					else
						player.sendMessage(ChatColor.RED + "Err: You are not allowed to plant more trees. (" + pl.getTreeManager().getPlayerTreeCount(playerName) + "/" + pl.getAppleseedConfig().MaxTreesPerPlayer.toString() + ")");
				}
				event.setCancelled(true);
				return;
			}
		}

		if(pl.getAppleseedConfig().MinimumTreeDistance != -1) {
			// MinimumTreeDistance is set, make sure this tree won't be too close to another
			if(pl.getTreeManager().IsNewTreeTooClose(blockRoot.getLocation())) {
				if(pl.getAppleseedConfig().ShowErrorsInClient)
					player.sendMessage(ChatColor.RED + "Err: Too close to another tree.");
				event.setCancelled(true);
				return;
			}
		}

		// all tests satisfied, proceed

		// cancel the event so we're the only one processing it
		event.setCancelled(true);

		// add the root location and type to the list of trees
		if(pl.getPlayerManager().hasPermission(player, "infinite.plant"))
			pl.getTreeManager().AddTree(new AppleseedLocation(blockRoot.getLocation()), aiStack, AppleseedCountMode.Infinite, -1, -1, -1, player.getName());
		else
			pl.getTreeManager().AddTree(new AppleseedLocation(blockRoot.getLocation()), aiStack, player.getName());

		// set the clicked block to dirt
		block.setType(Material.DIRT);

		// plant a sapling
		blockRoot.setType(Material.SAPLING);
		blockRoot.setData(treeType.getSaplingData());

		// take the item from the player
		if(player.getGameMode() != GameMode.CREATIVE) {
			if(iStack.getAmount() == 1)
				player.setItemInHand(null);
			else {
				iStack.setAmount(iStack.getAmount() - 1);
				player.setItemInHand(iStack);			
			}
		}
	}

	private void handleFertilzeEvent(PlayerInteractEvent event, Player player, ItemStack iStack, Block block) {
		// they might be fertilizing a tree

		Location loc = block.getLocation();
		if(!pl.getTreeManager().isTree(loc))
			return;

		// cancel the event so we're the only one processing it
		event.setCancelled(true);

		AppleseedTreeData tree = pl.getTreeManager().GetTree(new AppleseedLocation(loc));

		Boolean treesUpdated = false;
		if(pl.getPlayerManager().hasPermission(player, "infinite.fertilizer")) {
			tree.setInfinite();
			treesUpdated = true;
		} else {
			if(tree.Fertilize()) {
				tree.ResetDropCount();
				treesUpdated = true;
			} else {
				if(pl.getAppleseedConfig().ShowErrorsInClient)
					player.sendMessage(ChatColor.RED + "Err: This tree cannot be fertilized.");
				event.setCancelled(true);
				return;
			}
		}

		if(treesUpdated == true)
			if(tree.hasSign())
				pl.getTreeManager().updateSign(tree);

		// take the item from the player
		if(iStack.getAmount() == 1)
			player.setItemInHand(null);
		else {
			iStack.setAmount(iStack.getAmount() - 1);
			player.setItemInHand(iStack);			
		}
	}

	private void handleWandEvent(PlayerInteractEvent event, Player player, ItemStack iStack, Block block) {
		// they clicked with the wand
		if(!pl.getPlayerManager().hasPermission(player, "wand")) {
			if(pl.getAppleseedConfig().ShowErrorsInClient)
				player.sendMessage(ChatColor.RED + "Err: You don't have permission to do this.");
			event.setCancelled(true);
			return;
		}

		// cancel the event so we're the only one processing it
		event.setCancelled(true);

		Location loc = block.getLocation();
		if(!pl.getTreeManager().isTree(loc)) {
			player.sendMessage(ChatColor.RED + "Err: This is not an Appleseed tree.");
			return;
		} else {
			AppleseedTreeData tree = pl.getTreeManager().GetTree(new AppleseedLocation(loc));
			AppleseedItemStack treeIS = tree.getItemStack();

			player.sendMessage(ChatColor.RED + "Appleseed: Type=" + AppleseedItemStack.getItemStackName(treeIS) + ", NeedsFertilizer=" + tree.needsFertilizer() + ", HasSign=" + tree.hasSign());				
		}
	}
}
