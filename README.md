# JDFC

## Rocket Start
- Changed JDFC?
	- disable debug
	- mvn clean install
	- Copy jar to defects4j
- Changed defects4j perl files?
	- class path set correctly in defects4j.build.xml
	- cpanm --installdeps .
	- ./init.sh
- Delete lang_1b
- Checkout lang_1b
- Test
	- Run single test in d4j-repo
	- Run experiment script in jdfc/experiment
- Note down time

## How to run a successful analysis?
- defects4j installed
- class path set correctly in defects4j.build.xml
- most recent jar file in defects4j/framework/projects/lib
- Remember: The serialization files are merged. If you want a clean one delete it before you run an analysis

## DEBUG
- Instrumentation works
- Shutdown hook is called
- Is trackVar called?
- Is path to instrumented classes correct?

### DEBUG: defects4j.build.xml
```xml
<fail message=""/>
```

### DEBUG: JDFC while running defects4j cmd
```
try {
        throw new IllegalAccessException("not important");
    } catch (IllegalAccessException e) {
        String debug = "";
        throw new RuntimeException(debug);
    }
```

Check failing_tests in project under test.

## Requirements

### Experiment
#### General
```
- Defects4j
- Bash 4 or higher
- Python 3
```

#### Bash
```
- xmlstarlet
- jq
```

#### Python
```
- pip3
- regex
```

## Development
Use jdfc cli to test on playground.
Whole project and single class is possible.
Single method is only possible by invoking defects4j on a defects4j repo.

## Run Branch
```
cd <path-to-jdfc>/experiment
/opt/homebrew/bin/bash run_branch.sh <temp-defects4j-repo-folder>
```

### Test
___
```shell
defects4j <coverage | jdfc> \ 
    -w /Users/matthiasbloch/repos/hub/defects4j-repos/Lang_1b \
    -t org.apache.commons.lang3.BooleanUtilsTest::testConsctructor
```
- line: 0%
- intra-proc dataflow: 0%
___
```shell
defects4j <coverage | jdfc> \ 
    -w /Users/matthiasbloch/repos/hub/defects4j-repos/Lang_1b \
    -t org.apache.commons.lang3.BooleanUtilsTest::test_toIntegerObject_boolean
```
- line: 4.8%
- intra-proc dataflow: 
___
```shell
defects4j <coverage | jdfc> \ 
    -w /Users/matthiasbloch/repos/hub/defects4j-repos/Lang_1b \
    -t org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
```
- line: 31.8%
- intra-proc dataflow:
___
```shell
defects4j <coverage | jdfc> \ 
    -w /Users/matthiasbloch/repos/hub/defects4j-repos/Lang_1b \
    -t org.apache.commons.lang3.reflect.MethodUtilsTest::testInvokeStaticMethod
```
```shell
defects4j <coverage | jdfc> \ 
    -w /Users/matthiasbloch/repos/hub/defects4j-repos/Lang_1b \
    -t org.apache.commons.lang3.reflect.MethodUtilsTest::testInvokeExactStaticMethod
```
___

## Run Dataflow
```
cd <path-to-jdfc>/experiment
```

#### Default Terminal ZSH
```
time <path-to-bash> run_dataflow.sh <temp-defects4j-working-dir>
```
Example:
```
time /opt/homebrew/bin/bash run_dataflow.sh /tmp/defects4j-repos
```

### Test
Testing on jdfc-playground is not possible

## Structure
### Defects4j
```
- defects4j
- d4j-jdfc.pl
    - Project::compile_tests
    - Coverage::jdfc
        - Project::coverage_instrument
        - Project::run_tests
        - Project::coverage_report
```


## CLI
Following options are available to invoke the cli
```shell
option      required            description               arg     
-W          yes                 working directory         absolute path to working directory
-B          yes                 build directory           relative path from working directory to build directory
-C          yes                 classes directory         relative path from working directory to classes directory
-S          yes                 java source directory     relative path from working directory to java source root
-i          no                  run instrumentation       optional: fully qualified class name to instrument single class
-t          no                  run tests                 optional: name of test method to run single test (e.g. org.example.Class::test) (default: all test methods of all instrumented classes)
-r          no                  create report             optional: absolute path to report output directory (default: <workDir>/jdfc-report)
```
