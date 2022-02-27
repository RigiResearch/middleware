#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

if (length(args)!=3) {
  stop("Three arguments were expected: the root dir, the execution scenario and the architecture variant",
       call.=FALSE)
}

######################################################
# 1. Setup global variales
# 2. Main functions to create the charts
# 3. Control flow
######################################################

#if (!require("devtools")) install.packages("devtools")
#devtools::install_github("mkuhn/dict")
#devtools::install_github("bbc/bbplot")

library(lubridate)
library(dplyr)
library(ggplot2)
library(bbplot)
library(ggExtra)
library(grid)
require(gridExtra)
library(dict)
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

################################################
# 1. Setup global variales
################################################

# Replace the original function to save the plots
source(sprintf("%s/finalise-plot.R", getCurrentFileLocation()), local=TRUE)

root = args[1] # e.g., /Users/miguel/Desktop/deployments
traffic_type = args[2] # e.g., constant
variant = args[3] # e.g., proxy-cache-3.1
directory = sprintf("%s/%s-scenario-%s", root, traffic_type, variant)

# Set the default ggplot2 theme
# theme_set(bbc_style())

height = 220
width = 400
dir = "charts"
custom_style <- theme_minimal() + #bbc_style() +
  theme(legend.position="none") +
  theme(panel.grid.major.y=element_line(color="#eeeeee")) +
  theme(plot.title=element_text(size=18,color="#063376",hjust=0)) +
  theme(plot.subtitle=element_text(size=12,margin=margin(6, 0, 8, 0),hjust=0)) +
  theme(legend.text=element_text(size=12)) +
  theme(axis.text=element_text(size=11)) +
  theme(axis.text.x=element_text(size=9,color="#666666"))

# Helper functions/vars
format_latency = function(num) sprintf("%.0fs", num/1000)
breaks_for = function(mean){
  if(mean < 10000){
    latency_breaks <- 0:160000*1000
  } else {
    latency_breaks = 0:160000*15000
  }
  return(latency_breaks)
}
time_format = "%M:%S"

################################################
# 2. Main functions to create the charts
################################################

generate_charts_for_pattern <- function(root_dir, pattern, generate.miniatures){
  miniature_title=sprintf("%s - %s", traffic_type, pattern)
  page_title=miniature_title

  if(!generate.miniatures){
    miniature_title=element_blank()
  }

  # Read the resulting CSV file
  msrmts = read.csv(sprintf("%s/%s/%s-scenario-%s.csv", root_dir, pattern, traffic_type, variant))

  # Alter columns
  msrmts$start <- ymd_hms(as_datetime(msrmts$timeStamp/1000))
  # Subtract the first (date)time to represent the elapsed time from 00:00:00
  first = msrmts$start[1]
  hour(msrmts$start) <- hour(msrmts$start)-hour(first)
  minute(msrmts$start) <- minute(msrmts$start)-minute(first)
  second(msrmts$start) <- second(msrmts$start)-second(first)

  msrmts$end <- msrmts$start + as.duration(msrmts$Latency/1000)
  msrmts$success <- factor(with(msrmts,ifelse(grepl("true",msrmts$success), "success", "error")))
  msrmts$type <- factor(with(msrmts,ifelse(grepl("playlist",msrmts$URL), "playlist", "video")))
  msrmts$pattern <- pattern

  # Filtered data by result type
  successful <- msrmts[grep("success",msrmts$success),]

  # Charts!

  # 1. Users over time (secondly)
  summarized_msrmts <- group_by(msrmts,datetime_bucket=floor_date(start,"30 second"),label) %>% summarise(transaction_count=n())
  s_users_subtitle = "Number of requests (aggregated every 30s)"
  if(!generate.miniatures){
    s_users_subtitle = pattern
  }
  s_users <- ggplot(summarized_msrmts,aes(x=datetime_bucket,y=transaction_count,colour=label,group=label)) +
    geom_line(size=1) +
    custom_style +
    theme(legend.position="none") +
    # Labels
    labs(title=miniature_title,subtitle=s_users_subtitle) +
    # Axis
    scale_y_continuous("Transaction Count",breaks=0:100*20) +
    scale_x_datetime(date_breaks="2 min",date_labels=time_format)

  if(generate.miniatures){
    finalise_plot(plot_name = s_users,
                  save_filepath = sprintf("%s/%s/users_%s.pdf", root_dir, dir, pattern),
                  width_pixels = width,
                  height_pixels = height)
  }

  # 2. Errors/Successes
  s_successes_errors_subtitle = "Request results"
  if(!generate.miniatures){
    s_successes_errors_subtitle = pattern
  }
  s_successes_errors <- ggplot(msrmts, aes(x=type,group=interaction(success,type),fill=success)) +
    geom_bar(position=position_dodge()) +
    # Number of measurements
    geom_text(stat="count",aes(label=..count..),vjust=1.5,position=position_dodge(0.9)) +
    custom_style +
    # Labels
    labs(title=miniature_title,subtitle=s_successes_errors_subtitle,x="Number of requests",y="Result type")

  if(generate.miniatures){
    finalise_plot(plot_name = s_successes_errors,
                  save_filepath = sprintf("%s/%s/successes_errors_%s.pdf", root_dir, dir, pattern),
                  width_pixels = width,
                  height_pixels = height)
  }

  # 2. Latency for request type

  # Compare two variances: https://www.r-bloggers.com/f-test-compare-two-variances-in-r/
  # playlist vs latency
  # It requires the same number of measurements
  # ftest = var.test(successful[successful$type=="video",]$Latency,
  #                  successful[successful$type=="playlists",]$Latency,
  #                  alternative="two.sided")

  s_latency_subtitle = "Latency (successful requests)"
  if(!generate.miniatures){
    s_latency_subtitle = pattern
  }
  latency_breaks <- breaks_for(mean(successful$Latency))
  s_latency <- ggplot(successful, aes(x=end,y=Latency,color=success)) +
    geom_point() +
    geom_hline(yintercept=mean(successful$Latency),colour="#17BECF") +
    custom_style +
    theme(legend.position="none") +
    # Labels
    labs(title=miniature_title,subtitle=s_latency_subtitle,x="Latency",y="Time") +
    # Axis
    scale_y_continuous(breaks=latency_breaks,labels=format_latency) +
    scale_x_datetime(date_breaks="2 min",date_labels=time_format)

  if(generate.miniatures){
    finalise_plot(plot_name = s_latency,
                  save_filepath = sprintf("%s/%s/latency_%s.pdf", root_dir, dir, pattern),
                  width_pixels = width,
                  height_pixels = height)
  }

  s_latency_type_subtitle = "Playlist vs video latency (successful requests)"
  if(!generate.miniatures){
    s_latency_type_subtitle = pattern
  }
  latency_breaks <- breaks_for(mean(successful$Latency))
  s_latency_type <- ggplot(successful, aes(x=end,y=Latency,group=type,color=type)) +
    geom_point() +
    custom_style +
    # Labels
    labs(title=element_blank(),subtitle=element_blank(),x=element_blank(),y=element_blank()) +
    # Axis
    scale_y_continuous(breaks=0:130000*15000,labels=format_latency,limits=c(0,130000)) +
    scale_x_datetime(date_breaks="2 min",date_labels=time_format)

  if(generate.miniatures){
    finalise_plot(plot_name = s_latency_type,
                  save_filepath = sprintf("%s/%s/latency_type_%s.pdf", root_dir, dir, pattern),
                  width_pixels = width,
                  height_pixels = height)
  }

  give.n <- function(x){
    # From https://stackoverflow.com/a/28846438/738968
    # experiment with the multiplier to find the perfect position
    return(c(y = median(x)*1.2, label = length(x)))
  }
  s_latency_type_box_subtitle = "Playlist vs video latency (successful requests)"
  if(!generate.miniatures){
    s_latency_type_box_subtitle = pattern
  }
  latency_breaks <- breaks_for(mean(successful$Latency))
  s_latency_type_box <- ggplot(successful,aes(x=type,y=Latency,color=type)) +
    geom_boxplot() +
    custom_style +
    theme(legend.position="none") +
    # Number of measurements
    stat_summary(fun.data=give.n,geom="text",fun=median,
                 position=position_dodge(width=.75)) +
    # Mean
    stat_summary(fun=mean,geom="errorbar",aes(ymax=..y..,ymin=..y..),
                 width=.75,linetype="dashed") +
    # Labels
    labs(title=miniature_title,subtitle=s_latency_type_box_subtitle,x="Request type",y="Latency") +
    # Axis
    scale_y_continuous(breaks=latency_breaks,labels=format_latency)

  if(generate.miniatures){
    finalise_plot(plot_name = s_latency_type_box,
                  save_filepath = sprintf("%s/%s/latency_type_box_%s.pdf", root_dir, dir, pattern),
                  width_pixels = width,
                  height_pixels = height)
  }

  # All charts!
  if(!generate.miniatures){
    ggsave(
      sprintf("%s/%s/%s_%s.pdf",root_dir,dir,traffic_type,pattern),
      arrangeGrob(top=textGrob(page_title,gp=gpar(fontsize=18,col="#063376",fontface="bold"),vjust=1),
                  s_users,s_successes_errors,s_latency,s_latency_type,s_latency_type_box),
      unit="in",
      width=8.5,
      height=11,
      dpi = 300,
    )
  }

  mydict <- dict()
  mydict[["successful"]] <- successful$Latency
  mydict[["successes_errors"]] <- s_successes_errors
  mydict[["users"]] <- s_users
  mydict[["latency"]] <- s_latency
  mydict[["latency_type"]] <- s_latency_type
  mydict[["latency_type_box"]] <- s_latency_type_box
  return(mydict)
}

generate_charts_per_type <- function(root_dir, boxes, types){
  ggsave(
    sprintf("%s/%s/%s_latency_boxes.pdf",root_dir,dir,traffic_type),
    arrangeGrob(top=textGrob("Latency Box",gp=gpar(fontsize=18,col="#063376",fontface="bold"),vjust=1),
                boxes[[1]],boxes[[2]],boxes[[3]],boxes[[4]],boxes[[5]],boxes[[6]]),
    unit="in",
    width=8.5,
    height=11,
    dpi = 300,
  )
  ggsave(
    sprintf("%s/%s/%s_latency_types.pdf",root_dir,dir,traffic_type),
    arrangeGrob(top=textGrob("Latency Type",gp=gpar(fontsize=18,col="#063376",fontface="bold"),vjust=1),
                types[[1]],types[[2]],types[[3]],types[[4]],types[[5]],types[[6]]),
    unit="in",
    width=8.5,
    height=11,
    dpi = 300,
  )
}

################################################
# 3. Control flow
################################################

generate.miniatures = TRUE
identifiers = list.dirs(path=directory, full.names=FALSE, recursive=FALSE) # e.g., "1-1-10", "1-1-2", ...
patterns = identifiers
boxes = list()
types = list()
i = 1

for (pattern in patterns){
  mydict <- generate_charts_for_pattern(directory,pattern,generate.miniatures)
  boxes[[i]] <- mydict[["latency_type_box"]]
  types[[i]] <- mydict[["latency_type"]]
  i = i + 1
}

generate_charts_per_type(directory, boxes, types)
