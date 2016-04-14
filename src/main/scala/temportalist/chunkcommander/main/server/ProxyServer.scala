package temportalist.chunkcommander.main.server

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import temportalist.chunkcommander.main.common.ProxyCommon

/**
  *
  * Created by TheTemportalist on 1/14/2016.
  *
  * @author TheTemportalist
  */
class ProxyServer extends ProxyCommon {

	override def getServerElement(ID: Int, player: EntityPlayer,
			world: World, x: Int, y: Int, z: Int,
			tileEntity: TileEntity): AnyRef = {
		null
	}

}
