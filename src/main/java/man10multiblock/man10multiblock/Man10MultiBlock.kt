package man10multiblock.man10multiblock

import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
//import red.man10.realestate.RealEstateAPI
import red.man10.realestate.region.User
import java.lang.StringBuilder

class Man10MultiBlock : JavaPlugin(),Listener{

    var enableWorld = mutableListOf<String>()

//    lateinit var realEstateAPI:RealEstateAPI

    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()

        enableWorld = config.getStringList("world")
//        realEstateAPI = RealEstateAPI()

        server.pluginManager.registerEvents(this,this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    fun setBarrier(size:Int,location: Location):Boolean{

        val cube = getCube(size,location)

        for (loc in cube){
            if (loc.block.type != Material.AIR){
                return false
            }
        }

        cube.forEach{ loc -> loc.block.type = Material.BARRIER }

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
        armor.isVisible = false
        armor.setItem(EquipmentSlot.HEAD,item)


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

    fun getMultiBlockCommand(loc:Location):String{

        val stand = getArmorStand(loc)?:return "none"

        val head = stand.getItem(EquipmentSlot.HEAD)
        if (!head.hasItemMeta())return "none"
        val meta = head.itemMeta!!

        return meta.persistentDataContainer[NamespacedKey(this,"command"), PersistentDataType.STRING]?:"none"
    }

    fun getArmorStand(loc:Location): ArmorStand? {
        val entities = loc.world.getNearbyEntities(loc,1.5,1.5,1.5)

        for (entity in entities){
            if (entity.type !=EntityType.ARMOR_STAND)continue
            if (entity !is ArmorStand)continue

            return entity
        }

        return null

    }

    fun removeMachine(loc:Location):ItemStack?{

        val stand = getArmorStand(loc)?:return null

        val block = stand.location.block.location

        val cube = getCube(3,block)

        cube.forEach{c -> c.block.type = Material.AIR}

        val item = stand.getItem(EquipmentSlot.HEAD).clone()

        stand.remove()
        return item
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun setBarrier(e:PlayerInteractEvent){

        if (!enableWorld.contains(e.player.location.world.name))return

        if(e.hand != EquipmentSlot.HAND)return

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val item  = e.item?:return

        if (!item.hasItemMeta())return

        item.itemMeta.persistentDataContainer[NamespacedKey(this,"command"), PersistentDataType.STRING]?:return

        val loc = e.clickedBlock!!.location
        loc.y += 1.0
        loc.yaw = e.player.location.yaw

//        if (!realEstateAPI.hasPermission(e.player,loc,User.Companion.Permission.BLOCK)){ return }

        setMultiBlock(3,loc,item)

        e.player.inventory.removeItem(item)

        return
    }

    @EventHandler(priority = EventPriority.LOW)
    fun clickBarrier(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        if(e.hand != EquipmentSlot.HAND)return

        val block = e.clickedBlock?:return

        if (block.type != Material.BARRIER)return

        val cmd = getMultiBlockCommand(block.location)

        if (cmd == "none")return

//        if (!realEstateAPI.hasPermission(e.player,block.location,User.Companion.Permission.INVENTORY)){ return }

        val p = e.player

        if (!p.isOp){
            p.isOp = true
            p.performCommand(cmd)
            p.isOp = false
        }else{
            p.performCommand(cmd)
        }


    }

    @EventHandler(priority = EventPriority.LOW)
    fun breakBlock(e:PlayerInteractEvent){
        if (e.action != Action.LEFT_CLICK_BLOCK)return

        if (e.clickedBlock!!.type != Material.BARRIER)return

//        if (!realEstateAPI.hasPermission(e.player,e.clickedBlock!!.location,User.Companion.Permission.BLOCK)){ return }

        val item = removeMachine(e.clickedBlock!!.location)?:return

        e.player.inventory.addItem(item)
    }


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return true

        if (!sender.hasPermission("man10multiblock.op"))return true

        val item = sender.inventory.itemInMainHand

        if (item.type == Material.AIR){
            sender.sendMessage("§c§lアイテムを持ってください！")
            return true
        }

        if (args.isEmpty()){
            sender.sendMessage("§c§lコマンドを設定してください！")
            return true
        }

        val cmd = StringBuilder()
        for (a in args){
            if (cmd.isEmpty()){
                cmd.append(a)
                continue
            }
            cmd.append(" $a")
        }

        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(this,"command"), PersistentDataType.STRING,cmd.toString())

        item.itemMeta = meta

        sender.sendMessage("§a§l設定完了！")

        return false
    }

}