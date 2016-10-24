# Brownie

Distributed video converter, based on [Vert.x v3](http://vertx.io/) and [RxJava](https://github.com/ReactiveX/RxJava).

[![Build Status](https://travis-ci.org/KengoTODA/brownie.svg?branch=master)](https://travis-ci.org/KengoTODA/brownie)
[![Coverage Status](https://coveralls.io/repos/github/KengoTODA/brownie/badge.svg?branch=master)](https://coveralls.io/github/KengoTODA/brownie?branch=master)
[![codebeat badge](https://codebeat.co/badges/7e6fb749-341e-4b11-9042-9b1656370723)](https://codebeat.co/projects/github-com-kengotoda-brownie)

# Feature

- [x] Support [clustering](http://vertx.io/docs/#clustering) based on [Hazelcast](http://hazelcast.com/)
- [x] Support royalty-free format ([WebM](http://www.webmproject.org/)) only
- [ ] Support distributed file systems like AWS S3, Aliyun OOS, HDFS etc.
- [ ] Support multi presets to support many kinds of client
- [x] Support generating thumbnail
- [ ] Provide Swagger file for generating API client automatically
- [x] Provide admin console to manage running tasks and stored files
- [x] Provide Docker container to run brownie cluster
- [x] Provide recommended Docker Compose configuration for small cluster

# How to run

```
$ mvn clean package
$ java -DBROWNIE_MOUNTED_DIR=/tmp/brownie -jar target/brownie-*.jar
```
or
```
$ mvn clean package
$ java -DBROWNIE_MOUNTED_DIR=/tmp/brownie -DBROWNIE_CLUSTER_HOST="localhost" -jar target/brownie-*.jar
```
or
```
$ mvn clean package
$ docker-compose build
$ docker-compose up
```


# How to test

```
$ docker-compose up -d --force-recreate db file_storage_db
$ mvn clean verify
```

# Copyright and license

Copyright 2015-2016 Kengo TODA

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# Dependency

This product needs [ffmpeg](https://www.ffmpeg.org/) in the `PATH`.
