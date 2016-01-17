package temportalist.chunkcommander.common

import temportalist.origin.foundation.common.register.OptionRegister

/**
  * Created by TheTemportalist on 1/16/2016.
  */
object ModOptions extends OptionRegister {

	var TEMP_MAX_TIME_AWAY_MINUTES = 60
	var FORCED_MAX_TIME_AWAY_HOURS = 30

	override def getExtension: String = "json"

	override def register(): Unit = {
		this.TEMP_MAX_TIME_AWAY_MINUTES = this.getAndComment("general", "Max away time",
			"Max time players are allowed to be offline for personal chunks, in minutes.",
			this.TEMP_MAX_TIME_AWAY_MINUTES)
		this.FORCED_MAX_TIME_AWAY_HOURS = this.getAndComment("general", "Max time absent",
			"Max time player's can be offline before their chunks are no longer chunk loaded, in hours.",
			this.FORCED_MAX_TIME_AWAY_HOURS)

	}

}
