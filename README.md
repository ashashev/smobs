[Russian](README_ru.md)

# smobs ![build status][build-status]

A tool for simple migration of bitbucket server repositories to another bitbucket server

## Usage

Recommend to disable git password request and to turn off an anti-virus program. The tool uses the http(s) protocol to work with git repositories. The easiest way to disable git password request is to use [the credential store][git-credential-store].

The Java 8 is essential to run the tool. The run command looks like follow:

```
java -jar smobs-assembly-0.0.2.jar [options]
```

You can get help about the available options by running the following command:

```
java -jar smobs-assembly-0.0.2.jar --help
```

Options:

* `-c, --config <value>` path to the configuration file
* `-e, --example` outputs an example configuration file
*  `-n, --no-migrate` not perform migration, just shows that it will migrate. I recommend forwarding stderr to a file when you use this option.

The configuration file is a JSON file that describes the parameters for accessing servers and project filters.

Example:

```json
{
  "source":{
    "url":"http://example.com",
    "user":"<your login>",
    "password":"<your password>",
    "connectionTimeoutMs":1000,
    "readTimeoutMs":5000
  },
  "destination":{
    "url":"http://example.com",
    "user":"<your login>",
    "password":"<your password>",
    "connectionTimeoutMs":1000,
    "readTimeoutMs":5000
  },
  "useCredential":false,
  "includeProjects":[
    ".*"
  ],
  "excludeProjects":[
    
  ],
  "includeUsers":[
    ".*"
  ],
  "excludeUsers":[
    
  ],
  "addedProjectPrefix":""
}

```

* `source` - parameters for accessing Bitbucket Server which will act as a source.
* `destination` - the settings for accessing Bitbucket Server to which projects will migrate.
* `useCredential` - create and use a file for the [git credential store][git-credential-store]
* `includeProjects` is a list of regular expressions for selecting projects for migration.
* `excludeProjects` - a list of regular expressions for selecting projects that need to be excluded from migration. Have a higher priority.
* `includeUsers`, `excludeUsers` - is similar to `includeProjects`, `excludeProjects`, but for the migration of personal projects.
* `addedProjectPrefix` is the prefix that will be added to the project name on the destination server. It is also added for the project key in cases of conflict.

The tool recreates the structure of projects on the target server. If `addedProjectPrefix` is specified, it is inserted before the project name from the source server. If such a project already exists, it will be used as a target. If you need to create a new project, the project key is taken from the source server. But if such a key already exists on the receiving server, then it is substituted with `addedProjectPrefix`, if there is such a key, then the index `2`, then `3`, etc. is appended to the original key. In the case when the project key already ends with an index, for example `TMP11`, then it will be incremented, i.e. will become `TMP12`.

When migrating personal projects, users with the same "logins" should exist on the destination server and at least once log on to it. If the user does not find it on the target server, then its repositories will be excluded from the migration.

When the repository is migrated to the destination server, a repository is created in the target project with the same name as the source, if there is an LFS flag, then it is set. The repository is then cloned to the local machine and sent to the destination server. The following git commands are executed:

```
git clone --bare <repository>
git lfs fetch --all
git remote set-url origin <new_repository>
git push --all
git tags --all
git lfs push origin --all
```

The `git lfs` commands are executed only for repositories with LFS enabled. To migrate LFS projects, you need to install the [Git Large File Storage] [git-lfs-ext] extension.

**The utility does not migrate the permissions of projects and repositories, forks, pool of requests and comments to files.**

# Acknowledgments

I'm grateful to [Shipilov Denis][dartvaper]. He is the first tester and user of this tool. He gave me some very useful advices about CI on the github and some ideas how to make this tool better.

[git-credential-store]:https://git-scm.com/docs/git-credential-store
[git-lfs-ext]:https://git-lfs.github.com/
[build-status]:https://travis-ci.org/ashashev/smobs.svg?branch=master
[dartvaper]:https://github.com/dartvaper