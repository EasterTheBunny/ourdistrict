# Contributing to OurDistrict

This project is meant to be open and available to everyone. We would love for you to
contribute and help make it better! To help maintain a clean code base, we would like
you to follow some committing guidelines:

- [Code of Conduct](#coc)
- [Policy on issues and Feature Requests](#issue)
- [Submission Guidelines](#submit)

## <a name="coc"></a> Code of Conduct
The goal of OurDistrict is to provide a way for a larger community to engage in
legislative discussion. The best way to accomplish that goal is to maintain
oneself as civil and productive. The activity surrounding this project should
model our broader goal. People of all levels of ability are welcome here. Help,
don't hate.

## <a name="issue"></a> Policy on issues and Feature Requests:
* Non-committers should discuss issues on another forum before opening an issue.
* All non-committer issues MUST have a link back to the forum discussion that led
	to opening the issue so the whole world can see why the issue came into being and
	said link MUST NOT be behind a login or paywall.
* All non-committer issues should be set to low priority unless a committer
	explicitly asked for assignment (that means they agreed to do the work).
* A committer may open an issue at whatever priority for whatever milestone and
  assign it to themselves.
* There should be very little discussion about issues in the issuing system.
  The discussion should take place on a more open and available platform.

We will accept pull requests into the OURDISTRICT codebase if the pull requests meet
all of the following criteria:
* The request handles an issue that has been discussed on the Lift mailing list
  and whose solution has been requested by the committers (and in general adheres
  to the spirit of the issue guidelines above).

## <a name="submit"></a> Submission Guidelines
All development branches should be checked out from the development branch. Origin
branch naming is as follows:

- **feature/[xyz-branch]**: development of a change that will add something
- **docs/[xyz-branch]**: development of documentation only
- **refactor/[xyz-branch]**: does not add anything nor does it fix a bug
- **test/[xyz-branch]**: adds a test or corrects a test
- **fix/[xyz-branch]**: fixes an existing feature

#### Submitting a pull request
Before submitting a pull request, please consider our workflow. Here is an
example of what that might be:

- Identify a point of concern.
- Search the PR history for anyone else that might have already done the task.
- Search issues and common forums for discussion.
- Make sure you do the above well. You don't want to duplicate effort.
- Make changes in a new branch following the branch naming guidelines.
```
	git checkout -b fix/my-fix-branch development
```
- Commit your changes
```
	git commit -a
```
- Push your branch to GitHub
```
	git push origin fix/my-fix-branch
```
- In GitHub, send a pull request to `ourdistrict:development`

That's the push and the pull.

**After your pull request is merged**

Now you can safely delete your branch and pull changes from the upstream repo.
- Delete the remote branch.
```
	git push origin --delete fix/my-fix-branch
```
- Check out the development branch
```
	git checkout development -f
```
- Delete the local branch
```
	git branch -D fix/my-fix-branch
```
- Update your development with the latest upstream
```
	git pull -ff upstream development
```
