package fredboat.command.music

import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.PlayerRegistry
import fredboat.command.music.control.PlayCommandTest
import fredboat.test.IntegrationTest
import fredboat.test.sentinel.SentinelState.joinChannel
import fredboat.test.sentinel.delayUntil
import fredboat.test.sentinel.delayedAssertEquals
import fredboat.test.util.cachedGuild
import fredboat.test.util.queue
import junit.framework.Assert.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeekingTests : IntegrationTest() {

    private lateinit var player: GuildPlayer
    private var position: Int // Int because of assertions errors
        get() = player.position.toInt()
        set(pos) {
            player.seekTo(pos.toLong())
            delayUntil { position == pos }
        }

    @BeforeEach
    fun beforeEach(players: PlayerRegistry) {
        joinChannel()
        players.destroyPlayer(cachedGuild)
        player = players.getOrCreate(cachedGuild)
        cachedGuild.queue(PlayCommandTest.url)
        delayUntil { player.playingTrack != null }
        assertNotNull(player.playingTrack)
        player.setPause(true)
        position = 0
    }

    @Test
    fun forward() {
        testCommand(";;forward 5") { delayedAssertEquals(expected = 5000) { position } }
        testCommand(";;forward 5:6") { delayedAssertEquals(expected = 5000 + 6000 + 60000 * 5) { position } }
        position = 0
        testCommand(";;forward 1:0:0") { delayedAssertEquals(expected = 60 * 60 * 1000) { position } }
    }

    @Test
    fun restart() {
        position = 1000
        testCommand(";;restart") { delayedAssertEquals(expected = 0) { position } }
    }

    @Test
    fun rewind() {
        position = 60000
        testCommand(";;rewind 5") { delayedAssertEquals(expected = 55000) { position } }
        testCommand(";;rewind 60") { delayedAssertEquals(expected = 0) { position } }
        position = 60*60*1000
        testCommand(";;rewind 5:0") { delayedAssertEquals(expected = 55*60*1000) { position } }
        position = 60*60*1000 + 5000
        testCommand(";;rewind 1:0:4") { delayedAssertEquals(expected = 1000) { position } }
    }

    @Test
    fun seek() {
        val tests = mapOf(
                "5" to 5 * 1000,
                "5:5" to (5*60+5) * 1000,
                "50:50" to (50*60+50) * 1000,
                "1:50:50" to (50*60+50) * 1000 + 60*60*1000
        )

        tests.forEach { arg, exp ->
            testCommand(";;seek $arg") { delayedAssertEquals(expected = exp) { position } }
        }
    }

}