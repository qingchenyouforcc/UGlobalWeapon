package org.mmga.uglobal.weapon;

import org.bukkit.*;
import org.bukkit.block.TileState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mmga.uglobal.Weapon;
import org.mmga.uglobal.utils.RpgAnimation;
import java.util.*;

/** Server-authoritative charge, beam, implosion and terrain destruction. */
public final class Annihilation implements Listener,org.bukkit.plugin.messaging.PluginMessageListener {
    private static final Map<UUID,BukkitTask> CHARGES=new HashMap<>();
    private static final Map<UUID,Long> COOLDOWNS=new HashMap<>();
    private static final Set<Entity> DISPLAYS=new HashSet<>();
    private static final Random RANDOM=new Random();
    private static final double AFTERMATH_RADIUS=64;
    private static final double POLLUTION_RADIUS=AFTERMATH_RADIUS/3;
    private static final Map<UUID,Hold> HOLDS=new HashMap<>();
    private static final class Hold {
        final int session;long seen=System.nanoTime();boolean used;
        Hold(int session) {this.session=session;}
    }
    @Override public void onPluginMessageReceived(String channel,Player player,byte[] message) {
        if(!channel.equals("uglobalweapon:charge") || message.length!=5) return;
        var buffer=java.nio.ByteBuffer.wrap(message);byte action=buffer.get();int session=buffer.getInt();
        UUID id=player.getUniqueId();Hold hold=HOLDS.get(id);
        if(action==0) {if(hold!=null && hold.session==session) HOLDS.remove(id);return;}
        if(!isWeapon(player.getInventory().getItemInMainHand())) {HOLDS.remove(id);return;}
        if(action==1) {hold=new Hold(session);HOLDS.put(id,hold);}
        else if(action!=2 || hold==null || hold.session!=session) return;
        hold.seen=System.nanoTime();
        if(!hold.used) charge(player);
    }
    private static boolean held(UUID id,Hold expected) {
        return expected!=null && HOLDS.get(id)==expected && System.nanoTime()-expected.seen<600_000_000L;
    }
    private static NamespacedKey key() { return new NamespacedKey(Weapon.getInstance(),"annihilation"); }
    public static boolean isWeapon(ItemStack item) {
        return item!=null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key(),PersistentDataType.BYTE);
    }
    public static ItemStack create() {
        ItemStack item=new ItemStack(Material.NETHER_STAR);
        var meta=item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_RED+"寂灭");
        meta.setLore(List.of(ChatColor.GRAY+"奇点重炮",ChatColor.DARK_GRAY+"右键蓄力 · 火箭弹 × 1"));
        meta.getPersistentDataContainer().set(key(),PersistentDataType.BYTE,(byte)1);
        meta.getPersistentDataContainer().set(new NamespacedKey(Weapon.getInstance(),"special_item"),PersistentDataType.STRING,"RPG");
        meta.setCustomModelData(3001);meta.setEnchantmentGlintOverride(false);item.setItemMeta(meta);
        return item;
    }
    private static void frame(Player player,int frame) {
        var item=player.getInventory().getItemInMainHand();
        if (!isWeapon(item)) return;
        var meta=item.getItemMeta();meta.setCustomModelData(frame);meta.setEnchantmentGlintOverride(false);
        item.setItemMeta(meta);player.getInventory().setItemInMainHand(item);
    }
    public static void charge(Player player) {
        UUID id=player.getUniqueId();
        Hold hold=HOLDS.get(id);
        if(!held(id,hold) || hold.used) return;
        if(CHARGES.containsKey(id) || COOLDOWNS.getOrDefault(id,0L)>System.currentTimeMillis()) return;
        if(!RpgAnimation.isReady(player)) {
            if(!RpgAnimation.isAnimating(player)) RpgAnimation.raise(player,player.getInventory().getItemInMainHand());
            return;
        }
        if(RocketAmmo.find(player)<0) { player.playSound(player.getLocation(),Sound.BLOCK_DISPENSER_FAIL,.35F,.7F);return; }
        RpgAnimation.cancel(player);
        int slot=player.getInventory().getHeldItemSlot();
        ItemStack identity=player.getInventory().getItemInMainHand().clone();
        newCharge(player,id,slot,identity,hold);
    }
    private static void newCharge(Player player,UUID id,int slot,ItemStack identity,Hold hold) {
        BukkitTask task=new BukkitRunnable() {
            int ticks;boolean unwinding;
            @Override public void run() {
                var held=player.getInventory().getItemInMainHand();
                var normalized=held.clone();var meta=normalized.getItemMeta();
                if(meta!=null) {meta.setCustomModelData(3001); normalized.setItemMeta(meta);}
                var original=identity.clone();var om=original.getItemMeta();om.setCustomModelData(3001);original.setItemMeta(om);
                if(!player.isOnline() || player.isDead() || player.getInventory().getHeldItemSlot()!=slot || !isWeapon(held) || !normalized.equals(original)) {
                    resetSlot(player,slot);CHARGES.remove(id);cancel();return;
                }
                if(!held(id,hold)) unwinding=true;
                if(unwinding) {
                    ticks=Math.max(0,ticks-2);
                    frame(player,ticks==0?3001:3040+Math.min(11,(ticks-1)/4));
                    if(ticks==0) {CHARGES.remove(id);cancel();}
                    return;
                }
                ticks++;
                frame(player,3040+Math.min(11,(ticks-1)/4));
                Location muzzle=WeaponGeometry.muzzle(player);
                if(ticks%4==0) {
                    player.getWorld().playSound(muzzle,Sound.BLOCK_BEACON_AMBIENT,SoundCategory.PLAYERS,.65F,.55F+ticks/40F);
                    double radius=.65*(1-ticks/52.0);
                    ring(muzzle,player.getEyeLocation().getDirection(),radius,Color.fromRGB(230,24,38),.65F,18);
                }
                if(ticks>=48) {
                    hold.used=true;
                    int ammoSlot=RocketAmmo.find(player);
                    CHARGES.remove(id);cancel();
                    if(ammoSlot<0) {frame(player,3001);return;}
                    ItemStack ammo=player.getInventory().getItem(ammoSlot);ammo.setAmount(ammo.getAmount()-1);player.getInventory().setItem(ammoSlot,ammo);
                    COOLDOWNS.put(id,System.currentTimeMillis()+24000);
                    frame(player,3020);fire(player);
                    new BukkitRunnable() { int age;
                        public void run() {
                            if(!player.isOnline() || !isWeapon(player.getInventory().getItemInMainHand()) || player.getInventory().getHeldItemSlot()!=slot) {resetSlot(player,slot);cancel();return;}
                            age++;
                            frame(player,age<4?3020+age:age<52?3060+Math.min(11,(age-4)/4):3001);
                            if(age>=52) cancel();
                        }
                    }.runTaskTimer(Weapon.getInstance(),1,1);
                }
            }
        }.runTaskTimer(Weapon.getInstance(),1,1);
        CHARGES.put(id,task);
    }
    private static void resetSlot(Player player,int slot) {
        ItemStack item=player.getInventory().getItem(slot);
        if(isWeapon(item)) {var meta=item.getItemMeta();meta.setCustomModelData(3001);item.setItemMeta(meta);player.getInventory().setItem(slot,item);}
    }
    private static boolean loaded(Location p) {return p.getWorld().isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4);}
    private static void signal(Location center,String event) {
        for(Player viewer:center.getWorld().getPlayers()) if(viewer.getLocation().distanceSquared(center)<256*256)
            viewer.playSound(center,"uglobalweapon:"+event,SoundCategory.PLAYERS,1F,1F);
    }
    private static void fire(Player player) {
        World world=player.getWorld();Location eye=player.getEyeLocation();Vector direction=eye.getDirection().normalize();
        double range=112;
        for(double d=1;d<=112;d+=1) if(!loaded(eye.clone().add(direction.clone().multiply(d)))) {range=d-1;break;}
        if(range<1) return;
        var hit=world.rayTrace(eye,direction,range,FluidCollisionMode.NEVER,true,.25,e->e instanceof LivingEntity && e!=player && !(e instanceof Player p && p.getGameMode()==GameMode.SPECTATOR));
        Location target=hit==null?eye.clone().add(direction.clone().multiply(range)):hit.getHitPosition().toLocation(world);
        target.add(direction.clone().multiply(-.12));
        Location muzzle=WeaponGeometry.muzzle(player);
        // Use the eye if the first-person muzzle would put a visual beam through a wall.
        Vector offset=muzzle.toVector().subtract(eye.toVector());
        if(offset.lengthSquared()>.001 && world.rayTraceBlocks(eye,offset.clone().normalize(),offset.length())!=null) muzzle=eye.clone();
        Location start=muzzle.clone();
        signal(eye,"annihilation_fire");
        world.playSound(start,Sound.ENTITY_WARDEN_SONIC_BOOM,SoundCategory.PLAYERS,4F,.55F);
        Vector delta=target.toVector().subtract(start.toVector());
        double length=delta.length();
        if(length<.05) {impact(target,player);return;}
        Vector forward=delta.clone().normalize();
        int travel=Math.max(16,Math.min(36,(int)Math.ceil(length/3.5)));
        new BukkitRunnable() {int t; ItemDisplay beam; public void run() {
            t++;
            if(!loaded(start) || !loaded(target) || t>6+travel+32) {remove(beam);cancel();return;}
            if(t<=6) {
                ring(start,direction,t*.36,Color.fromRGB(255,65,85),1.5F,64);
                return;
            }
            double progress=Math.min(1,(t-6.0)/travel);
            double headDistance=length*progress;
            // Restore the visible full beam. Never interpolate from a singular zero-scale matrix.
            if(beam==null) beam=display(start,4002);
            float thickness=t<=6+travel?.85F:(float)Math.max(.08,.85*(1-(t-6-travel)/33.0));
            beam.setInterpolationDelay(0);beam.setInterpolationDuration(1);
            // ItemDisplay rendering adds a Y=180 degree rotation after our transform.
            // Cancel it on the right; the left rotation alone maps model +Z to the shot.
            beam.setTransformation(new Transformation(new Vector3f(),
                    new Quaternionf().rotationTo(new Vector3f(0,0,1),new Vector3f((float)forward.getX(),(float)forward.getY(),(float)forward.getZ())),
                    new Vector3f(thickness,thickness,(float)Math.max(.05,headDistance)),new Quaternionf().rotationY((float)Math.PI)));
            if(t<=6+travel) {
                Location head=start.clone().add(delta.clone().multiply(progress));
                particles(Particle.ELECTRIC_SPARK,head,5,.1,.1,.1,.025);
                ring(head,forward,.3,Color.fromRGB(255,12,35),.85F,16);
            }
            if(t%2==0) for(double d=0;d<headDistance;d+=.65) {
                Location trace=start.clone().add(forward.clone().multiply(d));
                float trailSize=t<=6+travel?1.6F:1.1F;
                world.spawnParticle(Particle.DUST,trace,1,0,0,0,0,new Particle.DustOptions(Color.fromRGB(255,15,35),trailSize),true);
            }
            if(t==6+travel) impact(target,player);
        }}.runTaskTimer(Weapon.getInstance(),0,1);
    }
    private static void particles(Particle type,Location at,int count,double x,double y,double z,double speed) {
        at.getWorld().spawnParticle(type,at,count,x,y,z,speed,null,true);
    }
    private static void impact(Location center,Player owner) {
        if(!loaded(center)) return;
        signal(center,"annihilation_impact");
        snowfall(center);
        AnnihilationDestruction.blast(center,44F,owner,new AnnihilationDestruction.Batch(),true);
        particles(Particle.FLASH,center,1,0,0,0,0);
        particles(Particle.EXPLOSION_EMITTER,center,8,4,4,4,0);
        particles(Particle.SMALL_FLAME,center,110,5,3,5,.18);
        BlastAfterfire.igniteRing(center,18,24,200,owner);
        Bukkit.getScheduler().runTaskLater(Weapon.getInstance(),()->singularity(center,owner),10);
    }
    private static ItemDisplay display(Location at,int model) {
        Location neutral=new Location(at.getWorld(),at.getX(),at.getY(),at.getZ(),0,0);
        ItemDisplay entity=at.getWorld().spawn(neutral,ItemDisplay.class,e->{
            ItemStack item=new ItemStack(Material.NETHER_STAR);var meta=item.getItemMeta();meta.setCustomModelData(model);meta.setEnchantmentGlintOverride(false);item.setItemMeta(meta);
            e.setItemStack(item);e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            e.setPersistent(false);e.setInvulnerable(true);e.setBrightness(new Display.Brightness(15,15));
            e.setViewRange(8F);e.setDisplayWidth(120);e.setDisplayHeight(120);
        });
        DISPLAYS.add(entity);return entity;
    }
    private static void remove(Entity entity) { if(entity!=null) {entity.remove();DISPLAYS.remove(entity);} }
    private static void size(ItemDisplay display,float scale) {
        display.setInterpolationDelay(0);display.setInterpolationDuration(1);
        display.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale),new Quaternionf()));
    }
    private static void ring(Location center,Vector normal,double radius,Color color,float width,int points) {
        Vector axis=normal.clone().crossProduct(Math.abs(normal.getY())>.9?new Vector(1,0,0):new Vector(0,1,0)).normalize();
        Vector second=normal.clone().crossProduct(axis).normalize();
        for(int i=0;i<points;i++) {double a=i*Math.PI*2/points;
            Location p=center.clone().add(axis.clone().multiply(Math.cos(a)*radius)).add(second.clone().multiply(Math.sin(a)*radius));
            center.getWorld().spawnParticle(Particle.DUST,p,1,0,0,0,0,new Particle.DustOptions(color,width),true);
        }
    }
    private static void singularity(Location center,Player owner) {
        if(!loaded(center)) return;
        World world=center.getWorld();ItemDisplay sphere=display(center,4001);size(sphere,.08F);
        Map<BlockDisplay,Vector> debris=new HashMap<>();
        new BukkitRunnable() {
            int age;
            void cleanup() { remove(sphere);debris.keySet().forEach(Annihilation::remove);debris.clear(); }
            public void run() {
                if(!loaded(center)) {cleanup();cancel();return;}
                age++;
                if(age<=164) {
                    double radius=age<80 ? .15+16.35*Math.pow(age/80.0,.75) : age<140 ? 16.5 : Math.max(.08,16.5*Math.pow((164-age)/24.0,2));
                    size(sphere,(float)(radius*2));
                    if(age%12==0) world.playSound(center,Sound.BLOCK_PORTAL_AMBIENT,SoundCategory.PLAYERS,2F,.5F);
                    if(age<152) {
                        for(Entity e:world.getNearbyEntities(center,144,144,144)) {
                            if(!(e instanceof LivingEntity) && !(e instanceof Item)) continue;
                            if(e instanceof Player p && (p.getGameMode()==GameMode.CREATIVE || p.getGameMode()==GameMode.SPECTATOR)) continue;
                            Vector pull=center.toVector().subtract(e.getLocation().toVector());double distance=pull.length();
                            if(distance>144 || distance<.05) continue;
                            double force=.4+3.2*(1-distance/144);
                            Vector velocity=e.getVelocity().multiply(.3).add(pull.normalize().multiply(force));
                            // Lift grounded mobs immediately so collision with the ground cannot pin them down.
                            if(e.isOnGround()) velocity.setY(Math.max(1.05,velocity.getY()));
                            e.setFallDistance(0);e.setVelocity(velocity);
                            if(distance<radius*.75 && age%5==0) {
                                if(e instanceof LivingEntity living) living.damage(12,owner);
                                else e.remove();
                            }
                        }
                        // Bound terrain work and display count while visually drawing blocks inward.
                        int taken=0;
                        for(int n=0;n<1120 && taken<(age<28?256:160) && debris.size()<2080;n++) {
                            // The first impact leaves empty air: sample exposed terrain, not the crater's void.
                            int band=n%20;
                            double angle=RANDOM.nextDouble()*Math.PI*2;
                            // Try nearby terrain first: 80% near, 15% middle, only 5% far away.
                            double radial=band<16?4+44*Math.pow(RANDOM.nextDouble(),.9)
                                    :band<19?48+32*Math.sqrt(RANDOM.nextDouble()):80+34*Math.sqrt(RANDOM.nextDouble());
                            Location sample=center.clone().add(Math.cos(angle)*radial,0,Math.sin(angle)*radial);
                            Location exposed=surface(sample,28,112,true);
                            if(exposed==null || exposed.distanceSquared(center)>144*144) continue;
                            var block=exposed.clone().subtract(0,1,0).getBlock();
                            if(block.getType().isAir() || block.isLiquid() || block.getType().getHardness()<0 || block.getState() instanceof TileState) continue;
                            BlockBreakEvent permission=new BlockBreakEvent(block,owner);Bukkit.getPluginManager().callEvent(permission);
                            if(permission.isCancelled()) continue;
                            var data=block.getBlockData();Location origin=block.getLocation().add(.5,.5,.5);
                            block.setType(Material.AIR,false);
                            BlockDisplay shard=world.spawn(origin,BlockDisplay.class,d->{d.setBlock(data);d.setPersistent(false);d.setInvulnerable(true);d.setViewRange(3);d.setTeleportDuration(2);});
                            DISPLAYS.add(shard);debris.put(shard,new Vector(origin.distance(center),RANDOM.nextDouble()*Math.PI*2,0));taken++;
                        }
                    }
                    var iterator=debris.entrySet().iterator();
                    while(iterator.hasNext()) {
                        if(age%2!=0) break;
                        var entry=iterator.next();BlockDisplay shard=entry.getKey();Vector pull=center.toVector().subtract(shard.getLocation().toVector());
                        double distance=pull.length();
                        if(distance<Math.max(.35,radius*.8)) {remove(shard);iterator.remove();continue;}
                        Vector state=entry.getValue();state.setZ(state.getZ()+2);
                        double travel=.7*2*2.5*(.4+Math.min(age,144)*.008);
                        double speed=travel*.76;
                        Vector orbit=pull.multiply(-1).rotateAroundAxis(new Vector(.12,1,.18).normalize(),Math.min(.28,travel*.65/distance));
                        orbit.multiply(Math.max(0,(distance-speed)/distance));
                        if(state.getZ()<5) orbit.setY(orbit.getY()+.8);
                        shard.teleport(center.clone().add(orbit));
                        float scale=(float)Math.min(1.15,.4+distance/9);
                        float spin=(float)(state.getZ()*.17+state.getY());
                        shard.setTransformation(new Transformation(new Vector3f(-scale/2,-scale/2,-scale/2),new Quaternionf().rotateXYZ(spin,spin*.7F,spin*.4F),new Vector3f(scale),new Quaternionf()));
                    }
                }
                if(age==165) {
                    cleanup();signal(center,"annihilation_collapse");
                    particles(Particle.FLASH,center,3,.5,.5,.5,0);
                    world.createExplosion(center,14F,false,true,owner);
                    world.playSound(center,Sound.BLOCK_BEACON_DEACTIVATE,SoundCategory.PLAYERS,4F,.5F);
                }
                if(age==179) { layeredBlast(center,owner);cancel(); }
            }
        }.runTaskTimer(Weapon.getInstance(),0,1);
    }
    /** One terrain blast per step: concentric shells keep expanding instead of one frozen mega-blast. */
    private static void layeredBlast(Location center,Player owner) {
        if(!loaded(center)) return;
        World world=center.getWorld();signal(center,"annihilation_blast");
        AnnihilationDestruction.Batch fracture=new AnnihilationDestruction.Batch();
        AnnihilationDestruction.blast(center,64F,owner,fracture,true);
        particles(Particle.FLASH,center,12,4,4,4,0);
        world.playSound(center,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,SoundCategory.PLAYERS,16F,.5F);
        new BukkitRunnable() {
            int age;boolean finished;
            final List<Location> innerGround=new ArrayList<>(),outerGround=new ArrayList<>();
            int groundSample;
            public void run() {
                if(!loaded(center) || ++age>520) {cancel();return;}
                if(age<=30) {
                    int index=age-1;
                    // All follow-up blasts share one overlapping circle, connected to the main crater.
                    double radius=40;
                    double angle=(index+.5)*Math.PI*2/30;
                    Location shell=center.clone().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
                    // Ground-following layers continue through the crater and over uneven terrain.
                    Location ground=surface(shell,32,64);
                    if(ground!=null) shell=ground.clone().add(0,1,0);
                    shell.setY(Math.max(shell.getY(),center.getY()-6));
                    if(loaded(shell)) {
                        AnnihilationDestruction.blast(shell,34F,owner,fracture,false);
                        particles(Particle.EXPLOSION_EMITTER,shell,2,2,2,2,0);
                        particles(Particle.SMALL_FLAME,shell,65,4,3,4,.2);
                        particles(Particle.CLOUD,shell,32,4,2,4,.13);
                    }
                }
                if(age<=150 && age%2==0) {
                    rollingSmoke(center,age);
                }
                if(age>=32 && !finished && fracture.done()) {
                    finished=true;signal(center,"annihilation_blast_end");
                    pollution(center,owner);
                    BlastAfterfire.igniteArea(center,POLLUTION_RADIUS,AFTERMATH_RADIUS,200,400,owner);
                    world.playSound(center,Sound.ENTITY_WARDEN_SONIC_BOOM,SoundCategory.PLAYERS,10F,.5F);
                }
                if(finished && groundSample<720) {
                    // Equal-area samples cover the entire disk, not only its circumference.
                    for(int n=0;n<90 && groundSample<720;n++,groundSample++) {
                        double radial=AFTERMATH_RADIUS*Math.sqrt((groundSample+.5)/720.0);
                        double angle=groundSample*2.399963229728653;
                        Location sample=center.clone().add(Math.cos(angle)*radial,0,Math.sin(angle)*radial);
                        Location point=effectSurface(sample);
                        if(point!=null) (radial<POLLUTION_RADIUS?innerGround:outerGround).add(point);
                    }
                }
                if(finished && age%4==0) {
                    for(Location point:outerGround) particles(Particle.SMALL_FLAME,point.clone().add(0,.35,0),3,.65,.4,.65,.014);
                    for(Location point:innerGround) particles(Particle.DRAGON_BREATH,point.clone().add(0,.55,0),7,.7,.5,.7,.018);
                }
            }
        }.runTaskTimer(Weapon.getInstance(),1,1);
    }
    private static Location effectSurface(Location point) {
        if(!loaded(point)) return null;
        World world=point.getWorld();
        for(int y=Math.min(world.getMaxHeight()-2,point.getBlockY()+48);y>=Math.max(world.getMinHeight()+1,point.getBlockY()-144);y--) {
            var at=world.getBlockAt(point.getBlockX(),y,point.getBlockZ());
            var below=world.getBlockAt(point.getBlockX(),y-1,point.getBlockZ());
            if((at.getType().isAir() || at.getType()==Material.FIRE) && (below.getType().isSolid() || below.isLiquid())) return at.getLocation().add(.5,.1,.5);
        }
        return null;
    }
    private static void snowfall(Location center) {
        World world=center.getWorld();
        new BukkitRunnable() {
            int age;
            public void run() {
                age+=4;
                if(age>720 || !loaded(center)) {cancel();return;}
                for(Player viewer:world.getPlayers()) {
                    Location eye=viewer.getEyeLocation();
                    double dx=eye.getX()-center.getX(),dz=eye.getZ()-center.getZ();
                    if(dx*dx+dz*dz>168*168) continue;
                    // Fill the visible air locally, but clip every flake to the affected region.
                    for(int n=0;n<64;n++) {
                        Location flake=eye.clone().add((RANDOM.nextDouble()*2-1)*23,2+RANDOM.nextDouble()*18,(RANDOM.nextDouble()*2-1)*23);
                        double x=flake.getX()-center.getX(),z=flake.getZ()-center.getZ();
                        if(x*x+z*z>144*144 || !loaded(flake) || flake.getY()>=world.getMaxHeight()) continue;
                        if(world.getHighestBlockYAt(flake.getBlockX(),flake.getBlockZ())>=flake.getY()) continue;
                        double wind=.045+Math.sin(age*.035+n)*.025;
                        viewer.spawnParticle(Particle.SNOWFLAKE,flake,0,wind,-.17-RANDOM.nextDouble()*.07,.018,1);
                    }
                }
            }
        }.runTaskTimer(Weapon.getInstance(),0,4);
    }
    private static void rollingSmoke(Location center,int age) {
        // Expanding, drifting lobes with empty gaps, rather than an emitter filling a cylinder.
        double expansion=9+Math.min(AFTERMATH_RADIUS*.6,age*.55);
        double lift=3+age*.16;
        for(int lobe=0;lobe<24;lobe++) {
            double a=lobe*Math.PI/12+.1*Math.sin(age*.045+lobe);
            double irregular=expansion*(.85+.12*Math.sin(lobe*2.3));
            double roll=age*.055+lobe*1.7;
            double curl=3+age*.07;
            double radius=irregular+Math.cos(roll)*curl;
            double height=lift+Math.sin(roll)*curl+Math.sin(lobe*1.8)*5;
            Location plume=center.clone().add(Math.cos(a)*radius+age*.055,Math.max(2,height),Math.sin(a)*radius+age*.025);
            particles(Particle.SMOKE,plume,20,3+age*.018,2.2+age*.012,3+age*.018,.05);
            particles(Particle.CLOUD,plume,8,3.5,2.2,3.5,.045);
            if(age<65) particles(Particle.SMALL_FLAME,plume,7,2.5,1.8,2.5,.045);
        }
    }
    private static Location surface(Location point,int up,int down) {
        return surface(point,up,down,false);
    }
    private static Location surface(Location point,int up,int down,boolean submerged) {
        if(!loaded(point)) return null;
        World world=point.getWorld();
        for(int y=Math.min(world.getMaxHeight()-2,point.getBlockY()+up);y>=Math.max(world.getMinHeight()+1,point.getBlockY()-down);y--) {
            var block=world.getBlockAt(point.getBlockX(),y,point.getBlockZ());
            if((block.getType().isAir() || submerged && block.isLiquid()) && world.getBlockAt(point.getBlockX(),y-1,point.getBlockZ()).getType().isSolid()) return block.getLocation().add(.5,.1,.5);
        }
        return null;
    }
    private static void pollution(Location center,Player owner) {
        for(int i=0;i<7;i++) {
            double angle=i*Math.PI/3;
            Location sample=i==0?center.clone():center.clone().add(Math.cos(angle)*POLLUTION_RADIUS*.55,0,Math.sin(angle)*POLLUTION_RADIUS*.55);
            Location ground=effectSurface(sample);
            if(ground==null) continue;
            AreaEffectCloud cloud=center.getWorld().spawn(ground,AreaEffectCloud.class,e->{
                e.setSource(owner);e.setParticle(Particle.DRAGON_BREATH);e.setRadius((float)(POLLUTION_RADIUS*.45));
                e.setDuration(380);e.setWaitTime(0);e.setReapplicationDelay(30);e.setRadiusOnUse(0);e.setRadiusPerTick(-.012F);
                e.addCustomEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WITHER,60,1),true);
                e.setPersistent(false);
            });
            DISPLAYS.add(cloud);
            Bukkit.getScheduler().runTaskLater(Weapon.getInstance(),()->remove(cloud),381);
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        HOLDS.remove(event.getPlayer().getUniqueId());
        var task=CHARGES.remove(event.getPlayer().getUniqueId());if(task!=null) task.cancel();
        for(int slot=0;slot<event.getPlayer().getInventory().getSize();slot++) resetSlot(event.getPlayer(),slot);
        COOLDOWNS.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void drop(org.bukkit.event.player.PlayerDropItemEvent event) {
        ItemStack item=event.getItemDrop().getItemStack();
        if(isWeapon(item)) {var meta=item.getItemMeta();meta.setCustomModelData(3001);item.setItemMeta(meta);event.getItemDrop().setItemStack(item);}
    }
    public static void shutdown() {
        CHARGES.values().forEach(BukkitTask::cancel);CHARGES.clear();COOLDOWNS.clear();
        HOLDS.clear();AnnihilationDestruction.shutdown();
        new ArrayList<>(DISPLAYS).forEach(Annihilation::remove);
        for(Player player:Bukkit.getOnlinePlayers()) for(int slot=0;slot<player.getInventory().getSize();slot++) resetSlot(player,slot);
    }
}
