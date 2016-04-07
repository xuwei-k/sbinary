
### Build instructions

SBinary uses sbt to build.

To build and run the example:

```
$ sbt publishLocal 'project treeExample' run
```

This will retrieve dependencies and compile, package, and publish SBinary to your ~/.ivy2/local repository.   If you just want to use the jar directly, it is in the `target/` directory.

You can find the source for the example is in the examples/bt/ directory.
