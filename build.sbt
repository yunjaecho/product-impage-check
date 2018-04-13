name := "product-impage-check"

version := "0.1"

scalaVersion := "2.12.5"

unmanagedBase := baseDirectory.value / "libs"

unmanagedJars in Compile := (baseDirectory.value ** "*.jar").classpath

libraryDependencies ++= Seq(
  "org.hibernate" % "hibernate-entitymanager" % "5.2.16.Final",
  "org.scalaj" %% "scalaj-http" % "2.3.0"
)