#!/usr/bin/env Rscript

args = commandArgs(trailingOnly=TRUE)

if (length(args)==0) {
  stop("One argument was expected", call.=FALSE)
}

# install.packages(tidyverse)

library(dplyr)
library(tidyverse)

getCurrentFileLocation <-  function() {
  this_file <- commandArgs() %>%
    tibble::enframe(name = NULL) %>%
    tidyr::separate(col=value, into=c("key", "value"), sep="=", fill='right') %>%
    dplyr::filter(key == "--file") %>%
    dplyr::pull(value)
  if (length(this_file)==0)
  {
    this_file <- rstudioapi::getSourceEditorContext()$path
  }
  return(dirname(this_file))
}

filename = args[1] # "constant-scenario-proxy-cache-3.1.csv"
dataset = read.csv(sprintf("%s/%s", getCurrentFileLocation(), filename))

# Alter some columns
dataset$success <- factor(with(dataset,ifelse(grepl("true",dataset$success), "success", "error")))
dataset$type <- factor(with(dataset,ifelse(grepl("playlist",dataset$URL), "playlist", "video")))

# Filtered data by request type
videos <- dataset %>% filter(type=="video")
playlists <- dataset %>% filter(type=="playlist")

score <- function(requests){
  # Separate successful and erroneous requests
  successful <- requests[grep("success",requests$success),]
  erroneous <- requests[grep("error",requests$success),]
  error_proportion = nrow(erroneous)/nrow(requests)
  mean = mean(successful$Latency)
  fitness = mean*0.8 + error_proportion*0.2
  return(fitness)
}

final_score = score(videos) + score(playlists)

print(final_score)
