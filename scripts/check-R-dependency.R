options(echo=TRUE)
rm(list=ls())

args = commandArgs(trailingOnly=TRUE)
pkg<-args[1]

exists <- pkg %in% rownames(installed.packages())

if(isTRUE(exists)) {
  quit(save = "no", status = 0, runLast = TRUE)
}
quit(save = "no", status = 1, runLast = FALSE)