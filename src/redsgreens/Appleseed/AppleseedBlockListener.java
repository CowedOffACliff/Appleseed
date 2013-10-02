package redsgreens.Appleseed;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

public class AppleseedBlockListener implements Listener {
	private Appleseed pl;
	
	public AppleseedBlockListener(Appleseed plugin) {
		this.pl = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSignChange(SignChangeEvent event){
		// return if the event is already cancelled
		if (event.isCancelled()) return;

		Block signBlock = event.getBlock();

		// only wall signs on trees supported
		if(signBlock.getType() != Material.WALL_SIGN)
			return;
		
		pl.getLogger().info("The block type is a wall sign.");
		
		// get the block behind the sign
		Block blockAgainst =  getBlockBehindWallSign((Sign)signBlock.getState());
		
		// the sign must be on a tree
		if(blockAgainst.getType() != Material.LOG)
			return;
		
		pl.getLogger().info("The block against it is a log.");
		
		pl.getLogger().info("The first line is: " + event.getLine(0));
		pl.getLogger().info("And I need it to : [" + pl.getAppleseedConfig().SignTag + "]");
		
		// only proceed if it's a new sign
		if (event.getLine(0).equalsIgnoreCase("[" + pl.getAppleseedConfig().SignTag + "]")) {
			AppleseedLocation aloc = new AppleseedLocation(blockAgainst.getLocation());
			final AppleseedTreeData tree = pl.getTreeManager().GetTree(aloc);

			// player placed an appleseed sign that isn't against a tree 
			if(tree == null)
				return;

			// cancel the event so we're the only one processing it
//			event.setCancelled(true);

			Player player = event.getPlayer();
			Location signLoc = signBlock.getLocation();

			if(tree.hasSign()) {
				// this tree already has a sign, destroy this sign and give it back to the player
				signBlock.setType(Material.AIR);
				signBlock.getWorld().dropItemNaturally(signLoc, new ItemStack(Material.SIGN, 1));
				if(pl.getAppleseedConfig().ShowErrorsInClient)
					player.sendMessage(ChatColor.RED + "Err: This tree already has a sign.");
				return;
			}

			if(!pl.getPlayerManager().hasPermission(player, "sign.place")) {
				signBlock.setType(Material.AIR);
				signBlock.getWorld().dropItemNaturally(signLoc, new ItemStack(Material.SIGN, 1));
				if(pl.getAppleseedConfig().ShowErrorsInClient)
					player.sendMessage(ChatColor.RED + "Err: You don't have permission to place this sign.");
				return;
			}

			// set the first line to blue
			event.setLine(0, ChatColor.BLUE + "[" + pl.getAppleseedConfig().SignTag + "]");

			// save the sign location
			tree.setSign(signLoc);
			
			
			pl.getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
			    public void run() {
			    	pl.getTreeManager().updateSign(tree);
			    }
			}, 0);

		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		// return if the event is already cancelled
		if (event.isCancelled()) return;

		Block signBlock = event.getBlock();

		// return if it's not a wall sign
		if(signBlock.getType() != Material.WALL_SIGN)
			return;
	
		Sign sign = (Sign)signBlock.getState();
		
		// return if it's not an appleseed sign
		if(!ChatColor.stripColor(sign.getLine(0)).equals("[" + pl.getAppleseedConfig().SignTag + "]"))
			return;
		
		AppleseedTreeData tree = pl.getTreeManager().GetTree(new AppleseedLocation(getBlockBehindWallSign(sign).getLocation()));
		
		// it looks like an appleseed sign but there's no tree behind it
		if(tree == null)
			return;
		
		// set the sign value to null for this tree
		tree.setSign(null);
	}
	
	
	// get the block that has a wall sign on it
	private Block getBlockBehindWallSign(Sign sign) {
		Block blockAgainst = null;
		org.bukkit.material.Sign sign2 = (org.bukkit.material.Sign) sign.getData();
		Block signBlock = sign.getBlock();
		
		if(sign.getType() == Material.WALL_SIGN) {
			switch(sign2.getFacing()){ // determine sign direction and get block behind it
			case EAST: // facing east
				blockAgainst = signBlock.getRelative(BlockFace.WEST);
				break;
			case WEST: // facing west
				blockAgainst = signBlock.getRelative(BlockFace.EAST);
				break;
			case NORTH: // facing north
				blockAgainst = signBlock.getRelative(BlockFace.SOUTH);
				break;
			case SOUTH: // facing south
				blockAgainst = signBlock.getRelative(BlockFace.NORTH);
				break;
			}
		}
		
		return blockAgainst;
	}
}
