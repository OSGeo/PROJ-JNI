Contributing to PROJ-JNI
========================

PROJ-JNI is an open source project and we appreciate contributions very much.

# How to contribute to PROJ-JNI

All users, regardless of the profession or skill level, have the ability to
contribute to PROJ-JNI. Here's a few suggestion on how:

* Help users less experienced than yourself.
* Write a bug report
* Request a new feature
* Write documentation
* Fix a bug
* Implement a new feature

In the following sections you can find some guidelines on how to contribute. As
PROJ-JNI is managed on GitHub most contributions require that you have a GitHub
account. Familiarity with [issues](https://guides.github.com/features/issues/)
and the [GitHub Flow](https://guides.github.com/introduction/flow/) is an
advantage.

## Help a fellow PROJ-JNI user

The main forum for support for PROJ-JNI is the GitHub repository. You can see
existing issues [here](https://github.com/Kortforsyningen/PROJ-JNI/issues) and
existing pull requests
[here](https://github.com/Kortforsyningen/PROJ-JNI/pulls).

## Adding bug reports

Bug reports are handled in the [issue
tracker](https://github.com/Kortforsyningen/PROJ-JNI/issues) on PROJ-JNI's home
on GitHub. Writing a good bug report is not easy. But fixing a poorly documented
bug is not easy either, so please put in the effort it takes to create a
thorough bug report.

A good bug report includes at least:

* A title that quickly explains the problem
* A description of the problem and how it can be reproduced
* Version of PROJ and PROJ-JNI being used
* Version numbers of any other relevant software being used, e.g. operating
  system
* A description of what already has been done to solve the problem

The more information that is given up front, the more likely it is that a
developer will find interest in solving the problem. You will probably get
follow-up questions after submitting a bug report. Please answer them in a
timely manner if you have an interest in getting the issue solved.

Finally, please only submit bug reports that are actually related to PROJ-JNI.
If the issue only materializes in software that uses PROJ-JNI it is likely a
problem with that particular software. Make sure that it actually is a PROJ-JNI
problem before you submit an issue.

## Feature requests

Got an idea for a new feature in PROJ-JNI? Submit a thorough description of the
new feature in the [issue
tracker](https://github.com/Kortforsyningen/PROJ-JNI/issues). Please include any
technical documents that can help the developer make the new feature a reality.
An example of this could be a publicly available academic paper that describes a
new projection. Also, including a numerical test case will make it much easier
to verify that an implementation of your requested feature actually works as you
expect.

Note that not all feature requests are accepted.

## Write documentation

PROJ-JNI is in dire need of better documentation. Any contributions of
documentation are greatly appreciated. The PROJ-JNI documentation is available
on
[GitHub](https://kortforsyningen.github.io/PROJ-JNI/org/kortforsyningen/proj/package-summary.html).
The website is generated with
[Javadoc](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).
Contributions to the documentation should be made as [Pull
Requests](https://github.com/Kortforsyningen/PROJ-JNI/pulls) on GitHub.

## Code contributions

Code contributions can be either bug fixes or new features. The process is the
same for both, so they will be discussed together in this section.

### Making Changes

* Create a topic branch from where you want to base your work.
* You usually should base your topic branch off of the master branch.
* To quickly create a topic branch: git checkout -b my-topic-branch
* Make commits of logical units.
* Check for unnecessary whitespace with git diff --check before committing.
* Make sure your commit messages are in the proper format.
* Make sure you have added the necessary tests for your changes.
* Make sure that all tests pass.

### Submitting Changes

* Push your changes to a topic branch in your fork of the repository.
* Submit a pull request to the PROJ-JNI repository.
* If your pull request fixes/references an issue, include that issue number in
  the pull request. For example: 
  
  ```
  Wiz the bang 
  Fixes #123.
  ``````

* PROJ-JNI developers will look at your patch and take an appropriate action.

### Legalese

Committers are the front line gatekeepers to keep the code base clear of
improperly contributed code. It is important to the PROJ-JNI users and
developers to avoid contributing any code to the project without it being
clearly licensed under the project license.

Generally speaking the key issues are that those providing code to be included
in the repository understand that the code will be released under the MIT/X
license, and that the person providing the code has the right to contribute the
code. For the committer themselves understanding about the license is hopefully
clear. For other contributors, the committer should verify the understanding
unless the committer is very comfortable that the contributor understands the
license (for instance frequent contributors).

If the contribution was developed on behalf of an employer (on work time, as
part of a work project, etc) then it is important that an appropriate
representative of the employer understand that the code will be contributed
under the MIT/X license. The arrangement should be cleared with an authorized
supervisor/manager, etc.

The code should be developed by the contributor, or the code should be from a
source which can be rightfully contributed such as from the public domain, or
from an open source project under a compatible license.

All unusual situations need to be discussed and/or documented.

Committer should adhere to the following guidelines, and may be personally
legally liable for improperly contributing code to the source repository:

* Make sure the contributor (and possibly employer) is aware of the contribution
  terms.
* Code coming from a source other than the contributor (such as adapted from
  another project) should be clearly marked as to the original source, copyright
  holders, license terms and so forth. This information can be in the file
  headers, but should also be added to the project licensing file if not exactly
  matching normal project licensing (LICENSE).
* Existing copyright headers and license text should never be stripped from a
  file. If a copyright holder wishes to give up copyright they must do so in
  writing to the foundation before copyright messages are removed. If license
  terms are changed it has to be by agreement (written in email is ok) of the
  copyright holders.
* Code with licenses requiring credit, or disclosure to users should be added to
  LICENSE.
* When substantial contributions are added to a file (such as substantial
  patches) the author/contributor should be added to the list of copyright
  holders for the file.
* If there is uncertainty about whether a change is proper to contribute to the
  code base, please seek more information from the project steering committee.


## Additional Resources

* [General GitHub documentation](https://help.github.com/)
* [GitHub pull request
  documentation](https://help.github.com/articles/about-pull-requests/)

## Acknowledgements

This CONTRIBUTING file is adapted from
[PROJ's](https://github.com/OSGeo/PROJ/blob/master/CONTRIBUTING.md), with
additional language from
[QGIS](https://github.com/qgis/QGIS/blob/master/.github/CONTRIBUTING.md).

