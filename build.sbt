ThisBuild / organization     := "io.github.liewhite"
ThisBuild / organizationName := "liewhite"
ThisBuild / version       := sys.env.get("RELEASE_VERSION").getOrElse("0.4.2")
ThisBuild / scalaVersion  := "3.2.0"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / publishTo              := sonatypePublishToBundle.value
sonatypeCredentialHost             := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

lazy val root = project
  .in(file("."))
  .settings(
    name                                        := "sqlx",
    libraryDependencies += "io.github.liewhite" %% "common" % "0.0.3",
    libraryDependencies += "org.typelevel" %% "shapeless3-deriving"  % "3.0.3",
    libraryDependencies += "org.jetbrains"  % "annotations"          % "23.0.0",
    libraryDependencies += "dev.zio"       %% "zio"                  % "2.0.6",
    libraryDependencies += "dev.zio"       %% "zio-json"             % "0.4.2",
    libraryDependencies += "mysql"          % "mysql-connector-java" % "8.0.28",
    libraryDependencies += "org.postgresql" % "postgresql"           % "42.3.3",
    libraryDependencies += "org.jooq"       % "jooq"                 % "3.17.7",
    libraryDependencies += "org.jooq"       % "jooq-meta"            % "3.17.7",
    libraryDependencies += "com.zaxxer"     % "HikariCP"             % "5.0.1",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )
