package temportalist.chunkcommander.main.common.world


import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.storage.MapStorage
import net.minecraft.world.{World, WorldSavedData}

import scala.collection.mutable
import scala.reflect._

/**
  *
  * Created by TheTemportalist on 1/16/2016.
  *
  * @author TheTemportalist
  */
object WorldDataHandler {

	private val classesAndKeys = mutable.Map[Class[_ <: WorldData], String]()
	private val dimensionGreyList = mutable.Map[Class[_ <: WorldData], (Seq[Int], Seq[Int])]()

	def register(worldDataClass: Class[_ <: WorldData], key: String,
			whiteList: Seq[Int] = Seq[Int](), blackList: Seq[Int] = Seq[Int]()): Unit = {
		this.classesAndKeys(worldDataClass) = key
		this.dimensionGreyList(worldDataClass) = (whiteList, blackList)
	}

	def forWorld[T <: WorldData : ClassTag](world: World)(implicit ctag: ClassTag[T]): T = {
		val dataClass = ctag.runtimeClass.asInstanceOf[Class[T]]
		val key = this.classesAndKeys(dataClass)
		if (key == null) throw new NoSuchElementException(
			"Data class " + dataClass + " was not registered.")

		val greyList = this.dimensionGreyList(dataClass)
		if (greyList == null) throw new NoSuchElementException(
			"Data class " + dataClass.getCanonicalName + " was not registered.")
		val dim = world.provider.getDimension
		if (greyList._1.nonEmpty && !greyList._1.contains(dim))return dataClass.cast(null)
		if (greyList._2.nonEmpty && greyList._2.contains(dim)) return dataClass.cast(null)

		val storage: MapStorage = world.getPerWorldStorage
		var data = storage.loadData(dataClass, key)
		if (data == null)
			try {
				data = dataClass.getConstructor(classOf[String]).newInstance(key)
				dataClass.cast(data).setWorld(world)
				storage.setData(key, data)
			}
			catch {
				case e: Exception =>
					e.printStackTrace()
					throw new IllegalStateException(
						"Could not instantiate " + dataClass.getCanonicalName)
			}
		dataClass.cast(data)
	}

	abstract class WorldData(str: String) extends WorldSavedData(str) {

		private var dimensionID: Int = 0

		def setWorld(world: World): Unit = this.dimensionID = world.provider.getDimension

		def getDimension: Int = this.dimensionID

		override final def writeToNBT(nbt: NBTTagCompound): Unit = {
			nbt.setInteger("dim", this.dimensionID)
			this.write(nbt)
		}

		override final def readFromNBT(nbt: NBTTagCompound): Unit = {
			this.dimensionID = nbt.getInteger("dim")
			this.read(nbt)
		}

		def write(nbt: NBTTagCompound): Unit

		def read(nbt: NBTTagCompound): Unit

	}

}
