# How to run the experiment with Defects4j

## Preparation
### Build JDFC
```shell
mvn clean install
```

### Checkout Project: Defects4j-jdfc
```shell
git checkout git@github.com:blochmat/defects4j-jdfc.git
```

### Checkout Branch: add_jdfc
```shell
cd <path-to-defects4j-jdfc>
git switch add_jdfc
```

### Initialize Defects4j-jdfc
```shell
cpanm --installdeps .
```
```shell
./init.sh
```

### Find run.dev.test target
```
- open defects4j.build.xml
- find "run.dev.test" target
- verify first path element is:
   <pathelement location="${d4j.dir.classes.jdfc-instrumented}" /> 
```

## Run full experiment with
```shell
cd <path-to-jdfc>/experiment
```
#### Intra-procedural Data Flow Analysis
```shell
sh run_dataflow <path-where-repositories-should-be-stored-during-experiment> intra
```
#### Inter-procedural Data Flow Analysis
```shell
sh run_dataflow <path-where-repositories-should-be-stored-during-experiment> inter
```

## Find results
```shell
cd /tmp/pc_analysis/
```