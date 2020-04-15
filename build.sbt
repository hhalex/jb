
scalaVersion := "2.13.1"

libraryDependencies += "co.fs2" %% "fs2-core" % "2.3.0"
libraryDependencies += "co.fs2" %% "fs2-io" % "2.3.0"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.1.1"
libraryDependencies += "org.typelevel" %% "cats-kernel" % "2.1.1"
libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.1"

libraryDependencies += "org.http4s" %% "http4s-core" % "0.21.3"
libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.21.3"
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.21.3"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.21.3"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.21.3" % Test

libraryDependencies += "org.http4s" %% "http4s-circe" % "0.21.3"

libraryDependencies += "io.circe" %% "circe-generic" % "0.13.0"
