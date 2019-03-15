#!/bin/bash

function runServices() {
	startPlatform
	$GRADLE -p $MAIN_DIR startService
}

function stopServices() {
	$GRADLE -p $MAIN_DIR stopService

	$GRADLE -p $MAIN_DIR rmiDocker
}

function checkForEyerisImagesInDocker() {
	if [ $(docker inspect --format="{{ .State.Running }}" eyeris* 2> /dev/null) ]; then
		$ECHO 1
	else
		$ECHO 0
	fi
}

function initBootDocker() {
    if [ -x $BOOTDOCKER -a "$(${BOOTDOCKER} status)" == "poweroff" ]; then
		$ECHO "Initializing boot2docker...."
		$BOOTDOCKER start
	fi

	if [ -x $BOOTDOCKER ]; then
		$ECHO "(Re)Initializing boot2docker shell...."
		$($BOOTDOCKER shellinit &> /dev/null)
	fi
}

function stopPlatform() {
	initBootDocker &> /dev/null

	$ECHO "Stopping Platform......"
	$GRADLE -p $MAIN_DIR stopPlatform
}

function startPlatform() {
	initBootDocker &> /dev/null
	setupHostNames

	if [ $(checkForEyerisImagesInDocker) == 0 ]; then
		$ECHO "Starting Platform......"
		$GRADLE -p $MAIN_DIR startPlatform
	else
		$ECHO "Using currently running instance of platform."
	fi

	loadTestData
	createUsers
}

# If an instance of the platform is already running, since this doesn't restart, does this "wipe" the data in cassandra?
function extraSteps() {
	if [ ! -x "${MAIN_DIR}/arcus-modelmanager/build/install/modelmanager/bin/modelmanager" ]; then
		# Not sure if this is needed.....
		$GRADLE -p "${MAIN_DIR}/arcus-modelmanager" installApp
	fi

	# Rerun the change-logs
	"${MAIN_DIR}/arcus-modelmanager/build/install/modelmanager/bin/modelmanager" \
		-H "${MAIN_DIR}/arcus-modelmanager/src/main/resources" -P dev -a
}

function restartPlatform() {
	if [ $(checkForEyerisImagesInDocker) -eq 1 ]; then
		stopPlatform
	fi

	startPlatform
}

function createUsers() {
	#no longer create test users via irisCreateUser
}

function showTestUsers() {
	
}

function loadTestData() {
	if [ -x $CQLSH ]; then
		$CQLSH $CASSANDRA_CLUSTER -k dev -f "${TEST_CQL_DATA_LOCATION}"
	else
		$ECHO "Cannot find/execute the ${CQLSH} - Will not be able to load data into Cassandra."
	fi
}

function setupHostNames() {
	if [ "$(uname)" == "Darwin" ]; then # Mac
   		APPEND_COMMAND="${APPEND_COMMAND} -a "
	elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
		APPEND_COMMAND="${APPEND_COMMAND} --append "
	fi

	if [ -x $BOOTDOCKER ] ; then
		IP_ADDRESS=`$BOOTDOCKER ip`
	fi

	if ! grep -q "cassandra.eyeris" /etc/hosts; then
   		echo "${IP_ADDRESS} cassandra.eyeris" | $APPEND_COMMAND /etc/hosts > /dev/null
	fi

	if ! grep -q "kafka.eyeris" /etc/hosts; then
   		echo "${IP_ADDRESS} kafka.eyeris kafkaops.eyeris" | $APPEND_COMMAND /etc/hosts > /dev/null
	fi

	if ! grep -q "zookeeper.eyeris" /etc/hosts; then
   		echo "${IP_ADDRESS} zookeeper.eyeris zookeeperops.eyeris" | $APPEND_COMMAND /etc/hosts > /dev/null
	fi

	if ! grep -q "client-bridge.eyeris" /etc/hosts; then
   		echo "${IP_ADDRESS} client-bridge.eyeris" | $APPEND_COMMAND /etc/hosts > /dev/null
	fi

	if ! grep -q "hub-bridge.eyeris" /etc/hosts; then
   		echo "${IP_ADDRESS} hub-bridge.eyeris" | $APPEND_COMMAND /etc/hosts > /dev/null
	fi

# 	if ! grep -q "driver-services.eyeris" /etc/hosts; then
#    		echo "${IP_ADDRESS} driver-services.eyeris" | $APPEND_COMMAND /etc/hosts > /dev/null
# 	fi
}

function systemTest() {
	startPlatform
	runServices

	$ECHO "n\n\n\n"
	echo "Running the system tests located in src/system-test/java"
	sleep 2

	$GRADLE --continue -p $MAIN_DIR systemTestRunner

	$ECHO "n\n\n\n"
	echo "Stopping containers and services"
	sleep 2
	stopServices
	stopPlatform
}


# run a system test that does not start/stop the platform, assumes running platform and services.
function unmanagedSystemTest() {
	echo "Please ensure you have the platform, and any needed services running, this does NOT start them"
	echo "Running all system tests located in src/system-test/java for the project"
	sleep 2

	$GRADLE --continue -p $MAIN_DIR systemTest
}

function singleSystemTest() {
    startPlatform
    runServices

	echo "Running the system tests located in src/system-test/java for $(command pwd)"
	sleep 2

	$GRADLE --continue -p $(command pwd) systemTestRunner
}

function checkDirectories() {
	if [ ! -d $MAIN_DIR -o ! -w $MAIN_DIR ]; then
		$ECHO "The directory \"${MAIN_DIR}\" does not exist or is not read/write -able by this script."
		$ECHO $USAGE
		exit 20
	elif [ ! -x $DOCKER ]; then
		$ECHO "FATAL: Docker not found; Cannot run system tests\n"
		exit 30
	fi
}












MAIN_DIR=$1
CASSANDRA_CLUSTER="cassandra.eyeris"
TEST_CQL_DATA_LOCATION="${MAIN_DIR}/arcus-modelmanager/src/test/data/test_data.cql"

ECHO="$(command -v echo) -e "
DOCKER=$(command -v docker)
GRADLE=$(command -v gradle)
BOOTDOCKER=$(command -v boot2docker)
CQLSH=$(command -v cqlsh)
APPEND_COMMAND="$(command -v sudo) $(command -v tee)"
IP_ADDRESS="127.0.0.1"

USAGE="Usage: $0 path_to_platform [target_from_list_below]"
USAGE_BLURB=(
"\tIf no target is provided, systemTest is assumed.\n\n"
"\trunServices\t\tExecutes \"gradle startPlatform\" & \"gradle startService\" -- loads all test data & creates users\n"
"\tstopServices\t\tExecutes \"gradle stopService\"\n"
"\tstopPlatform\t\tExecutes \"gradle stopPlatform\"\n"
"\tplatformRunning\t\tChecks if any platform eyeris images are running in the docker container\n"
"\tcreateUsers\t\tCreates \"test\" users\n"
"\tshowUsers\t\tShows test users created\n"
"\tloadTestData\t\tInserts data from [path_to_platform]${TEST_CQL_DATA_LOCATION}\n"
"\tsystemTest\t\tRuns gradle systemTest after initializing the environment via runServices\n"
"\tunmanagedSystemTest\tRun a system test that does not start/stop the platform, assumes running platform and services\n"
"\tsingleSystemTest\tRun a single system test - DOES start the platform AND services DOES load test data\n"
)
FULL_USAGE="${USAGE}\n\n${USAGE_BLURB[@]}"




if [ $# -lt 1 ]; then
	$ECHO "\n" $FULL_USAGE
	exit 10
fi

if [ $# -eq 1 ]; then
	case "$1" in
		platformRunning)
			if [ $(checkForEyerisImagesInDocker) == 1 ]; then
				$ECHO "Platform is running..."
			else
				$ECHO "Platform was not detected..."
			fi
			;;
		showUsers)
			showTestUsers
			;;
		*)
			checkDirectories
			systemTest
	esac
elif [ $# -gt 1 ]; then

	checkDirectories

	case "$2" in
		runServices)
			$2
			;;
		stopServices)
			$2
			;;
		stopPlatform)
			$2
			;;
		createUsers)
			$2
			;;
		showUsers)
			showTestUsers
			;;
		loadTestData)
			$2
			;;
		platformRunning)
			if [ $(checkForEyerisImagesInDocker) == 1 ]; then
				$ECHO "At least one platform instance WAS detected in the running state"
			else
				$ECHO "At least one platform instance was NOT detected in the running state"
			fi
			;;
		systemTest)
			$2
			;;
		unmanagedSystemTest)
			$2
			;;
		singleSystemTest)
			$2
			;;
		*)
			$ECHO $FULL_USAGE
			exit 1
	esac
fi
