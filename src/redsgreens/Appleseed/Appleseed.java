package redsgreens.Appleseed;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

/**
 * Appleseed for Bukkit
 *
 * @author redsgreens
 */
public class Appleseed extends JavaPlugin {
	private AppleseedPlayerListener playerListener = null;
	private AppleseedBlockListener blockListener = null;
	private AppleseedWorldListener worldListener = null;

	private AppleseedConfig config;
	private AppleseedPlayerManager playerManager;
	private AppleseedTreeManager treeManager;

	public void onEnable() {
		playerListener = new AppleseedPlayerListener(this);
		blockListener = new AppleseedBlockListener(this);
		worldListener = new AppleseedWorldListener(this);

		// initialize the config object and load the config 
		config = new AppleseedConfig();
		config.LoadConfig(this);

		// initialize the player manager
		playerManager = new AppleseedPlayerManager(this);

		// initialize the tree manager
		treeManager = new AppleseedTreeManager(this);

		// register our events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(playerListener, this);
		pm.registerEvents(blockListener, this);
		pm.registerEvents(worldListener, this);

		// start the timer
		treeManager.ProcessTrees();

		getLogger().info(getDescription().getName() + " version " + getDescription().getVersion() + " is enabled!");
	}

	public void onDisable() {
		if(!AppleseedTreeManager.SaveRunning)
			treeManager.saveTrees();

		blockListener = null;
		playerListener = null;
		worldListener = null;
		getLogger().info(getDescription().getName() + " version " + getDescription().getVersion() + " is disabled.");
	}

	public AppleseedConfig getAppleseedConfig() {
		return this.config;
	}

	/** Returns the {@link AppleseedPlayerManager} instance. */
	public AppleseedPlayerManager getPlayerManager() {
		return this.playerManager;
	}

	/** Returns the {@link AppleseedTreeManager} instance. */
	public AppleseedTreeManager getTreeManager() {
		return this.treeManager;
	}
}
