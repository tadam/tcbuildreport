## How to build

Prerequisites:
- Java 8
- Docker (or exclude `buildImage` task)

```sh
git clone --recurse-submodules https://github.com/tadam/tcbuildreport.git
cd tcbuildreport
./gradlew build

# run backend on http://localhost:8080/
./gradlew backend:run
```


## How to use

### Health check
```sh
wget -O- -q http://localhost:8080/api/ping
```

### List of running builds
```sh
wget --content-on-error -O- -S --header="Content-Type: application/json" --header="Accept: application/json" --post-data='{ "servers": [ { "url": "https://teamcity.jetbrains.com" } ] }' 'http://localhost:8080/api/builds?sortOrder=desc'
```

Optional query parameters:
- `sortBy: server | startDate` (default `server`)
- `sortOrder: asc | desc` (default `asc`)
- `offset` (default `0`)
- `limit` (default 10)

Send list of servers as JSON in POST data:
```json
{
  "servers": [
    {
      "url": <teamcity_server_url>,
      "credentials": {
        "login": <login>,
        "password": <password>
      }
    },
    ...
  ]
}
```

`credentials` are optional. If not specified, then guest auth will be used.

Response:
```json
{
  "builds": [
    {
      "server": <teamcity_server_url>
      "id": <build_id>,
      "buildTypeId": <build_type_id>
      "buildNumber": <build_number>
      "startDate": <start_date>
      "webUrl": <teamcity_web_url_for_this_build>
    },
    ...
  ],
  "total": <total_number_of_running_builds_on_all_servers>
  "errors": [
    "error1",
    "error2",
    ...
  ]
```

If retrieval of running builds didn't succeed for some reason, it's reflected in `errors` list.


## Deployment in AWS

Read info in [deploy](./deploy).


## teamcity-rest-client

Original `teamcity-rest-client` [has been forked](https://github.com/tadam/teamcity-rest-client/tree/running-builds-selector) to make it asynchronous and add a couple of features. It's added as a git submodule in `tcbuildreport`.

There is a known issue (XXX) if you try to run this code in Docker container.