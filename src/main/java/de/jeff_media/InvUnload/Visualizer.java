package de.jeff_media.InvUnload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Visualizer {
	
	private Main main;
	HashMap<UUID,ArrayList<Block>> lastUnloads;
	HashMap<UUID,Location> lastUnloadPositions;
	HashMap<UUID,Integer> activeVisualizations;
	HashMap<UUID,ArrayList<Laser>> activeLasers;

	//ArrayList<Location> destinations;
	//Player p;
	
	// Barrier: okay

	
	protected Visualizer(Main main) {
		this.main = main;
		lastUnloads = new HashMap<UUID,ArrayList<Block>>();
		lastUnloadPositions = new HashMap<UUID,Location>();
		//activeVisualizations = new HashMap<UUID,Integer>();
		activeLasers = new HashMap<UUID,ArrayList<Laser>>();
		
		if(main.getConfig().getBoolean("laser-moves-with-player")) {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

			short timer=0;
			@Override
			public void run() {
				timer++;
				if(activeLasers==null) return;
				if(activeLasers.size()==0) return;
				
				for(Entry<UUID,ArrayList<Laser>> entry : activeLasers.entrySet()) {
					Player p = main.getServer().getPlayer(entry.getKey());
					if(p==null) {
						stopLaser(entry.getKey());
						continue;
					}
					
					//ArrayList<Block> lastUnload = lastUnloads.get(entry.getKey());
					for(Laser laser : entry.getValue()) {
						try {
							laser.moveStart(p.getLocation().add(0, 0.75, 0));
							if(timer>50) {
								laser.callColorChange();

							}
						} catch (ReflectiveOperationException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
					if(timer>50) timer=0;
				}
				
			}
			
		}, 0, 2);
		}
		
		
	}
	
	/*void cancelVisualization(int id) {
		Bukkit.getScheduler().cancelTask(id);
	}*/
	
	/*void cancelVisualization(Player p) {
		if(activeVisualizations.containsKey(p.getUniqueId())) {
			cancelVisualization(activeVisualizations.get(p.getUniqueId()));
		}
		activeVisualizations.remove(p.getUniqueId());
	}*/
	
	void save(Player p, ArrayList<Block> affectedChests) {
		lastUnloads.put(p.getUniqueId(), affectedChests);
		lastUnloadPositions.put(p.getUniqueId(),p.getLocation().add(0,0.75,0));
		/*if(activeVisualizations.containsKey(p.getUniqueId())) {
			cancelVisualization(activeVisualizations.get(p.getUniqueId()));
			//play(p);
		}*/
	}
	
	/*void play(Player p) {
		if(lastUnloads.containsKey(p.getUniqueId())) {
			play(lastUnloads.get(p.getUniqueId()),p);
		}
	}*/

	/*void play(ArrayList<Block> destinations, Player p, double interval, int count, Particle particle,double speed,int maxDistance) {
		for(Block destination : destinations) {
			Location start = p.getLocation();
			Vector vec = getDirectionBetweenLocations(start, BlockUtils.getCenterOfBlock(destination).add(0, -0.5, 0));
			if(start.distance(destination.getLocation())<maxDistance) {
	            for (double i = 1; i <= start.distance(destination.getLocation()); i += interval) {
	                vec.multiply(i);
	                start.add(vec);
	                p.spawnParticle(particle, start, count, 0, 0, 0, speed);
	                start.subtract(vec);
	                vec.normalize();
	            }
            }
		}
	}*/
	
	/*void play(ArrayList<Block> affectedChests, Player p) {
		// Visualize
				
				int task = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {
				    public void run() {
				    	// TODO: Move the declarations out of the Runnable
				    	Particle particle = Particle.CRIT;
				    	int count = 1;
				    	int maxDistance = 128;
				    	double interval = 0.3;
				    	double speed = 0.001;
				    	play(affectedChests, p, interval, count, particle,speed,maxDistance);
				    }
				}, 0, 2);
				
				activeVisualizations.put(p.getUniqueId(), task);
				
				new BukkitRunnable() {
					public void run() {
						Bukkit.getServer().getScheduler().cancelTask(task);
					}
				}.runTaskLater(main, 100);
	}*/
	
	void toggleLaser(Player p,int duration) {
		if(lastUnloads.containsKey(p.getUniqueId())
				&& lastUnloads.get(p.getUniqueId()).size()>0 
				& !activeLasers.containsKey(p.getUniqueId())) {
			playLaser(lastUnloads.get(p.getUniqueId()),p,duration);
		} else {
			stopLaser(p.getUniqueId());
		}
	}
	
	void stopLaser(UUID p) {
		ArrayList<Laser> lasers = activeLasers.get(p);
		if(lasers==null) return;
		for(Laser laser : lasers) {
			if(laser.isStarted()) laser.stop();
			laser = null;
		}
		lasers = null;
		activeLasers.remove(p);
	}
	
	void playLaser(ArrayList<Block> affectedChests,Player p,int duration) {
		stopLaser(p.getUniqueId());
		ArrayList<Laser> lasers = new ArrayList<Laser>();
		Location loc = lastUnloadPositions.get(p.getUniqueId());
		for(Block block : affectedChests) {
			try {
				Laser laser = new Laser(loc, BlockUtils.getCenterOfBlock(block).add(0, -1, 0), duration, main.getConfig().getInt("laser-max-distance"));
				laser.start(main);
				lasers.add(laser);
			} catch (ReflectiveOperationException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				main.getLogger().warning("Could not start laser for player "+p.getName());
			}
		}
		if(lasers.size()>0) {
			activeLasers.put(p.getUniqueId(), lasers);
		}
	}
	
	void chestAnimation(Block block, Player player) {
		final Location loc = BlockUtils.getCenterOfBlock(block);
		
		if(main.getConfig().getBoolean("spawn-particles")) {
			if(main.getConfig().getBoolean("error-particles")) {
				main.getLogger().warning("Cannot spawn particles, because particle type \""+main.getConfig().getString("particle-type")+"\" does not exist! Please check your config.yml");
			} else {
				final int particleCount = main.getConfig().getInt("particle-count");
				final Particle particle = Particle.valueOf(main.getConfig().getString("particle-type").toUpperCase());
				block.getWorld().spawnParticle(particle, loc, particleCount, 0.0, 0.0, 0.0);
			}
		}
		
		if(main.getConfig().getBoolean("play-sound")) {
			if(main.getConfig().getBoolean("error-sound")) {
				main.getLogger().warning("Cannot play sound, because sound effect \""+main.getConfig().getString("sound-effect")+"\" does not exist! Please check your config.yml");
			}
			else {
				final Sound sound = Sound.valueOf(main.getConfig().getString("sound-effect").toUpperCase());
				player.playSound(loc, sound, 1, 1);
			}
		}
	}
	
    /*private static Vector getDirectionBetweenLocations(Location start, Location end) {
        return end.toVector().subtract(start.toVector());
    }*/

}