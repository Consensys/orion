
# Working on Java on MacOS


We use java 8, gradle for builds, and intellij as an IDE. Feel free to use something else as you IDE if you are so inclined, but IntelliJ rocks.

Install java 8 on your mac

```
brew tap caskroom/versions
brew cask install java8
```

Install gradle

`
brew install gradle
`

Install IntelliJ community edition

`
brew cask install intellij-idea-ce
`
After doing this you are pretty close to ready.

We'll just need to setup intellij, then checkout athena and get it working.

There is a stackoverflow that answers questions around this, but I'll link you to the order to do things in
First startup intellij, and then setup the default JDK:
https://stackoverflow.com/a/31420120

Then outside of intellij checkout athena as per the project readme using git directly.  Then open this folder in intellij.

You'll be presented with an import project screen, and series of steps to complete as per the linked to doc.  Set the path to Gradle will be something like: /usr/local/Cellar/gradle/4.3.1/ 
(based on your brew config and setup)


Once you've done this you should have an environment that is nicely setup for working with Athena.
