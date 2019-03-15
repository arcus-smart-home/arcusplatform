## Building Model Manager

Running the following command on the console will build the model manager and make it ready to run.

$ gradle installApp

## Running Model Manager

cd build/install/modelmanager/bin

./modelmanager -P dev

That tells it to use the dev profile, which I just updated to point to cassandra.eyeris rather than localhost.  If you leave -P off, it will by default try to look for a production profile, which has not been defined yet.  The profiles are in src/main/resources/profiles.

We don't create any keyspaces in our cassandra docker image at the moment (so one of us should probably look at that), but you can do so by doing the following on your local machine (cannot run cqlsh on the docker image due to missing python dependencies):

export CQLSH_HOST="cassandra.eyeris"

cqlsh> CREATE KEYSPACE dev WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

You should be displayed a list of changes that will be applied and prompted if you'd like to continue.

It doesn't generate a schema, it executes commands directly against cassandra.  At some point it would be nice to add a command line option to have it dump the describes for all the tables, but not a priority at the moment.
