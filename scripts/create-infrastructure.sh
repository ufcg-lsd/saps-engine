##!/bin/sh
# It creates the infrastructure (compute and attached volumes) as specified in the spec_file_path

#TODO: it would be better to add volume spec in the spec_file_path as well (we decided not to do because it needs some refactor in sebal old code base)

#TODO: update help
show_help() {
	echo "Illegal number of parameters. Usage: $0 workload nforeground nbackground trace_tool"
	echo -n "Usage:  $0 -w | --workload GUEST_USERNAME "
	echo -n "-bg | --nbackground NUMBER_OF_BACKGROUND_JOBS"
	echo -n "-fg | --nforeground NUMBER_OF_FOREGROUND_JOBS"
	echo -n "-bs | --bsize BLOCK_SIZE"
	echo "-t | --trace_tool TRACE_TOOL"

	echo -e "\t$0 -h | --help"

	echo
	echo "-h  | --help: shows this help"
	echo "-t  | --trace_tool: the trace tool used (baseline | strace | stap)"
}

define_parameters() {
	while [ ! -z $1 ]; do
		case $1 in
			-vs | --volume_size)
				shift;
				volume_size=$1;
				;;
			-h | --help | *)
				show_help;
				exit 0;
				;;
		esac
		shift
	done
}

if [ "$#" -lt 1 ]; then
	echo "Usage: $0 spec_file_path [--volume_size size (opt)] "
	exit 1
fi

spec_file_path=$1
if [ ! -f "$spec_file_path" ]
then	
	echo "File not found: " $spec_file_path
	exit 1
fi

shift
define_parameters $@
echo $volume_size

# if the user did not specify volume_size opt arg, do not create the volume
#TODO: it would be better to add volume spec in the spec_file_path as well (we decided not to do because it needs some refactor in sebal old code base)

#update volume sizes
echo "Creating infrastructure..."
java -cp target/sebal-scheduler-0.0.1-SNAPSHOT.jar:target/lib/* org.fogbowcloud.infrastructure.InfrastructureMain src/main/resources/sebal.conf $spec_file_path $WANT_STORAGE
echo "Operation finished."
