package man10multiblock.man10multiblock

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class Man10MultiBlock : JavaPlugin(),Listener{
    override fun onEnable() {
        // Plugin startup logic
        server.pluginManager.registerEvents(this,this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    fun setBarrier(size:Int,location: Location):Boolean{

        logger.info("X:${location.blockX}")
        logger.info("Y:${location.blockY}")
        logger.info("Z:${location.blockZ}")


        val cube = getCube(size,location)

        for (loc in cube){
            if (loc.block.type != Material.AIR){
                logger.info("cant filled!!")
                return false
            }
        }

        cube.forEach{ loc -> loc.block.type = Material.BARRIER }

        logger.info("Filled")

        return true
    }

    fun setArmorStand(location: Location,item:ItemStack){

        val loc = location.clone()

        var yaw: Float = loc.yaw
        if (yaw < 0) {
            yaw += 360f
        }
        if (yaw >= 315 || yaw < 45) {
            loc.yaw = 0.0F
        } else if (yaw < 135) {
            loc.yaw = 90F
        } else if (yaw < 225) {
            loc.yaw = 180F
        } else if (yaw < 315) {
            loc.yaw = -90F
        }
        loc.x+=0.5
        loc.z+=0.5

        val armor = location.world.spawn(loc,ArmorStand :: class.java)
        armor.setGravity(false)
        armor.canPickupItems = false
//        armor.isVisible = false
        armor.setItem(EquipmentSlot.HEAD,item)
        logger.info("X:${location.blockX}")
        logger.info("Y:${location.blockY}")
        logger.info("Z:${location.blockZ}")


    }

    fun getCube(size:Int,location:Location):List<Location>{

        val list = mutableListOf<Location>()

        val fx = location.blockX-((size-1)/2)
        val fy = location.blockY
        val fz = location.blockZ-((size-1)/2)

        for (x in fx until (fx+size)){
            for (y in fy until (fy+size)){
                for (z in fz until (fz+size)){
                    list.add(Location(location.world,x.toDouble(),y.toDouble(),z.toDouble()))
                }
            }
        }

        return list

    }

    fun setMultiBlock(size: Int,location: Location,item:ItemStack){
        if(!setBarrier(size,location))return
        setArmorStand(location,item)
    }

    @EventHandler
    fun setBlock(e:BlockPlaceEvent){

        e.isCancelled = true

        setBarrier(5,e.block.location)

    }

    @EventHandler
    fun setItem(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        if (!e.hasItem())return

        val loc = e.clickedBlock!!.location
        loc.y += 1.0
        loc.yaw = e.player.location.yaw

        setMultiBlock(3,loc,e.player.inventory.itemInMainHand)

    }

}