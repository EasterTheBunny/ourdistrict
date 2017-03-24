package code.lib

import java.io.File
import java.text.MessageFormat

import net.jxta.platform.NetworkManager
import net.liftweb.actor.LiftActor

/**
  * Created by athughlett on 3/16/17.
  */
object JXTATest extends LiftActor {
  case class StartClient()
  case class Stop()

  private var stopped = false

  def messageHandler = {
    case StartClient if !stopped =>
      println("testing")
      try {
        // Set the main thread name for debugging.
        Thread.currentThread().setName(JXTATest.getClass.getSimpleName());

        // Configure this peer as an ad-hoc peer named "HelloWorld" and
        // store configuration info ".cache/HelloWorld" directory.
        println("Configuring JXTA")
        val manager: NetworkManager =
          new NetworkManager(NetworkManager.ConfigMode.ADHOC,
            "HelloWorld",
            new File(new File(".cache"), "HelloWorld").toURI())

        // Start the JXTA
        println("Starting JXTA")
        manager.startNetwork()
        println("JXTA Started")

        // Wait up to 20 seconds for a connection to the JXTA Network.
        println("Waiting for a rendezvous connection")
        val connected: Boolean = manager.waitForRendezvousConnection(20 * 1000)
        println(s"Connected :$connected")

        // Stop JXTA
        println("Stopping JXTA")
        manager.stopNetwork()
        println("JXTA stopped")
      } catch {
        case e: Throwable =>
          println("Fatal error -- Quitting")
          e.printStackTrace(System.err)
      }
    case Stop => stopped = true
  }

}
