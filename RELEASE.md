MicroDI Release HowTo
=====================

## Operation Sequence

### Perform release

This is performs an automated process of releasing project artifacts, including version
updates, signing and publishing artifacts.

``
$ mvn release:clean release:prepare -P release 

$ mvn release:perform -P release
``

### Set version

This should be used to set new version for all the projects.

``
mvn versions:set -DnewVersion=1.0.3 -P release
``
