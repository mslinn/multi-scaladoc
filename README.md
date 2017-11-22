# Scaladoc for SBT MultiProjects

[![Build Status](https://travis-ci.org/mslinn/scaladoc.svg?branch=master)](https://travis-ci.org/mslinn/scaladoc)
[![GitHub version](https://badge.fury.io/gh/mslinn%2Fscaladoc.svg)](https://badge.fury.io/gh/mslinn%2Fscaladoc)
[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/dwyl/esta/issues)

This program creates Scaladoc for SBT multi-projects hosted on GitHub.
You must have write access to the GitHub project being documented.
Following is a high-level description of what this program does for the GitHub project that you specify.
The system's temporary directory is used for all steps.
1. If the `gh-pages` branch exists, any existing Scaladoc for SBT subprojects is deleted, otherwise the `gh-pages` branch is created.
2. The `master` branch is checked out.
3. Scaladoc is built from the `master` branch source, 
   and the Scaladoc output for each SBT subproject is created under a common root in the `gh-pages` branch.
4. An index page is created in the Scaladoc root directory, unless one had previously been built.
5. All of the Scaladoc is committed into the `gh-pages` git branch.
6. The temporary directory is deleted.

The following directory structure is temporarily created while the program runs. 
`XXXX` is a random string, unique each time the program runs.
```
/tmp/
  scaladocXXXX/          # Unique name for each program run
    ghPages/             # Git project checked out as gh-pages branch
      index.html         # created if it does not already exist
      api/               #
        latest/          # contents are wiped prior to generating Scaladoc
           subProject1/  # Scaladoc for SBT subProject1 is generated here
           subProject2/  # Scaladoc for SBT subProject2 is generated here
           subProject3/  # Scaladoc for SBT subProject3 is generated here
    master/              # Git project checked out as master branch (not modified, just read)
      subProject1/       # SBT subProject1 source
      subProject2/       # SBT subProject1 source
      subProject3/       # SBT subProject1 source
```

## Before Running this Program
1. Update the version string in the target project's `build.sbt` and in this `README.md` before attempting to running this program.
2. Commit changes with a descriptive comment:
   ```
   $ git add -a && git commit -m "Comment here"
   ```
3. Publish a new version of the program being documented, if a published version has not already been created.
   ```
   git push origin master
   ```

### Updating Scaladoc
The documentation for this project is generated separately for each subproject.
For usage, simply type:
```
$ bin/run
Scaladoc publisher for multi-project SBT builds 0.1.0
Usage: bin/run [options]

  -c, --copyright <value>  Scaladoc footer
  -k, --keepAfterUse <value>
                           Keep the GhPages temporary directory when the program ends
  -n, --gitHubName <value>
                           Github project ID for project to be documented
  -o, --overWriteIndex <value>
                           Do not preserve any pre-existing index.html in the Scaladoc root
  -r, --dryRun <value>     Show the commands that would be run
  -s, --subProjectNames <value>
                           Comma-delimited names of subprojects to generate Scaladoc for
  -u, --gitRemoteOriginUrl <value>
                           Github project url for project to be documented
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
