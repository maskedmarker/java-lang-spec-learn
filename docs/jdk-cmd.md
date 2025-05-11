# jdk自带的命令

## java

```text

```

## jar


```text
jar - Manipulates Java Archive (JAR) files.

The jar command is a general-purpose archiving and compression tool, based on ZIP and the ZLIB compression format. 
However, the jar command was designed mainly to package Java applets or applications into a single archive. 
The jar command also allows individual entries in a file to be signed by the applet author so that their origin can be authenticated. 
A JAR file can be used as a class path entry, whether or not it is compressed.

The syntax for the jar command resembles(看起来像) the syntax for the tar command. 
It has several operation modes, defined by one of the mandatory operation arguments. 
Other arguments are either options that modify the behavior of the operation, or operands required to perform the operation.
(operation arguments/options/operands)

OPERATION ARGUMENTS
    the operation argument is the first argument specified.

       c      Create a new JAR archive.
       i      Generate index information for a JAR archive.
       t      List the contents of a JAR archive.
       u      Update a JAR archive.
       x      Extract files from a JAR archive.


OPTIONS
    Use the following options to customize how the JAR file is created, updated, extracted, or viewed:
       e      Sets the class specified by the entrypoint operand to be the entry point for a standalone Java application bundled into an executable JAR file. 
       f      Sets the file specified by the jarfile operand to be the name of the JAR file that is created (c), updated (u), extracted (x) from, or viewed (t).
       v      Generates verbose output to standard output.
       -C dir
              When creating (c) or updating (u) a JAR file, this option temporarily changes the directory while processing files specified by the file operands. 
              Its operation is intended to be similar to the -C option of the UNIX tar utility.
              
              For example, the following command changes to the classes directory and adds the Bar.class file from that directory to my.jar:
              jar uf my.jar -C classes Bar.class 
              (如果不添加-C classes, 则会将classes/Bar.class添加到my.jar的根目录;如果添加-C classes, 则会将Bar.class添加到my.jar的根目录)
              The following command adds to my.jar all files within the classes directory (without creating a classes directory in the JAR file), then changes
              back to the original directory before changing to the bin directory to add Xyz.class to my.jar.
              jar uf my.jar -C classes . -C bin Xyz.class
              (将classes内的所有文件添加到my.jar的根目录,将bin目录下的Xyz.class添加到my.jar的根目录)
              
              
OPERANDS
    The following operands are recognized by the jar command.
        file   
            the file operand defines the path and name of the file or directory that should be added to the archive.
            If the entrypoint, jarfile, or manifest operands are used, the file operands must be specified after them.
        entrypoint    
             the entrypoint operand defines the name of the class that should be the entry point.
             The entrypoint operand must be specified if the e option is present.
        jarfile
            The jarfile operand must be specified if the f option is present.
        manifest
            The manifest operand must be specified if the f option is present.
        @arg-file
            To shorten or simplify the jar command, you can specify arguments in a separate text file and pass it to the jar command with the at sign (@) as a prefix.
            When the jar command encounters an argument beginning with the at sign, it expands the contents of that file into the argument list.
            An argument file can include options and arguments of the jar command.
            The arguments within a file can be separated by spaces or newline characters.
            File names within an argument file are relative to the current directory from which you run the jar command, not relative to the location of the argument file.
            Wild cards, such as the asterisk (*), that might otherwise be expanded by the operating system shell, are not expanded.
            
            The following example, shows how to create a classes.list file with names of files from the current directory output by the find command:
            find . -name '*.class' -print > classes.list
            jar cf my.jar @classes.list
            
            An argument file can be specified with a path:
            jar @dir/classes.list
```
