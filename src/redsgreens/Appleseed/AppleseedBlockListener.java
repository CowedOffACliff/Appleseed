package redsgreens.Appleseed;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

public class AppleseedBlockListener extends BlockListener {

	@Override
	public void onSignChange(SignChangeEvent event)
	{
		// return if the event is already cancelled
		if (event.isCancelled()) return;

		Block signBlock = event.getBlock();

		// only wall signs on trees supported
		if(signBlock.getType() != Material.WALL_SIGN)
			return;
		
		// get the block behind the sign
		Block blockAgainst =  getBlockBehindWallSign(new CraftSign(signBlock));
		
		// the sign must be on a tree
		if(blockAgainst.getType() != Material.LOG)
			return;
		
		// only proceed if it's a new sign
		if (event.getLine(0).equalsIgnoreCase("[Appleseed]"))
		{
			AppleseedLocation aloc = new AppleseedLocation(blockAgainst.getLocation());
			AppleseedTreeData tree = Appleseed.TreeManager.GetTree(aloc);
			
			// player placed an appleseed sign that isn't against a tree 
			if(tree == null)
				return;
			
			// cancel the event so we're the only one processing it
			event.setCancelled(true);

			Player player = event.getPlayer();
			Location signLoc = signBlock.getLocation();

			if(tree.hasSign())
			{
				// this tree already has a sign, destroy this sign and give it back to the player
				signBlock.setType(Material.AIR);
				signBlock.getWorld().dropItemNaturally(signLoc, new ItemStack(Material.SIGN, 1));
				if(Appleseed.Config.ShowErrorsInClient)
					player.sendMessage("�cErr: This tree already has a sign.");
				return;
			}
			
			if(!Appleseed.PlayerManager.hasPermission(player, "sign.place"))
			{
				signBlock.setType(Material.AIR);
				signBlock.getWorld().dropItemNaturally(signLoc, new ItemStack(Material.SIGN, 1));
				if(Appleseed.Config.ShowErrorsInClient)
					player.sendMessage("�cErr: You don't have permission to place this sign.");
				return;
			}

			// set the first line to blue
			(new CraftSign(signBlock)).setLine(0, "�1[Appleseed]");

			// save the sign location
			tree.setSign(signLoc);
			Appleseed.TreeManager.updateSign(tree);
		}
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event)
	{
		// return if the event is already cancelled
		if (event.isCancelled()) return;

		Block signBlock = event.getBlock();

		// return if it's not a wall sign
		if(signBlock.getType() != Material.WALL_SIGN)
			return;
	
		Sign sign = new CraftSign(signBlock);
		
		// return if it's not an appleseed sign
		if(!sign.getLine(0).equals("�1[Appleseed]"))
			return;
		
		AppleseedTreeData tree = Appleseed.TreeManager.GetTree(new AppleseedLocation(getBlockBehindWallSign(sign).getLocation()));
		
		// it looks like an appleseed sign but there's no tree behind it
		if(tree == null)
			return;
		
		// set the sign value to null for this tree
		tree.setSign(null);
	}
	
	
	// get the block that has a wall sign on it
	public  Block getBlockBehindWallSign(Sign sign)
	{
		Block blockAgainst = null;
		Block signBlock = sign.getBlock();

		if(sign.getType() == Material.WALL_SIGN)
		{
			switch(signBlock.getData()){ // determine sign direction and get block behind it
			case 2: // facing east
				blockAgainst = signBlock.getFace(BlockFace.WEST);
				break;
			case 3: // facing west
				blockAgainst = signBlock.getFace(BlockFace.EAST);
				break;
			case 4: // facing north
				blockAgainst = signBlock.getFace(BlockFace.SOUTH);
				break;
			case 5: // facing south
				blockAgainst = signBlock.getFace(BlockFace.NORTH);
				break;
			}
		}
		
		return blockAgainst;
	}
	

}
