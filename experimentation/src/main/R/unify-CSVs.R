#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

if (length(args)==0) {
  stop("Two arguments were expected: the root dir and the CSV filename", call.=FALSE)
}

root = args[1] # e.g., /Users/miguel/Desktop/deployments/constant-scenario-proxy-cache-3.1
filename = args[2] # e.g., constant-scenario-proxy-cache-3.1.csv
identifiers = list.dirs(path=root, full.names=FALSE, recursive=FALSE) # e.g., "1-1-10", "1-1-2", ...
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
# From: https://stackoverflow.com/a/67216573/738968
# 1. read the files in as plain text
csv_list <- lapply(filenames , readLines)
# 2. remove the header from all but the first file
csv_list[-1] <- sapply(csv_list[-1], "[", 2)
# 3. unlist to create a character vector
csv_list <- unlist(csv_list)
#4. write the csv as one single file
writeLines(text=csv_list, con=combined)

print(combined)
