# jdk自带的命令

## java

```text
java命令的使用详细说明参考 https://github.com/openjdk/jdk/blob/e65fd45d/src/java.base/share/man/java.md

To launch a class file:  java [options] mainclass [args ...]
To launch the main class in a JAR file: java [options] -jar jarfile [args ...]
To launch the main class in a module: java [options] -m module[/mainclass] [args ...]    or  java [options] --module module[/mainclass] [args ...]
To launch a source-file program: java [options] source-file [args ...]


Overview of Java Options

Standard Options for Java
    Options guaranteed to be supported by all implementations of the Java Virtual Machine (JVM)
Extra Options for Java
    General purpose options that are specific to the Java HotSpot Virtual Machine. They aren't guaranteed to be supported by all JVM implementations, and are subject to change. These options start with -X.
The advanced options
    These are developer options used for tuning specific areas of the Java HotSpot Virtual Machine operation. Advanced options start with -XX
    
Standard Options for Java
    --class-path classpath, -classpath classpath, or -cp classpath
            Specifies a list of directories, JAR files, and ZIP archives to search for class files.
    --module-path modulepath... or -p modulepath
            Specifies where to find application modules with a list of path elements. The elements of a module path can be a file path to a module or a directory containing modules.
    --add-modules module[,module...]
            Specifies the root modules to resolve in addition to the initial module.
    -Dproperty=value
            Sets a system property value. 
    -agentlib:libname[=options]    
            Loads the native agent library specified by the absolute path name. 
    -javaagent:jarpath[=options]
            Loads the specified Java programming language agent. 
    -verbose:class
            Displays information about each loaded class.
    -verbose:gc
            Displays information about each garbage collection (GC) event.
    -verbose:jni
            Displays information about the use of native methods and other Java Native Interface (JNI) activity.
    -verbose:module
            Displays information about the modules in use.
    -X 
            Prints the help on extra options to the error stream.        
    @argfile
            The @argfile option overcomes command-line length limitations by enabling the launcher to expand the contents of argument files after shell expansion, but before argument processing.
            

Extra Options for Java
    -Xbootclasspath/a:directories|zip|JAR-files
            Specifies a list of directories, JAR files, and ZIP archives to append to the end of the default bootstrap class path.
    -Xmn size 
            Sets the initial and maximum size (in bytes) of the heap for the young generation (nursery) in the generational collectors.     (-Xmn中的n指的就是nursery 幼儿园)
    -Xms size
            Sets the minimum and the initial size (in bytes) of the heap.     (-Xms中的s指的就是size)              
    -Xmx size
            Specifies the maximum size (in bytes) of the heap.          (-Xmx中的x指的就是max)     
    -Xss size
            Sets the thread stack size (in bytes)
    -Xnoclassgc
            Disables garbage collection (GC) of classes. 
    -Xlog:option
            Configure or enable logging with the Java Virtual Machine (JVM) unified logging framework. (jvm层面的日志)
            -Xlog:gc
                    Logs messages tagged with the gc tag using info level to stdout.
            


Advanced Runtime Options for Java
    -XX:+HeapDumpOnOutOfMemoryError
            Enables the dumping of the Java heap to a file in the current directory by using the heap profiler (HPROF) when a java.lang.OutOfMemoryError exception is thrown by the JVM.
    -XX:HeapDumpPath=path
            Sets the path and file name for writing the heap dump provided by the heap profiler (HPROF) when the -XX:+HeapDumpOnOutOfMemoryError option is set.

    -XX:ErrorFile=filename
            Specifies the path and file name to which error data is written when an irrecoverable error occurs. 
            By default, this file is created in the current working directory and named hs_err_pid.log where pid is the identifier of the process that encountered the error.
    -XX:OnError=string
            Sets a custom command or a series of semicolon-separated commands to run when an irrecoverable error occurs.
    -XX:+UsePerfData
            Enables the perfdata feature.
    -XX:+PerfDataSaveToFile
            If enabled, saves jstat binary data when the Java application exits.  Use the jstat command to display the performance data contained in this file
    
    -Xloggc:filename 
            this command is replaced by -Xlog:gc:filename
    
    -XX:OnOutOfMemoryError=string : Sets a custom command or a series of semicolon-separated commands to run when an OutOfMemoryError exception is first thrown by the JVM. 
    -XX:+PrintCommandLineFlags : Enables printing of ergonomically selected JVM flags that appeared on the command line. 
    -XX:ActiveProcessorCount=x
            Overrides the number of CPUs that the VM will use to calculate the size of thread pools it will use for various operations such as Garbage Collection and ForkJoinPool.    
    -XX:AllocateHeapAt=path
            Takes a path to the file system and uses memory mapping to allocate the object heap on the memory device. 
            Using this option enables the HotSpot VM to allocate the Java object heap on an alternative memory device, such as an NV-DIMM, specified by the user.        
            Some operating systems expose non-DRAM memory through the file system.Memory-mapped files in these file systems bypass the page cache and provide a direct mapping of virtual memory to the physical memory on the device.
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
