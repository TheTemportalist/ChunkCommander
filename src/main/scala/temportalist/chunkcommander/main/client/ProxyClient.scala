package temportalist.chunkcommander.main.client

import java.util

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.client.IModGuiFactory.{RuntimeOptionCategoryElement, RuntimeOptionGuiHandler}
import temportalist.chunkcommander.main.common.{ChunkCommander, ProxyCommon}

/**
  *
  * Created by TheTemportalist on 1/14/2016.
  *
  * @author TheTemportalist
  */
class ProxyClient extends ProxyCommon with IModGuiFactory {

	override def preInit(): Unit = {
		ChunkCommander.registerHandler(WorldRender)
		ModKeys.register()
		Client.preInit()
	}

	override def register(): Unit = {

	}

	override def postInit(): Unit = {

	}

	override def getClientElement(ID: Int, player: EntityPlayer,
			world: World, x: Int, y: Int, z: Int,
			tileEntity: TileEntity): AnyRef = {
		null
	}

	override def initialize(minecraftInstance: Minecraft): Unit = {}

	override def runtimeGuiCategories(): util.Set[RuntimeOptionCategoryElement] = null

	override def getHandlerFor(
			element: RuntimeOptionCategoryElement): RuntimeOptionGuiHandler = null

	override def mainConfigGuiClass(): Class[_ <: GuiScreen] = classOf[GuiConfig]

}
