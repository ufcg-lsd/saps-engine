#!/bin/bash

Rscript check-R-dependency.R raster
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing raster dependency"
else
  echo "raster is installed"
fi

Rscript check-R-dependency.R rgeos
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing rgeos dependency"
else
  echo "rgeos is installed"
fi

Rscript check-R-dependency.R rgdal
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing rgdal dependency"
else
  echo "rgdal is installed"
fi

Rscript check-R-dependency.R maptools
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing maptools dependency"
else
  echo "maptools is installed"
fi

Rscript check-R-dependency.R ncdf4
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing ncdf4 dependency"
else
  echo "ncdf4 is installed"
fi

Rscript check-R-dependency.R sp
PROCESS_OUTPUT=$?

if [ $PROCESS_OUTPUT -ne 0 ]
then
  echo "Missing sp dependency"
else
  echo "sp is installed"
fi
