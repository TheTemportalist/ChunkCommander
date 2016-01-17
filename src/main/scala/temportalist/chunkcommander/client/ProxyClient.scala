package temportalist.chunkcommander.client

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import temportalist.chunkcommander.common.ProxyCommon
import temportalist.origin.foundation.client.KeyHandler
import temportalist.origin.foundation.common.register.Registry

/**
  * Created by TheTemportalist on 1/14/2016.
  */
class ProxyClient extends ProxyCommon {

	override def register(): Unit = {
		Registry.registerHandler(WorldRender)
		KeyHandler.register(ModKeys)
	}

	override def getClientElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int,
			z: Int, tileEntity: TileEntity): AnyRef = null

}
