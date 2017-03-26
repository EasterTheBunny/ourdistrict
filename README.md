# ourdistrict

Our District is a project that aims to use technology to improve communication from individuals to law makers.
Quite a bit of effort has been applied in transparency and top down communication, but we want to enhance the
flow in the other direction. This project intends to develop tools that can be easily used by the masses to
direct concerns and ideas upward in the political process efficiently and effectively.

[Reddit Discussion](https://www.reddit.com/r/OurDistrict/)

#### Current Implementations
[Awbrey Hughlett - Candidate US House TX - 32](http://www.ourdistrict32.com)

## Goals and Direction
------------------

- [ ] complete front page voting widget; (privides example of [Ranked Pairs](https://en.wikipedia.org/wiki/Ranked_pairs) voting system)
- [ ] complete charts feature by adding recurring data retrieval and more default charts
- [ ] rebuild documents feature to make its function more clear and include a way to import proposed bills for community edit
- [ ] create email input form for front page to allow newsletter signup

## Quick Start
-----------
#### Build Prerequisites:
- git
- java
- sbt
- npm
- grunt-cli

To get started, follow the steps below.

1. Clone the project
	- $ git clone https://github.com/EasterTheBunny/ourdistrict.git
	- $ cd ourdistrict
2. Set connection db identifiers
	- file of interest -> **./src/main/resources/props/default.props**
	- db.driver (the driver for your database)
	- db.url (the jdbc url for your database)
	- db.user (your db user)
	- db.password (your db password)
	- make these fields blank to use Java's H2 database; in this case, the database will be created automatically
3. Start the sbt console
	- $ ./sbt update ~container:start
4. View site in browser
	- http://127.0.0.1:8080/

## Framework Support and Material
-----------------
OurDistrict is built using the [Lift Framework](https://liftweb.net/). If you have no experience with the Lift framework,
[Simply Lift](https://simply.liftweb.net/) is a decent place to start and [Lift Cookbook](http://chimera.labs.oreilly.com/books/1234000000030/index.html)
goes into a good bit more detail. If you still have questions, the [Lift Mailing List](https://groups.google.com/forum/#!forum/liftweb) an excellent
resource.

If you don't want to learn a new framework and still want to get down to the html/css/js, take a look at
/src/main/webapp for templates and other resources. Everything without an underscore prepending the file name
in this directory is accessible by name in the url:
www.xxx.com/documents -> /src/main/webapp/documents.html

While this isn't a strict rule, it's the general theme if you don't want to dig too deep in the framework.

## Scala IDE Support 
-----------------

#### Eclipse 

Sbteclipse provides SBT command to create Eclipse project files

* **Usage** 

To create a eclipse project: 
```
	$ ./sbt
	> eclipse
```
* **In eclipse do:** 
```
	File ==> Import...
	Select General ==> Existing Project into Workspace 
	Use "Browse" to look up the project root ....
```
#### IDEA

sbt-idea provides a `gen-idea` command to SBT to generate IDEA project files

* **Usage**
```
	$ ./sbt
	> gen-idea no-classifiers
```
* **In Intellij / IDEA do:**
```
	File ==> Open...
	Select project root directory
```
For further information, see both the plugin docs on github and stackoverflow responses:
```
	https://github.com/mpeltonen/sbt-idea
	http://stackoverflow.com/questions/4250318/how-to-create-sbt-project-with-intellij-idea
```

## Properties/Logging
-----------------

#### Properties
Setting runtime properties for the program is done by either changing settings here:
```
    $ cd ./src/main/resources/props/default.props
```
Or by creating your own development or production properties that override the defaults. You can do this by
copying the default and naming it with your hostname. Example where my computer's hostname is 'Chilli-Dog.local':
```
    $ cp ./src/main/resources/props/default.props ./src/main/resources/props/Chilli-Dog.local.props
```
There are other layers that override this example, but that is the basic style and the .gitignore already
ignores these extra files ... usually.

#### Logging
Logging is handled by logback so reference that documentation for setup rules. As for the setup file, the location is here:
```
    $ cd ./src/main/resources/logback.xml
```
The same naming that applied to properties also applies to the logback setup file. Copy the file and name it with
your hostname prepending what it currently is.