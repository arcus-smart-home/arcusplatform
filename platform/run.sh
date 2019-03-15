#!/bin/bash
#
#
#
#
#

function new_tab() {
  TAB_NAME=$1
  COMMAND=$2
  osascript \
    -e "tell application \"Terminal\"" \
    -e "tell application \"System Events\" to keystroke \"t\" using {command down}" \
        -e "do script \"printf '\\\e]1;$TAB_NAME\\\a'; $COMMAND\" in front window" \
    -e "end tell" > /dev/null
  sleep 15
}


# setup some colors
red=`tput setaf 1`
green=`tput setaf 2`
reset=`tput sgr0`

echo ${green}"Using Java" $(java -version 2>&1 | awk '/version/{print $NF}')${reset}
## HomeBrew setup
if ! which 'brew' &>/dev/null; then
    echo "HomeBrew not installed"

    read -p ${red}"Continue (y/n)?"${reset} choice
    case "$choice" in
      y|Y ) ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"; brew doctor;;
      * ) echo "invalid";;
    esac
else
    echo ${green}"Using" $(brew -v)${reset}
fi

## Docker setup
if ! which 'docker' &>/dev/null; then
    echo "Docker not installed"

    rread -p ${red}"Continue (y/n)?"${reset} choice
    case "$choice" in
      y|Y ) brew install caskroom/cask/brew-cask; brew cask install virtualbox; brew install docker boot2docker; boot2docker init;;
      * ) echo "invalid";;
    esac
else
    echo ${green}"Using" $(docker -v)${reset}
fi

## Gradle setup
if ! which 'gradle' &>/dev/null; then
    echo "Gradle not installed"

    read -p ${red}"Continue (y/n)?"${reset} choice
    case "$choice" in
      y|Y ) brew install gradle;;
      * ) echo "invalid";;
    esac
else
    echo ${green}"Using Gradle" $(gradle --version | grep '^Gradle ' | sed 's/Gradle //g')${reset}
fi

## Cassandra setup
if ! which 'cassandra' &>/dev/null; then
    echo "Cassandra not installed"

    read -p ${red}"Continue (y/n)?"${reset} choice
    case "$choice" in
      y|Y ) brew install python cassandra;;
      * ) echo "invalid";;
    esac
else
    echo ${green}"Using Cassandra" $(cassandra -v)${reset}
fi

# Start boot2docker and setup shell with
if [ "$(boot2docker status)" == "poweroff" ]; then
    boot2docker start; $(boot2docker shellinit)
fi

DIR=$(dirname $0)

gradle -p $DIR clean install -x test
gradle -p $DIR startPlatform

cqlsh cassandra.eyeris -k dev -f $DIR/arcus-modelmanager/src/test/data/test_data.cql
echo "You now need to create a user test via either oculus or a mobile app"

$(new_tab "PlatformService" "cd '$DIR'/arcus-containers/platform-services; gradle run")
$(new_tab "ClientBridge" "cd '$DIR'/arcus-containers/client-bridge; gradle run")
