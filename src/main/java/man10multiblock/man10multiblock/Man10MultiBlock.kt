package man10multiblock.man10multiblock

import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import red.man10.realestate.RealEstateAPI
import red.man10.realestate.region.User

class Man10MultiBlock : JavaPlugin(),Listener{

    var enableWorld = mutableListOf<String>()

    lateinit var realEstateAPI:RealEstateAPI
    lateinit var machine: Machine

    companion object{

        lateinit var plugin : Man10MultiBlock
        lateinit var recipe : Recipe

        fun setData(item: ItemStack,key:String,value:String): ItemStack {
            val meta = item.itemMeta

            meta.persistentDataContainer.set(NamespacedKey(plugin,key), PersistentDataType.STRING,value)
            item.itemMeta = meta

            return item
        }

        fun getData(item: ItemStack,key: String):String?{
            if (!item.hasItemMeta())return null
            return item.itemMeta.persistentDataContainer[NamespacedKey(plugin,key), PersistentDataType.STRING]
        }

        fun removeData(item: ItemStack,key: String){
            val meta = item.itemMeta
            meta.persistentDataContainer.remove(NamespacedKey(plugin,key))
            item.itemMeta = meta
        }

    }

    override fun onEnable() {

        saveDefaultConfig()

        plugin = this
        enableWorld = config.getStringList("world")
        realEstateAPI = RealEstateAPI()
        machine = Machine()
        recipe = Recipe()

        recipe.load()

        server.pluginManager.registerEvents(this,this)
        server.pluginManager.registerEvents(machine,this)
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

    fun setMultiBlock(size: Int,location: Location,item:ItemStack):Boolean{

        item.amount = 1

        if(!setBarrier(size,location))return false

        setArmorStand(location,item)
        return true
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

    fun isMachine(item:ItemStack):Boolean{
        if (getData(item,"machine") != null)return true

        return false
    }

    fun setMachine(item: ItemStack,name:String):ItemStack{

        setData(item,"machine",name)
        return item
    }

    fun getMachine(loc:Location):ItemStack?{

        val stand = getArmorStand(loc)?:return null

        val head = stand.getItem(EquipmentSlot.HEAD)
        if (!head.hasItemMeta())return null
        if (!isMachine(head))return null
        return head
    }

    fun breakMachine(loc:Location):ItemStack?{

        val stand = getArmorStand(loc)?:return null

        val item = stand.getItem(EquipmentSlot.HEAD).clone()

        if (recipe.isCraft(item)){
            return null
        }

        if (!isMachine(item))return null

        stand.remove()

        val cube = getCube(3, stand.location.block.location)

        cube.forEach{c -> c.block.type = Material.AIR}

        return item
    }

    @EventHandler
    fun setMachineEvent(e:PlayerInteractEvent){

        if (!enableWorld.contains(e.player.location.world.name))return

        if(e.hand != EquipmentSlot.HAND)return

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val item  = e.item?.clone()?:return

        if (!item.hasItemMeta())return

        if (!isMachine(item))return

        val loc = e.clickedBlock!!.location
        loc.y += 1.0
        loc.yaw = e.player.location.yaw

        if (!realEstateAPI.hasPermission(e.player,loc, User.Companion.Permission.BLOCK)){ return }

        e.isCancelled = true

        if (!setMultiBlock(3,loc,item)){ return }

        e.player.world.playSound(loc,Sound.BLOCK_ANVIL_PLACE,1.0F,1.0F)

        e.item!!.amount --

        return
    }

    @EventHandler
    fun clickBarrier(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        if(e.hand != EquipmentSlot.HAND)return

        val block = e.clickedBlock?:return

        if (block.type != Material.BARRIER)return

        val machine = getMachine(block.location) ?: return

        if (!realEstateAPI.hasPermission(e.player,block.location,User.Companion.Permission.INVENTORY)){ return }

        val p = e.player

        this.machine.openMenu(p,machine,block.location)
    }

    @EventHandler(priority = EventPriority.LOW)
    fun breakMachineEvent(e:PlayerInteractEvent){
        if (e.action != Action.LEFT_CLICK_BLOCK)return

        if (e.clickedBlock!!.type != Material.BARRIER)return

        if (!realEstateAPI.hasPermission(e.player,e.clickedBlock!!.location,User.Companion.Permission.BLOCK)){ return }

        val item = breakMachine(e.clickedBlock!!.location) ?:return

        e.player.inventory.addItem(item)
    }


    @EventHandler
    fun inventoryClose(e:InventoryCloseEvent){
        if (e.view.title != "MultiBlock")return

        val material = e.inventory.getItem(0)?:return
        val product = e.inventory.getItem(8)?:return
        val data = e.inventory.getItem(4)?:return

        val machine = getData(data,"machine")?:return
        val time = getData(data,"time")?:return

        recipe.set(machine,material,product,time.toInt())
    }

    @EventHandler
    fun inventoryClickEvent(e:InventoryClickEvent){
        if (e.view.title != "MultiBlock")return

        if (e.clickedInventory == e.whoClicked.inventory)return

        if (e.slot != 0 && e.slot != 8){

            e.isCancelled = true

            if (e.slot == 4){
                e.whoClicked.closeInventory()
                return
            }

            return
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return true

        if (!sender.hasPermission("man10multiblock.op"))return true

        if (args.isEmpty())return true

        if (args[0] == "create"){
            val item = sender.inventory.itemInMainHand

            if (item.type == Material.AIR){
                sender.sendMessage("§c§lアイテムを持ってください！")
                return true
            }

            if (args.isEmpty()){
                sender.sendMessage("§c§lマシンを設定してください！")
                return true
            }

            setMachine(item,args[1])

            sender.sendMessage("§a§l設定完了！")

        }

        if (args[0] == "recipe"){//multiblock recipe machine time

            if (args.size != 3){
                sender.sendMessage("§a/multiblock recipe <machine> <time>")
                return true
            }

            val inv = Bukkit.createInventory(null,9,"MultiBlock")

            val panel1 = ItemStack(Material.RED_STAINED_GLASS_PANE)

            val meta1 = panel1.itemMeta
            meta1.setDisplayName("§3§l←材料")
            panel1.itemMeta = meta1

            val panel2 = ItemStack(Material.RED_STAINED_GLASS_PANE)

            val meta2 = panel2.itemMeta
            meta2.setDisplayName("§3§l完成品→")
            panel2.itemMeta = meta2

            val panel3 = ItemStack(Material.LIME_STAINED_GLASS_PANE)
            setData(panel3,"machine",args[1])
            setData(panel3,"time",args[2])
            val meta3 = panel3.itemMeta
            meta3.setDisplayName("§a§l決定")
            panel3.itemMeta = meta3


            inv.setItem(1,panel1)
            inv.setItem(2,panel1)
            inv.setItem(3,panel1)
            inv.setItem(4,panel3)
            inv.setItem(5,panel2)
            inv.setItem(6,panel2)
            inv.setItem(7,panel2)

            sender.openInventory(inv)

        }


        return true

    }

}