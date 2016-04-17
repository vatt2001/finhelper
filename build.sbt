organization  := "ru.art0"

version       := "1.0.3"

scalaVersion  := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "joda-time"           % "joda-time"       % "2.9.3",
    "com.typesafe"        % "config"          % "1.3.0",
    "com.typesafe.play"   %% "play-json"      % "2.5.1"
  )
}

Revolver.settings
