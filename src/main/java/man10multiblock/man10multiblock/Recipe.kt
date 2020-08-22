package man10multiblock.man10multiblock

import man10multiblock.man10multiblock.Man10MultiBlock.Companion.getData
import man10multiblock.man10multiblock.Man10MultiBlock.Companion.plugin
import man10multiblock.man10multiblock.Man10MultiBlock.Companion.removeData
import man10multiblock.man10multiblock.Man10MultiBlock.Companion.setData
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Recipe {

    val recipes = ConcurrentHashMap<Pair<ItemStack,String>,Pair<ItemStack,Int>>()//material,<product,minute>

    fun startCraft(machine: ItemStack,material:ItemStack): Calendar? {

        val item = material.clone()
        item.amount = 1

        val produceData = recipes[Pair(item, getData(machine,"machine")!!)]?:return null

        val product = produceData.first.clone()

        product.amount = material.amount

        val finish = Calendar.getInstance()
        finish.add(Calendar.MINUTE,(produceData.second*material.amount))

        setData(machine,"time",finish.time.time.toString())
        setData(machine,"product",itemToBase64(product))

        Bukkit.getLogger().info(isFinish(machine).toString())
        Bukkit.getLogger().info(isCraft(machine).toString())

        return finish
    }

    fun isFinish(machine: ItemStack):Boolean{

        val date = getData(machine,"time")?:return false

        if (date.toLong()>Date().time)return false

        return true
    }

    fun isCraft(machine: ItemStack):Boolean{

        if (getData(machine,"product") !=null)return true

        return false

    }

    fun finishCraft(machine: ItemStack){

        removeData(machine,"time")
        removeData(machine,"product")

    }

    fun load(){

        val sql = MySQLManager(plugin,"Man10MultiBlock")

        val rs = sql.query("select * from recipes;")?:return

        while (rs.next()){

            val material = itemFromBase64(rs.getString("material"))!!
            val product = itemFromBase64(rs.getString("product"))!!

            recipes[Pair(material,rs.getString("machine"))] = Pair(product,rs.getInt("time"))
        }

        rs.close()
        sql.close()

    }

    fun set(machine:String,material: ItemStack,product:ItemStack,time:Int){

        recipes[Pair(material,machine)] = Pair(product,time)

        val sql = MySQLManager(plugin,"Man10MultiBlock")

        sql.execute("INSERT INTO recipes (machine, material, product,time) " +
                "VALUES ('$machine', '${itemToBase64(material)}', '${itemToBase64(product)}',$time)")

    }



    fun itemFromBase64(data: String): ItemStack? {
        try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())

            // Read the serialized inventory
            for (i in items.indices) {
                items[i] = dataInput.readObject() as ItemStack
            }

            dataInput.close()
            return items[0]
        } catch (e: Exception) {
            return null
        }

    }

    @Throws(IllegalStateException::class)
    fun itemToBase64(item: ItemStack): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)
            val items = arrayOfNulls<ItemStack>(1)
            items[0] = item
            dataOutput.writeInt(items.size)

            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }

            dataOutput.close()
            val base64: String = Base64Coder.encodeLines(outputStream.toByteArray())

            return base64

        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }
}