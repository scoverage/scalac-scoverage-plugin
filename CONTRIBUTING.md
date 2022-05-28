# Contributing

To run the tests, you will first need to run `npm install`. This will
install the JSDOM dependency used to run some of the Scala.js tests.

When working in the code base it's a good idea to utilize the
`.git-blame-ignore-revs` file at the root of this project. You can add it
locally by doing a:

```sh
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

This will ensure that when you are using `git blame` functionality that the
listed commit in that file are ignored.

## Making a release

scalac-scoverage-plugin relies on
[sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) for an automated
release process. In order to make this clear for anyone in the future that may
need to cut a release, I've outlined the steps below:

1. Tag a new release locally. `git tag -a vX.X.X -m "v.X.X.X"`
2. Push the new tag upstream. `git push upstream --tags` The tag will trigger a
   release via GitHub Actions. You can see this if you look in
   `.github/workflows/release.yml`.
3. Once the CI has ran, everything should be available pretty much right away.
   You can verify this with the script in `bin/test-release.sh`. Keep in mind
   that if you add support for a new Scala version, add it to the
   `test-release.sh` script.
4. Once the release is verified, update the draft release in
   [here](https://github.com/scoverage/scalac-scoverage-plugin/releases) and
   "publish" the release. This will notify everyone that follows the repo that a
   release was made and also serve as the release notes.
