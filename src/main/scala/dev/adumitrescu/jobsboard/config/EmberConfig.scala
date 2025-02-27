package dev.adumitrescu.jobsboard.config

import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.derivation.default.*

// generates given configReader
final case class EmberConfig(host: Host, port: Port) derives ConfigReader

object EmberConfig {
//   need given ConfigReader[Host] + given ConfigReader[Port] => compiler generate ConfigReader[EmberConfig]
  implicit val hostReader: ConfigReader[Host] = ConfigReader[String].emap(hostString =>
    Host
      .fromString(hostString)
      .toRight(CannotConvert(hostString, Host.getClass.toString, s"Invalid host name $hostString"))
  )
  implicit val portReader: ConfigReader[Port] = ConfigReader[Int].emap(portInt =>
    Port
      .fromInt(portInt)
      .toRight(
        CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port number $portInt")
      )
  )
}
