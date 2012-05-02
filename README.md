Clisson Server
==============

Clisson is an event database for message processing systems.

Setup
-----

1. download the [latest packaged version](http://bimbr.com/downloads/clisson-server-0.2.0.zip)
2. unzip to a directory of your choice
3. edit `clisson-server.properties` and set `clisson.db.path` property to a directory where you'd like the database files stored

Running
-------

Run `clisson-server 8321`, where 8321 is the port the app should listen on.

Recording Events
----------------

To add events to the database one needs to issue a `POST` HTTP request to `/event` URI with the body containing a JSON describing the event. It's easiest done using [clisson-client library](https://github.com/mmakowski/clisson-client) invoked from a Java application.

Querying
--------

`curl http://localhost:9000/trail/msg001` responds with the JSON for trail of `msg001` or HTTP 404 if `msg001` is not in the database.
