import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.mmga.uglobal.weapon.*;
import org.mmga.uglobal.utils.RpgAnimation;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/** Isolated disposable-world rocket regression. Never install on the user's server. */
public final class EffectsSmoke extends JavaPlugin implements Listener {
    private int explosions,primes,received;
    private boolean longFlight,ammoRoundTrip,heat,afterfire;
    @Override public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getScheduler().runTaskLater(this,() -> {
            try { startChecks(); } catch (Throwable error) { error.printStackTrace();getLogger().severe("SMOKE FAIL: setup");Bukkit.shutdown(); }
        },20L);
    }
    private void startChecks() {
        World world=Bukkit.getWorlds().getFirst();
        Location floor=world.getSpawnLocation().add(0,40,0);
        for(int x=-8;x<=8;x++) for(int z=-8;z<=8;z++) for(int y=-8;y<=0;y++) floor.clone().add(x,y,z).getBlock().setType(Material.STONE);
        new RPG().summonFireball(world,floor.clone().add(.5,4,.5),new Vector(0,-1,0),3);
        Location far=floor.clone().add(.5,20,.5);
        for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++) far.clone().add(400,y,z).getBlock().setType(Material.STONE);
        RocketProjectiles.launch(far,new Vector(1,0,0),3,null);
        if (RocketAmmo.isAmmo(new ItemStack(Material.FIRE_CHARGE)) || RocketAmmo.isAmmo(new ItemStack(Material.PRISMARINE_SHARD)))
            throw new AssertionError("Vanilla items counted as ammunition");
        if (!RocketAmmo.isAmmo(RocketAmmo.create(16))) throw new AssertionError("Custom ammunition rejected");
        PlayerInventory inventory=(PlayerInventory)Proxy.newProxyInstance(getClassLoader(),new Class[]{PlayerInventory.class},(p,m,a) -> {
            if(m.getName().equals("addItem")) { for(ItemStack item:(ItemStack[])a[0]) received+=item.getAmount();return new HashMap<Integer,ItemStack>(); }
            throw new UnsupportedOperationException(m.getName());
        });
        Player player=(Player)Proxy.newProxyInstance(getClassLoader(),new Class[]{Player.class},(p,m,a) -> {
            if(m.getName().equals("getInventory")) return inventory;
            throw new UnsupportedOperationException(m.getName());
        });
        Interaction placed=RocketAmmo.place(floor.clone().add(0,1,0));
        boolean first=RocketAmmo.retrieve(player,placed),second=RocketAmmo.retrieve(player,placed);
        ammoRoundTrip=first&&!second&&received==1;
        ItemStack weapon=new ItemStack(Material.NETHER_STAR);var meta=weapon.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(Bukkit.getPluginManager().getPlugin("uGlobalWeapon"),"special_item"),org.bukkit.persistence.PersistentDataType.STRING,"RPG");
        meta.setCustomModelData(1001);weapon.setItemMeta(meta);
        RpgAnimation.setHeat(weapon,3);RpgAnimation.setModel(weapon,1020);
        boolean recoilHot=weapon.getItemMeta().getCustomModelData()==1320;
        RpgAnimation.setHeat(weapon,2);RpgAnimation.setModel(weapon,1001);
        boolean idleWarm=weapon.getItemMeta().getCustomModelData()==1201;
        RpgAnimation.setHeat(weapon,0);
        heat=recoilHot&&idleWarm&&weapon.getItemMeta().getCustomModelData()==1001;
        Bukkit.getScheduler().runTaskLater(this,() -> {
            afterfire=BlastAfterfire.activeCount()>0;
            getLogger().info("AFTERFIRE visible: "+afterfire);
        },50L);
        Bukkit.getScheduler().runTaskLater(this,() -> {
            longFlight=RocketProjectiles.activeCount()==1&&world.getEntitiesByClass(org.bukkit.entity.Fireball.class).isEmpty();
            getLogger().info("LONG FLIGHT after 220 ticks: "+longFlight);
        },220L);
        Bukkit.getScheduler().runTaskLater(this,() -> {
            boolean pass=explosions==8&&primes==0&&longFlight&&ammoRoundTrip&&heat&&afterfire&&BlastAfterfire.activeCount()==0&&RocketProjectiles.activeCount()==0&&RocketProjectiles.ticketCount()==0;
            getLogger().info((pass?"SMOKE PASS":"SMOKE FAIL")+": explosions="+explosions+", fireballPrimes="+primes+", longFlight="+longFlight+", ammoRoundTrip="+ammoRoundTrip+", heat="+heat+", afterfire="+afterfire+", remainingFires="+BlastAfterfire.activeCount()+", tickets="+RocketProjectiles.ticketCount());
            Bukkit.shutdown();
        },420L);
    }
    @EventHandler(priority=EventPriority.MONITOR) public void onPrime(ExplosionPrimeEvent event){primes++;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onBlockBlast(BlockExplodeEvent event){explosions++;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onEntityBlast(EntityExplodeEvent event){explosions++;}
}
