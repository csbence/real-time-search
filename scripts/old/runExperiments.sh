#!/bin/bash

SCRIPT=$(basename $0)
OPTIONS=":hf:c:d:m:a:n:t:p:e:i:Ib:o:DgG:"
PROJECT_NAME="real-time-search"
GRADLE=./gradlew
BUILD_DIR=build
DEFAULT_DIR="unknown"
RESULTS_TOP_DIR="results"
DIR=$RESULTS_TOP_DIR
INSTALL_DIR="$BUILD_DIR/install/$PROJECT_NAME"
BIN="$INSTALL_DIR/bin"
JAR="$INSTALL_DIR/lib/$PROJECT_NAME*.jar"
RUN_SCRIPT="$BIN/$PROJECT_NAME"
NUM_RUNS=1
RUN_NUM=1
OUT_EXT=.json
IGNORE_ERR=false
IBM_PATH=/opt/ibm/java-x86_64-80/bin/java
IBM_ARGS="-Xgc:targetPauseTime=20 -Xverbosegclog -Xgc:nosynchronousGCOnOOM -verbose:gc -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xmx8g -Xgcpolicy:metronome"

usage() {
# lim:  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
  echo "usage:"
  echo "$SCRIPT [options]"
  echo "options:"
  echo "  h                  show this usage info"
  echo "  o <file>           specify an output file name"
  echo "  I                  ignore errors in results when performing multiple runs"
  echo "  b <path>           provide path to IBM jdk; default $IBM_PATH"
  echo "  n <num>            specify the number of experiment runs"
  echo "file options: (overwrite separate options)"
  echo "  f <file>           specify configuration file"
  echo "  c <config>         specify configuration string"
  echo "separate options:"
  echo "  d <domain>         specify the domain to run against"
  echo "  a <name>           specify the algorithm to run"
  echo "  m <file>           specify a map input file"
  echo "  t <type>           specify the termination type"
  echo "  p <param>          specify the time limit to provide"
  echo "  e <key(type)=val>  specify key/value pair for extra parameters"
  echo "  i <name>           specify an instance name for the configuration"
  echo "distribution options:"
  echo "  D                  run the experiments via installed distribution"
  echo "  g                  install the dist with gradle before running"
  echo "  G <args>           install the dist with gradle before running with parameters"
  echo "Default running will be done with 'gradle run' unless -D parameter is given in "
  echo "  which case the installed jar will be used from 'gradle installDist'.  The IBM "
  echo "  JDK will be used if found; otherwise default JDK."
  echo "Results will be placed in separate files with the following directory structure:"
  echo "  results/algorithm/domain/params/[instance]/out"
  echo "If a parameter is not given then the directory name will be '$DEFAULT_DIR'."
  echo "The output file will be appended with 2 numbers with format 'out_XX_YY',"
  echo "  where XX is a unique digit and YY is the run number of that set of runs."
}

# Makes the given directory.  Uses 'mkdir -p' so multiple directories may be 
# created in a single call.  Formats the given directory such that it replaces 
# any invalid characters with '_' (underscore).
# arg1: the directory path to create
add_dir() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    NEW_DIR=$(echo $1 | sed -e 's/[^A-Za-z0-9._-\*]/_/g')
    DIR="$DIR/$NEW_DIR"
    if [ ! -d "$DIR" ]; then
      mkdir -p "$DIR"
    fi
  fi
}

# Adds an argument to EXPERIMENT_ARGS.  Handles the format of the arguments 
# based on whether the experiment will be run through gradle or not.
# arg1: the argument switch
# arg2: the argument value
add_arg() {
  if [ -z "$1" ] || [ -z "$2" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    if [ -n "$DIST" ] && [ "$DIST" = true ]; then
      echo "$EXPERIMENT_ARGS $1 \"$2\""
    else
      echo "$EXPERIMENT_ARGS'$1','$2',"
    fi
  fi
}

# Format the provided number into '_XY' format where X is a leading 0 if Y < 10.
# arg1: the number to format
get_file_num() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    if [ $1 -lt 10 ]; then
      echo "_0$1"
    else
      echo "_$1"
    fi
  fi
}

# Returns a unique filename within the provided path by appending numbers in 
# the form '_XX'.  Checks for uniqueness with the OUT_EXT file extension 
# appended to the end of the filename.
# arg1: the path and initial filename
get_unique_filename() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    CURRENT=_00
    COUNTER=0
    while [ -f "$1$CURRENT$OUT_EXT" -o -f "$1${CURRENT}_$RUN_NUM$OUT_EXT" -o -f "$1${CURRENT}_0$RUN_NUM$OUT_EXT" ]; do
      let COUNTER+=1
      CURRENT=$(get_file_num $COUNTER)
    done
    echo "$1$CURRENT"
  fi
}

# Parses a configuration file and sets up the directory structure based on the 
# results in the form:
# results/algorithm/domain/params/instance
# arg1: the configuration file
get_dirs_from_config() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    PCONFIG=$(echo $1 | sed -re 's/\\/\\\\/g')
SUB_DIRS=$(python <(cat <<EOF
import json
config = json.loads('$PCONFIG')
algorithm = config['algorithmName']
domain = config['domainName']
termType = config['terminationType']
timeLimit = config['timeLimit'] if 'timeLimit' in config else None
duration = config['actionDuration'] if 'actionDuration' in config else None
instanceName = config['domainInstanceName'] if 'domainInstanceName' in config else None
if instanceName is not None:
    instanceName = instanceName.replace("/", "_")
lookaheadDepthLimit = config['lookaheadDepthLimit'] if 'lookaheadDepthLimit' in config else None
commitmentStrategy = config['commitmentStrategy'] if 'commitmentStrategy' in config else None
singleStepLookahead = config['singleStepLookahead'] if 'singleStepLookahead' in config else None
print algorithm + "/" + domain + "/" + termType + "-" + str(timeLimit) + "-" + str(duration) + "-" + (str(lookaheadDepthLimit) if lookaheadDepthLimit is not None else "None") + "-" + (commitmentStrategy if commitmentStrategy is not None else "None") + "-" + (str(singleStepLookahead) if singleStepLookahead is not None else "None") + "/" + instanceName
EOF
))
    add_dir "$SUB_DIRS"
  fi
}

# Checks a result file for error.  Prints a message if the result file cannot 
# be found, cannot be parsed, or contains a non-empty 'errorMessage' value and 
# also terminates if IGNORE_ERR is set ot false.
# arg1: the result file
check_error() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    # Read output file to check for errors
ERR=$(python <(cat <<EOF
import json
try:
  msg = json.loads(open('$NEW_OUT', 'r').read())['errorMessage']
  if msg != None:
    print msg
except IOError as e:
  print "I/O error({0}): {1}".format(e.errno, e.strerror)
EOF
))
    if [ -n "$ERR" ]; then
      echo "Detected error in file '$NEW_OUT': $ERR"
      if [ "$IGNORE_ERR" = false ]; then
        echo "Terminating..."
        exit 1
      fi
    fi
  fi
}

# Run an experiment configuration using the gradle run target and specifying 
# the app arguments with the special -PappArgs parameter added to build.gradle.
# arg1: output file
run_gradle() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    $GRADLE run $GRADLE_PARAMS -PappArgs="[$(add_arg "-o" "$1")]"
  fi
}

# Run an experiment configuration using the jar file produced by running the 
# installDist gradle target.
# arg1: output file
run_dist() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    if [ "$USE_IBM" = true ]; then
      eval $IBM_PATH $IBM_ARGS -jar $JAR $(add_arg "-o" "$1")
    else
      eval $RUN_SCRIPT $(add_arg "-o" "$1")
    fi
  fi
}

# Run a set of experiments.  Executes configuration NUM_RUNS times and stores 
# results for each in a different file.
# arg1: either run_dist or run_gradle to select run method
run() {
  if [ -z "$1" ]; then
    >&2 echo "Internal script error: missing parameter to $FUNCNAME"
    exit 1
  else
    EXP_SCRIPT="$1"
    if [ "$NUM_RUNS" -eq 1 ]; then
      NEW_OUT="$OUT_FILE$OUT_EXT"
      $EXP_SCRIPT "$NEW_OUT"
      check_error "$NEW_OUT"
    else
      for ((i=1; i <= $NUM_RUNS; i++)); do
        RUN_NUM=$i
        NEW_OUT="$OUT_FILE$(get_file_num $i)$OUT_EXT"
        echo "Starting run #$RUN_NUM of $NUM_RUNS..."
        $EXP_SCRIPT "$NEW_OUT"
        echo "Finished run #$RUN_NUM of $NUM_RUNS. Results output to: $NEW_OUT"
        check_error "$NEW_OUT"
      done
    fi
  fi
}

while getopts "$OPTIONS" arg; do
  case $arg in
    h)
      usage
      exit 0
      ;;
    f)
      FILE_CONFIG="$OPTARG"
      ;;
    c)
      FILE_CONFIG="$OPTARG"
      ;;
    d)
      DOMAIN=$OPTARG
      ;;
    m)
      MAP=$OPTARG
      if [ ! -f $MAP ]; then
        >&2 echo "Map file $MAP does not exist"
        usage
        exit 1
      fi
      EXPERIMENT_ARGS=$(add_arg "-m" "$MAP")
      ;;
    a)
      ALG=$OPTARG
      ;;
    n)
      NUM_RUNS=$OPTARG
      ;;
    t)
      TERM_TYPE=$OPTARG
      ;;
    p)
      TIME_LIMIT=$OPTARG
      ;;
    e)
      EXPERIMENT_ARGS=$(add_arg "-e" "$OPTARG")
      ;;
    i)
      INSTANCE_NAME=$OPTARG
      ;;
    I)
      IGNORE_ERR=true
      ;;
    b)
      IBM_PATH=$OPTARG
      ;;
    o)
      OUT_FILE=$OPTARG
      ;;
    D)
      DIST=true
      ;;
    g)
      RUN_GRADLE=true
      ;;
    G)
      RUN_GRADLE=true
      GRADLE_PARAMS=$OPTARG
      ;;
    \?)
      >&2 echo "Invalid argument given: '$OPTARG'"
      usage
      exit 1
      ;;
    :)
      >&2 echo "Option '$OPTARG' requires a parameter"
      usage
      exit 1
      ;;
  esac
done

shift $((OPTIND-1))

# Experiment Directory Structure:
# results/algorithm/domain/params/instance/out
if [ ! -d "$RESULTS_TOP_DIR" ]; then
  mkdir $RESULTS_TOP_DIR
fi
if [ -z "$OUT_FILE" ]; then
  OUT_FILE="out"
fi

#TODO change to allow overriding config file with arguments
# Process config from file or separate run parameters
if [ -n "$FILE_CONFIG" ]; then
  EXPERIMENT_ARGS=""
  if [ -f "$FILE_CONFIG" ]; then
    CONFIG="$(cat $FILE_CONFIG)"
    get_dirs_from_config "$CONFIG"
    EXPERIMENT_ARGS=$(add_arg "-f" "$FILE_CONFIG")
  else
    CONFIG="$FILE_CONFIG"
    get_dirs_from_config "$CONFIG"

    # Fix escapes for passing to application
    CONFIG=$(echo $CONFIG | sed -re 's/(\\n)/ /g')
    CONFIG=$(echo $CONFIG | sed -re 's/\\"/\\\\"/g')
    CONFIG=$(echo $CONFIG | sed -re 's/"/\\"/g')
    EXPERIMENT_ARGS=$(add_arg "-c" "$CONFIG")
  fi
else
  # Translate to experiment expected args and build directory structure
  if [ -n "$ALG" ]; then
    EXPERIMENT_ARGS=$(add_arg "-a" "$ALG")
    add_dir "$ALG"
  else
    add_dir "$DEFAULT_DIR"
  fi

  if [ -n "$DOMAIN" ]; then
    EXPERIMENT_ARGS=$(add_arg "-d" "$DOMAIN")
    add_dir "$DOMAIN"
  else
    add_dir "$DEFAULT_DIR"
  fi

  if [ -n "$TERM_TYPE" ]; then
    EXPERIMENT_ARGS=$(add_arg "-t" "$TERM_TYPE")
    PARAM_DIR="$TERM_TYPE"
  fi
  if [ -n "$TIME_LIMIT" ]; then
    EXPERIMENT_ARGS=$(add_arg "-p" "$TIME_LIMIT")
    PARAM_DIR="$PARAM_DIR-$TIME_LIMIT"
  fi
  add_dir "$PARAM_DIR"

  if [ -n "$INSTANCE_NAME" ]; then
    add_dir "$INSTANCE_NAME"
  fi
fi

# Detect if the IBM path exists
if [ ! -f "$IBM_PATH" ]; then
  >&2 echo "Could not find IBM JDK at '$IBM_PATH'; using default JDK:"
  which java
else
  USE_IBM=true
fi

# Setup out file
OUT_FILE=$(get_unique_filename "$DIR/$OUT_FILE")

# Run it
if [ -n "$DIST" ] && [ "$DIST" = true ]; then
  if [ -n "$RUN_GRADLE" ] && [ "$RUN_GRADLE" = true ]; then
    echo "Running gradle with parameters '$GRADLE_PARAMS'..."
    $GRADLE installDist $GRADLE_PARAMS
    echo "Gradle finished"
  fi

  if [ ! -d "$INSTALL_DIR" ]; then
    >&2 echo "'$INSTALL_DIR' directory does not exist; build first or run with gradle"
    usage
    exit 1
  fi

  date
  run run_dist
  date
else
  date
  run run_gradle
  date
fi
