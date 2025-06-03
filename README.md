# Replication package for patchNote

## Content

/code: The implementation of patchNote

/data: The human-crafted bug-fixing patch explanations

## Steps to patchNote

### Retrieval Module

#### Requirements

- Java 17
- Gson 2.88
- Eclipse. JDT Core 3.10

#### Steps to retrievalModule

**1. Retrieve the bug and fix version source code**

`defects4j checkout -p [proejctName] -v 1b -w /tmp/[projectName]_[bugId]_buggy`

`defects4j checkout -p [projectName] -v 1f -w /tmp/[projectName]_[bugId]_fixed`

**2. Retrieve the project-internal paths to the main and test source code directories for each bug instance**

`cd ./retrievalModule/EntitySemanticsRetrieval/pythonScripts`

`python pre_path.py`

`python subproject.py`

**3. Extract the trigger tests associated with each bug**

`python trigger_tests.py`

**4. Context retrieval**

`cd ./src/main/java/retrieval/core/main/`

`run Main.java`



### generation Module

#### Requirements

- Python 3.8
- OpenAI

#### Steps to generation module

**1. Retrieve example**

`cd generationModule/`

`python examples.py`

**2. Generate bug-fixing patch explanation**

`python llm_generation.py`


