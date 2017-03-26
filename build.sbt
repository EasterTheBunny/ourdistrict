import org.rbayer.GruntSbtPlugin._
import GruntKeys._

name := "Hacking Politics"

organization := "com.ourdistrict32"

version := "0.0.1"

scalaVersion := "2.11.8"

gruntSettings

unmanagedJars in Compile <++= baseDirectory map { base =>
    val libs = base / "lib"
    (libs ** "*.jar").classpath
}

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
                  "staging"       at "http://oss.sonatype.org/content/repositories/staging",
                  "releases"      at "http://oss.sonatype.org/content/repositories/releases",
                  "RoundEights"   at "http://maven.spikemark.net/roundeights"
                 )

seq(webSettings :_*)

unmanagedResourceDirectories in Test <+= (baseDirectory) { _ / "src/main/webapp" }

watchSources ~= { (ws: Seq[File]) =>
  ws filterNot { path =>
    path.getName.endsWith(".js") || path.getName.endsWith(".css") || path.getName.endsWith(".html") || path.getName == ("resources") || path.getName == ("static")
  }
}

watchSources <++= baseDirectory map { path => ((path / "src/main/webapp/assets") ** "*.js").get }

watchSources <++= baseDirectory map { path => ((path / "src/main/webapp/assets") ** "*.css").get }

watchSources <++= baseDirectory map { path => (path / "Gruntfile.js").get }

//pollInterval := 5000

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

libraryDependencies ++= {
    val liftVersion = "3.0.1"
    val liftEdition = "3.0"

    Seq(
        "net.liftweb"             %% "lift-webkit"            % liftVersion            % "compile",
        "net.liftweb"             %% "lift-mapper"            % liftVersion            % "compile",
        "net.liftmodules"         %% ("fobo"+"_"+liftEdition) % "1.7"                  % "compile",
        "net.liftmodules"         %% ("widgets"+"_"+liftEdition)        % "1.4.1"      % "compile",
        "org.eclipse.jetty"       % "jetty-webapp"            % "8.1.17.v20150415"     % "container,test",
        "org.eclipse.jetty"       % "jetty-plus"              % "8.1.17.v20150415"     % "container,test", // For Jetty Config
        "org.eclipse.jetty.orbit" % "javax.servlet"           % "3.0.0.v201112011016"  % "container,test" artifacts Artifact("javax.servlet", "jar", "jar"),
        "ch.qos.logback"          % "logback-classic"         % "1.1.3",
        "com.h2database"          % "h2"                      % "1.4.193",
        "com.andersen-gott"       %% "scravatar"              % "1.0.3",
        "org.specs2"              %% "specs2"                 % "3.7",
        "org.specs2"              %% "specs2-core"            % "3.8.9",
        "joda-time"               % "joda-time"               % "2.9.7",
        "org.joda"                % "joda-convert"            % "1.8.1",
        "org.postgresql"          % "postgresql"              % "9.3-1100-jdbc4",
        "com.roundeights"         %% "hasher"                 % "1.2.0",
        "org.bouncycastle"        % "bcprov-jdk15on"          % "1.56",
        "io.github.cloudify"      %% "spdf"                   % "1.4.0",
        "com.lambdaworks"         % "scrypt"                  % "1.4.0",
        "org.yaml"                % "snakeyaml"               % "1.18",
        "org.scalacheck"          %% "scalacheck"             % "1.13.4"            % "test",
        "org.scalaj"              %% "scalaj-http"            % "2.3.0"
    )
}

