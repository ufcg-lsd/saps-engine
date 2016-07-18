#!/bin/bash
VM_SCHEDULER_INFO="TESTE"

#if [[ ! $VM_SCHEDULER_INFO == *"INSTANCE_ID"* ]]; then
	#echo $VM_SCHEDULER_INFO
	#echo "There is no resource available for deploy Scheduler App."
	#exit
#fi
ECHO_COMMAND="echo (echo $var1) and (echo $var2)";
echo " - - - -- "
echo $?

teste="teste"

var1="Var1Value";
var2="Var2Value";
$ECHO_COMMAND;

if [ $teste == "teste" ]; then
	echo $teste
	echo "Is equal"
	
	if [ $teste == "teste" ]; then
		echo $teste
		echo "Is equal again"
			
	fi
fi


DB_STORAGE_PORT="";
while [ -z $DB_STORAGE_PORT]
do
	DB_STORAGE_PORT=$(curl http://150.165.15.81:2223/token/d4507e70-a2c7-4215-a921-213c8a5e9576-postgres);
	if [ "$DB_STORAGE_PORT" = "404 Port Not Found" ]; then
		echo "No port ..."
		DB_STORAGE_PORT="";
	fi
	sleep 10

done

echo $DB_STORAGE_PORT;