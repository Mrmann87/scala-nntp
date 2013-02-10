name := "nntp"

version := "0.1"

scalaVersion := "2.10"

libraryDependencies ++= Seq(
    // Compile dependencies
    "com.twitter" % "util_2.10" & "6.1.0",

    // Test dependencies
    "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
)