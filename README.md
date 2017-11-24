# Scaladoc for SBT MultiProjects

[![Build Status](https://travis-ci.org/mslinn/scaladoc.svg?branch=master)](https://travis-ci.org/mslinn/scaladoc)
[![GitHub version](https://badge.fury.io/gh/mslinn%2Fscaladoc.svg)](https://badge.fury.io/gh/mslinn%2Fscaladoc)
[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/dwyl/esta/issues)

This program creates Scaladoc for SBT multi-projects hosted on GitHub.
You must have write access to the GitHub project being documented.

This program creates Scaladoc for the Scala code found in the `master` branch of your project.
The resulting Scaladoc output is saved in the `gh-pages` branch, which is created if it does not already exist.
Any other `git` branches in your project are ignored.

Output will be made available at your GitHub Pages mini-site, in a subdirectory named after the GitHub project being documented.
If you have never set up GitHub Pages before, no worries, this program does it for you automatically.
For example, if your GitHub user id is `mslinn` and your project is called `web3j-scala`, Scaladoc output will be viewable at
[mslinn.github.io/web3j-scala/index.html](http://mslinn.github.io/web3j-scala/index.html).
Note that [GitHub pages do not support SSL certificates for custom subdomains](https://github.com/isaacs/github/issues/156).

## Scaladoc Output Features
This program takes advantage of several important Scaladoc features that are not used by default:

1. Links well-known Scala types to other online Scaladoc.
2. Creates inheritance diagrams.
3. Deprecation warnings are included.
4. The publishing address of the generated Scaladoc is embedded in the generated output.
5. The Lightbend copyright is not generated at the bottom of every Scaladoc HTML page; 
   instead, a custom footer can be specified.
6. The SBT project name and version is used as the title of the generated Scaladoc.
7. Members inherited by implicit conversions are documented.
8. Verbosity is controllable.

## Before Running this Program
The following are a recommended best practices; this program will work fine if you do not follow these directions, 
or a similar process, however your users will hate you for being sloppy.

### Preflight Check: Optimize Your Scaladoc Source
Create and proofread the Scaladoc output for the SBT project you wish to document.
[Here](https://docs.scala-lang.org/overviews/scaladoc/for-library-authors.html) are the official Scaladoc instructions.

1. Work in the `master` branch of the project that you want to generate Scaladoc for:
   ```
   $ git checkout master
   ```
1. Generate Scaladoc for one SBT subproject at a time.
   For example, generate Scaladoc for subproject `a` by typing:
   ```
   $ sbt "; project a; doc"
   ```
   The above will only use the default options for Scaladoc generation, but that is ok for this preflight check.

2. If the Scaladoc program issues errors and/or warnings, edit the source code to correct the issues, 
   and regenerate the Scaladoc output as shown in the previous step.

3. You can also generate Scaladoc for multiple SBT projects with one command.
   For example, given an SBT project with 3 subprojects (`a`, `b` and `c`), 
   you can generate the Scaladoc for all 3 SBT subprojects by typing:
   ```
   $ sbt "; project a; doc; project b; doc; project c; doc"
   ```

4. Once you have reduced or removed the errors and warnings,
   view the Scaladoc located at `{a,b,c}/target/scala-2.12/api/index.html` in a web browser, 
   and continue fussing with the Scaladoc source until you are satisfied with the result.

### Update the Git Version String and Make a New Tag
1. Ensure the version string in the target project's `build.sbt` is current before running this program.
2. **Commit changes to the master branch** with a descriptive comment and make a tag. 
   Here is an example of how to do that from a bash prompt, for version 0.1.0 of your project:
   ```
   $ git add -A && \
   git commit -m "Releasing v0.1.0" && \
   git tag -a "0.1.0" -m "v0.1.0" && \
   git push origin master --tags
   ```
3. If you are documenting a library you should release a version to correspond with the documentation that you are about 
   to publish. Details vary considerably depending on your infrastructure.

## Running this Program
1. Check it out to another directory.
   In this example, this program is cloned into `~/scaladoc`:
   ```
   $ git clone https://github.com/mslinn/scaladoc.git ~/scaladoc
   $ cd ~/scaladoc
   ```
2. Edit the settings in the `bin/run` script:
   ```
   # Edit these values to suit:
   export SCALADOC_COPYRIGHT="Copyright 2017 Micronautics Research Corporation."
   #export SCALADOC_DRYRUN="true"                 # comment this out for production
   export SCALADOC_GITHUB_NAME="mslinn"           # replace this with your github user id
   export SCALADOC_KEEP="true"                    # comment this out for production
   #export SCALADOC_PRESERVE_INDEX="true"         
   export SCALADOC_GIT_URL="git@github.com:mslinn/web3j-scala.git"  # Edit this to point to the SBT project on GitHub that you want to document
   export SCALADOC_SUB_PROJECT_NAMES="root,demo"  # Edit this to include the SBT project names that you want to generate Scaladoc for
   # No need to modify anything below this line
   ```
3. Run this program:
   ```
   $ bin/run
   ```
4. View the results in your GitHub pages.
   For example, the URL for GitHub user `mslinn` and GitHub project `web3j-scala` is 
   [http://mslinn.github.io/web3j-scala](http://mslinn.github.io/web3j-scala).

You are done!

### Getting Help

For help information, simply type `bin/run -h`:
```
$ bin/run -h
This script generates Scaladoc for this project's subprojects.
SBT and git must be installed for this script to work, and the generated Scaladoc will look better if graphviz is installed.
To see the help information for this programName, type:
    bin/run -h

To run this script in debug mode so a debugger can remotely attach to it, use the -d option:
    bin/run -d

This script builds a fat jar, which takes longer the first time it runs, but speeds up subsequent invocations.
The -j option forces a rebuild of the jar:
    bin/run -j

Scaladoc publisher for multi-project SBT builds 0.1.0
Usage: bin/run [options]

  -c <value> | --copyright <value>
        Scaladoc footer. This command-line parameter overrides the SCALADOC_COPYRIGHT
        environment variable.

  -h | --help
        Display this help message

  -k | --keepAfterUse
        Keep the scaladocXXXX temporary directory when the program ends. This
        command-line parameter overrides the SCALADOC_KEEP environment variable.

  -n <value> | --gitHubName <value>
        Github project ID for project to be documented. This command-line parameter
        overrides the SCALADOC_GITHUB_NAME environment variable.

  -p | --preserveIndex
        Preserve any pre-existing index.html in the Scaladoc root. If this option is
        not specified, the file is regenerated each time this program runs. This
        command-line parameter overrides the SCALADOC_PRESERVE_INDEX environment
        variable.

  -r | --dryRun
        Stubs out 'git commit' and displays the command line that would be run instead,
        along with the output of 'git status'.This command-line parameter
        overrides the SCALADOC_DRYRUN environment variable.

  -s <value> | --subProjectNames <value>
        Comma-delimited names of subprojects to generate Scaladoc for. This
        command-line parameter overrides the SCALADOC_SUB_PROJECT_NAMES
        environment variable.

  -u <value> | --gitRemoteOriginUrl <value>
        Github project url for project to be documented. This command-line parameter
        overrides the SCALADOC_GIT_URL environment variable.
```

## How It Works
The system's temporary directory is used for all steps; for Mac, Linux and Windows Subsystem for Linux this is often `/tmp`.
For [native Windows](https://stackoverflow.com/a/29716813/553865) the temporary directory defaults to `%TMP%`. 
Following is a high-level description of what this program does for the SBT project on GitHub being documented.

1. A temporary directory called `scaladocXXXX` is created.
   `XXXX` is a random string, guaranteed to be unique each time the program runs.
2. The `master` branch is checked out within the temporary directory.
3. If the `gh-pages` branch of the Git project exists, 
   it is cloned into the `ghPages` directory under the temporary directory and any existing Scaladoc for SBT subprojects is deleted, 
   otherwise the `gh-pages` branch is created in the `ghPages` directory under the temporary directory.
4. Scaladoc for each SBT sub-project is built from the `master` branch source, 
   and the Scaladoc output is written into subdirectories named after the SBT subprojects under the `ghPages` directory.
5. An index page is created in the `ghPages` directory, unless one had previously been built and the `--preserveIndex` switch was specified.
6. The `gh-pages` branch in the `ghPages` directory is committed; this includes all of the Scaladoc and `index.html`.
7. The temporary directory is deleted unless the `--keepAfterUse` switch is specified.

The following directory structure is temporarily created while the program runs. 
```
/tmp/
  scaladocXXXX/          # A unique name is generated for this temporary directory each time this program runs
    ghPages/             # Git project; an empty branch is created if non-existant
      .git/              # Checked out as gh-pages branch
      latest/            # created if it does not already exist
        api/             # created if it does not already exist; contents wiped before generating Scaladoc
           subProject1/  # Scaladoc for SBT subProject1 is generated here
           subProject2/  # Scaladoc for SBT subProject2 is generated here
           subProject3/  # Scaladoc for SBT subProject3 is generated here
      index.html         # created if it does not already exist
    master/              # Git project (not modified, just read)
      .git/              # Checked out as master branch 
      subProject1/       # SBT subProject1 source
      subProject2/       # SBT subProject1 source
      subProject3/       # SBT subProject1 source
```

## Sponsor
<img src='https://www.micronauticsresearch.com/images/robotCircle400shadow.png' align='right' width='15%'>

This project is sponsored by [Micronautics Research Corporation](http://www.micronauticsresearch.com/),
the company that delivers online Scala training via [ScalaCourses.com](http://www.ScalaCourses.com).
You can learn Scala by taking the [Introduction to Scala](http://www.ScalaCourses.com/showCourse/40),
and [Intermediate Scala](http://www.ScalaCourses.com/showCourse/45) courses.

Micronautics Research also offers Ethereum and Scala consulting.
Please [contact us](mailto:sales@micronauticsresearch.com) to discuss your organization&rsquo;s needs.

## License
This software is published under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
