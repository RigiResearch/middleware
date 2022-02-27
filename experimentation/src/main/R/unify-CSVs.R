#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

args[1]="/Users/miguel/Desktop/deployments"
args[2]="constant"
args[3]="proxy-cache-3.1"

if (length(args)!=3) {
  stop("Three arguments were expected: the root dir, the execution scenario and the architecture variant",
       call.=FALSE)
}

library(plyr)
library(dplyr)

name = sprintf("%s-scenario-%s", args[2], args[3])
root = sprintf("%s/%s", args[1], name) # e.g., /Users/miguel/Desktop/deployments/constant-scenario-proxy-cache-3.1
filename = sprintf("%s.csv", name) # e.g., constant-scenario-proxy-cache-3.1.csv
identifiers = list.dirs(path=root, full.names=FALSE, recursive=FALSE) # e.g., "1-1-10", "1-1-2", ...
identifiers = identifiers[grep("\\d+\\-\\d+\\-\\d+", identifiers)]
filenames = character(length(identifiers))
combined = sprintf("%s/all.csv", root)

# Duplicate the file, adding the configuration id as a new column
for (i in 1:length(identifiers)) {
  id = identifiers[i]
  target = sprintf("%s/%s/%s", root, id, filename)
  newfile = sprintf("%s_formatted.csv", target)
  filenames[i] = newfile
  csv = read.csv(target)
  # Name the new column "pattern" to reuse the R code from the architecture experiments
  csv$pattern = id
  write.csv(csv, file=newfile, row.names=FALSE)
}

# Combine all CSV files into one single file
# From: https://stackoverflow.com/a/67207118/738968
combined_csv <- ldply(filenames, read.csv, header=TRUE, sep=',')
write.csv(combined_csv, file=combined, row.names=FALSE)

print(combined)
