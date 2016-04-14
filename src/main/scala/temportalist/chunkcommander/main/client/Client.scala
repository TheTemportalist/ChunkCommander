package temportalist.chunkcommander.main.client

import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import temportalist.chunkcommander.main.common.ChunkCommander
import temportalist.origin.foundation.client.modTraits.IHasKeys
import temportalist.origin.foundation.client.{IKeyBinder, IModClient}
import temportalist.origin.foundation.common.IMod

/**
  *
  * Created by TheTemportalist on 4/13/2016.
  *
  * @author TheTemportalist
  */
object Client extends IModClient with IHasKeys {

	override def getMod: IMod = ChunkCommander

	@SideOnly(Side.CLIENT)
	override def getKeyBinder: IKeyBinder = ModKeys

}
