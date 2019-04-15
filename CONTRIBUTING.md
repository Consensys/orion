# Contributing to Orion

:+1::tada: First off, thanks for taking the time to contribute! :tada::+1:

Welcome to the Orion repository!  The following is a set of guidelines for contributing to this 
repo and its packages. These are mostly guidelines, not rules. Use your best judgment, 
and feel free to propose changes to this document in a pull request.

### Table of Contents

[Code of Conduct](#code-of-conduct)

[I just have a quick question](#i-just-have-a-quick-question)

[How to Contribute](#how-to-contribute)

* [Reporting Bugs](#reporting-bugs)
* [Suggesting Enhancements](#suggesting-enhancements)
* [Pull Requests](#pull-requests)

[Styleguides](#styleguides)

* [Java Styleguide](#java-styleguide)
* [Coding Conventions](#coding-conventions)
* [Documentation Styleguide](#documentation-style-guide)
* [Git Commit Messages & Pull Request Messages](#git-commit-messages--pull-request-messages)

## Code of Conduct
* This project is governed by the [Orion Code of Conduct](CODE_OF_CONDUCT.md). By participating, 
you are agreeing to uphold this code. Please report unacceptable behavior. Please report unacceptable behavior to [private@pegasys.tech].

## I just have a quick question

> **Note:** Please don't file an issue to ask a question.  You'll get faster results by using the resources below.

* [Orion documentation]
* [Gitter]

## How to Contribute

### Reporting Bugs
#### Before Submitting A Bug 
* Ensure the bug is not already reported by searching on GitHub under 
[Issues](https://github.com/pegasyseng/orion/issues).
#### How Do I Submit a (Good) Bug?
* If you are unable to find an open issue addressing the problem, open a new one. Be sure to include a 
**title and clear description**, as much relevant information as possible, and a **code sample** or 
an **executable test case** demonstrating the unexpected behavior.
* Describe the **exact steps** to **reproduce the problem** in as many details as possible. When 
listing steps, don't just say what you did, but explain how you did it. For example, the exact 
commands used in the terminal to start Orion. 
* Provide **specific examples** to demonstrate the steps. Include links to files or GitHub projects, or 
copy/pasteable snippets, which you use in those examples. If you're providing snippets in the issue, 
use [Markdown code blocks](https://help.github.com/articles/getting-started-with-writing-and-formatting-on-github/).
* Describe the **behavior you observed** after following the steps and explain the 
problem with that behavior.
* Explain the **behavior you expected** instead and why.
* **Can you reliably reproduce the issue?** If not, provide details about how often the problem 
happens and under which conditions it normally happens.

### Suggesting Enhancements
#### Before Submitting An Enhancement Suggestion
* [Search](https://github.com/pegasyseng/orion/issues) to see if the enhancement has already been 
suggested. If it has, add a comment to the existing issue instead of opening a new one.

#### How Do I Submit A (Good) Enhancement Suggestion?
Enhancement suggestions are tracked as GitHub issues. Create an issue on and provide 
the following information:

* Use a **clear and descriptive title** for the issue to identify the suggestion.
* Provide a **step-by-step description** of the suggested enhancement in as much detail as possible.
* Describe the **current behavior** and explain the **behavior you expect** instead and why.
* Explain why this enhancement would be useful to other users.
* Specify the **name and version of the OS** you're using.
* Specify the **name and version of any relevant packages** - eg Geth.

### Pull Requests

Complete the CLA, as described in [CLA.md].

There are a number of automated checks:
* unit tests
* integration tests
* acceptance tests
* code formatting 

If these checks pass, pull requests will be reviewed by the project team against criteria including:
* purpose - is this change useful
* test coverage - are there unit/integration/acceptance tests demonstrating the change is effective
* [style](CODING-CONVENTIONS.md)
* code consistency - naming, comments, design
* changes that are solely formatting are likely to be rejected

Always write a clear log message for your commits. One-line messages are fine for small changes, but 
bigger changes should contain more detail.

## Style Guides

### Java Code Style Guide

We use Google's Java coding conventions for the project. To reformat code, run: 

```
./gradlew spotlessApply
```

Code style will be checked automatically during a build.

### Coding Conventions
We have a set of [coding conventions](CODING-CONVENTIONS.md) to which we try to adhere.
These are not strictly enforced during the build, but should be adhered to and called out in [code reviews](docs/community/code-reviews.md).

### Documentation Style Guide
For documentation, we have [documentation guidelines and examples](DOC-STYLE-GUIDE.md). 
These rules are not automatically enforced but are recommended to make the documentation consistent
 and enhance the user experience.

Also have a look at our [MKDocs Markdown guide](MKDOCS-MARKDOWN-GUIDE.md) if you're not famililar with 
MarkDown syntax. We also have a number of extensions that are available in the Pantheon documentation described
in this guide.

## Git Commit Messages & Pull Request Messages
* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Provide a summary on the first line with more details on additional lines as needed
* Reference issues and pull requests liberally

[private@pegasys.tech]: mailto:private@pegasys.tech
[Gitter]: https://gitter.im/PegaSysEng/orion
[Orion documentation]: https://docs.orion.pegasys.tech/
[CLA.md]: /CLA.md
