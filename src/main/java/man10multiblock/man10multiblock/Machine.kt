package man10multiblock.man10multiblock

import man10multiblock.man10multiblock.Man10MultiBlock.Companion.getData
import man10multiblock.man10multiblock.Man10MultiBlock.Companion.plugin
import man10multiblock.man10multiblock.Man10MultiBlock.Companion.recipe
import man10multiblock.man10multiblock.Man10MultiBlock.Companion.setData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.text.SimpleDateFormat

class Machine : Listener {

    fun openMenu(p:Player,machine:ItemStack,loc:Location){

        if (recipe.isCraft(machine)){
            if (!recipe.isFinish(machine)){
                p.sendMessage("§e§lまだ製品は完成していません！")
                return
            }

            val product = recipe.itemFromBase64(getData(machine,"product")?:return)!!

            p.inventory.addItem(product)
            p.sendMessage("§a§l製品が完成しました！")
            recipe.finishCraft(machine)

            return

        }

        val inv = Bukkit.createInventory(null,18,"§a§lマシン")

        val panel1 = ItemStack(Material.YELLOW_STAINED_GLASS_PANE)

        for (i in 0..8){
            if (i == 4)continue
            inv.setItem(i,panel1)
        }

        val panel2 = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        setData(panel2,"location","${loc.blockX};${loc.blockY};${loc.blockZ}")
        val meta3 = panel2.itemMeta
        meta3.setDisplayName("§a§l作成開始！")
        panel2.itemMeta = meta3

        for (i in 9..17){
            inv.setItem(i,panel2)
        }

        p.openInventory(inv)
    }

    @EventHandler
    fun inventoryEvent(e:InventoryClickEvent){

        if (e.view.title != "§a§lマシン")return

        val p = e.whoClicked
        if (p !is Player)return

        if (e.clickedInventory == p.inventory)return

        if (e.slot == 4)return

        e.isCancelled = true

        if (e.slot >=9){

            val material = e.inventory.getItem(4)?:return

            val data = getData(e.inventory.getItem(9)?:return,"location")!!.split(";")

            val machine = plugin.getMachine(Location(p.world,data[0].toDouble(),data[1].toDouble(),data[2].toDouble()))

            if (machine == null){
                p.sendMessage("§c§l何者かによって、マシンを破壊されてしまったようだ")
                p.closeInventory()
                return
            }

            val time = recipe.startCraft(machine,material)?:return

            p.sendMessage("§a§l完成予想時刻：${SimpleDateFormat("MM/dd kk:mm").format(time.time)}")
            p.closeInventory()
        }

    }

}