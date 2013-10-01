package redsgreens.Appleseed;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class AppleseedPlayerManager {
	private Appleseed pl = null;
	private WorldGuardPlugin WorldGuard = null;

	public AppleseedPlayerManager(Appleseed plugin) {
		this.pl = plugin;
		// attempt to hook to the worldguard plugin
		try {
			Plugin test = pl.getServer().getPluginManager().getPlugin("WorldGuard");

			if (WorldGuard == null) {
				if (test != null) {
					WorldGuard = (WorldGuardPlugin) test;
					pl.getLogger().info(WorldGuard.getDescription().getName() + " " + WorldGuard.getDescription().getVersion() + " found.");
				}
			}
		} catch (Exception ex) {
			WorldGuard = null;
			pl.getLogger().info("WorldGuard not found, we will not be interfacing with it.");
		}
	}

	public boolean hasPermission(Player player, String permission) {
		boolean isOp = player.isOp();
		if (isOp == false) {
			if (pl.getAppleseedConfig().AllowNonOpAccess == true) {
				if (permission.length() >= 5 && permission.toLowerCase().substring(0, 5).equalsIgnoreCase("sign."))
					return true;
				if (permission.length() >= 6 && permission.toLowerCase().substring(0, 6).equalsIgnoreCase("plant."))
					return true;
			}

			boolean retval = player.hasPermission("appleseed." + permission);

			if (retval == false && permission.length() >= 6 && permission.toLowerCase().substring(0, 6).equalsIgnoreCase("plant."))
				retval = player.hasPermission("appleseed.plant.*");

			if (retval == false)
				retval = player.hasPermission("appleseed.*");

			return retval;
		} else {
			return player.hasPermission("appleseed." + permission);
		}
	}

	public boolean canBuild(Player p, Block b) {
		if (WorldGuard != null)
			return WorldGuard.canBuild(p, b);
		else
			return true;
	}
}
