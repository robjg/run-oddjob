#Oddjob Launch

This is the runnable launch jar for Oddjob. 

The Maven Assembly plugin creates the application
layout in ${project.buildDir}/oddjob. This is then
copied to the final application package by
[oj-assembly](http://rgordon.co.uk/oj-assembly)

The Assembly descriptor defines these implementations used
in the application:
- The SL4J Logging Implementation: Logback.
- The JavaScript Implementation: Nashorn.

Oddjob is a task automation and scheduling solution. More information can be found [here](http://rgordon.co.uk/oddjob).

