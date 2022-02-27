#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

if (length(args)!=3) {
  stop("Three arguments were expected: the root dir, the execution scenario and the architecture variant",
       call.=FALSE)
}

######################################################
# Charts: Summary
######################################################

library(lubridate)
library(dplyr)
library(ggplot2)
library(bbplot)
library(ggExtra)
library(grid)
require(gridExtra)

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

# Replace the original function to save the plots
source(sprintf("%s/finalise-plot.R", getCurrentFileLocation()), local=TRUE)

root = args[1] # e.g., /Users/miguel/Desktop/deployments
traffic_type = args[2] # e.g., constant
variant = args[3] # e.g., proxy-cache-3.1
directory = sprintf("%s/%s-scenario-%s", root, traffic_type, variant)

generate.miniatures = TRUE
dataset = read.csv(sprintf("%s/all.csv", directory))

# Alter columns
dataset$pattern <- factor(dataset$pattern)
dataset$start <- ymd_hms(as_datetime(dataset$timeStamp/1000))

# Subtract the first (date)time to represent the elapsed time from 00:00:00
first = dataset$start[1]
hour(dataset$start) <- hour(dataset$start)-hour(first)
minute(dataset$start) <- minute(dataset$start)-minute(first)
second(dataset$start) <- second(dataset$start)-second(first)

dataset$end <- dataset$start + as.duration(dataset$Latency/1000)
dataset$success <- factor(with(dataset,ifelse(grepl("true",dataset$success), "success", "error")))
dataset$type <- factor(with(dataset,ifelse(grepl("playlist",dataset$URL), "playlist", "video")))

# Successfull requests only
successful <-  dataset %>% filter(success == "success")

# Set the default ggplot2 theme
theme_set(bbc_style())
height = 800
width = 500
dir = "charts"
miniature_title=sprintf("Summary")
page_title=miniature_title
custom_style <- bbc_style() +
  theme(legend.position="none") +
  theme(panel.grid.major.y=element_line(color="#eeeeee")) +
  theme(plot.title=element_text(size=18,color="#063376",hjust=0)) +
  theme(plot.subtitle=element_text(size=12,margin=margin(6, 0, 8, 0),hjust=0)) +
  theme(legend.text=element_text(size=12)) +
  theme(axis.text=element_text(size=11)) +
  theme(axis.text.x=element_text(angle=0,hjust=1,size=9,color="#666666"))

if(!generate.miniatures){
  miniature_title=element_blank()
}

# Helper functions/vars
format_latency = function(num) sprintf("%.0fs", num/1000)
time_format = "%M:%S"

# mean
latency_mean <- successful %>%
  ggplot(aes(x=pattern,y=Latency,group=type,fill=type)) +
  stat_summary(fun="mean",geom="bar") +
  custom_style +
  coord_flip() +
  # Labels
  labs(title=miniature_title,subtitle="Mean latency",x="Pattern",y="Latency") +
  # Axis
  scale_y_continuous(breaks=0:160000*25000,labels=format_latency)

latency_median <- successful %>%
  ggplot(aes(x=pattern,y=Latency,group=type,fill=type)) +
  stat_summary(fun="median",geom="bar") +
  custom_style +
  coord_flip() +
  # Labels
  labs(title=miniature_title,subtitle="Median latency",x="Pattern",y="Latency") +
  # Axis
  scale_y_continuous(breaks=0:160000*25000,labels=format_latency)

latency_min_max <- successful %>%
  ggplot(aes(x=pattern,y=Latency)) +
  stat_summary(fun=mean,
               geom="pointrange",
               fun.min=min,
               fun.max=max) +
  facet_grid(type ~ .) +
  custom_style +
  coord_flip() +
  # Labels
  labs(title=miniature_title,subtitle="Min-Max latency",x="Pattern",y="Latency") +
  # Axis
  scale_y_continuous(breaks=0:160000*35000,labels=format_latency)

latency_std_dev <- successful %>%
  ggplot(aes(x=pattern,y=Latency)) +
  stat_summary(fun.data="mean_sdl",
               fun.args=list(
                 mult=1
               )) +
  facet_grid(type ~ .) +
  custom_style +
  coord_flip() +
  # Labels
  labs(title=miniature_title,subtitle="Standard Deviation",x="Pattern",y="Latency") +
  # Axis
  scale_y_continuous(breaks=0:160000*25000,labels=format_latency)

latency_std_errors <- successful %>%
  ggplot(aes(x=pattern,y=Latency)) +
  stat_summary(fun=mean,
               geom="pointrange",
               fun.max=function(x) mean(x) + sd(x) / sqrt(length(x)),
               fun.min=function(x) mean(x) - sd(x) / sqrt(length(x))) +
  facet_grid(type ~ .) +
  custom_style +
  coord_flip() +
  # Labels
  labs(title=miniature_title,subtitle="Standard Errors",x="Pattern",y="Latency") +
  # Axis
  scale_y_continuous(breaks=0:160000*25000,labels=format_latency)

latency_conf_interval <- successful %>%
  ggplot(aes(x=pattern,y=Latency)) +
  stat_summary(fun.data="mean_cl_normal",
               fun.args = list(
                 conf.int = .95
               )) +
  facet_grid(type ~ .) +
  custom_style +
  coord_flip() +
  # Labels
  labs(title=miniature_title,subtitle="Confidence Interval (95%)",x="Pattern",y="Latency") +
  # Axis
  scale_y_continuous(breaks=0:160000*25000,labels=format_latency)

latency_mean_conf_interv <- successful %>%
  ggplot(aes(x=pattern,y=Latency,fill=type)) +
  stat_summary(fun="mean",geom="bar",
               position=position_dodge(0.95)) +
  stat_summary(fun="mean",geom="point",
               position=position_dodge(0.95),
               size=1) +
  stat_summary(fun.data="mean_cl_normal",
               geom="errorbar",
               position=position_dodge(0.95),
               width=.2,
               fun.args = list(
                 conf.int = .95
               )) +
  custom_style +
  coord_flip() +
  # Labels
  labs(title=element_blank(),subtitle=element_blank(),x=element_blank(),y=element_blank()) +
  # Axis
  scale_y_continuous(breaks=0:200000*20000,labels=format_latency,limits=c(0,200000))
  #scale_x_discrete(labels = c("V1","V2","V3","V4","V5","V6"),
  #                 limits=c("Cache-2","Cache-3.2","Cache-3.1","Rate-Limiting-Proxy","Master-Worker","Producer-Consumer"))

# latency_mean <- dataset %>%
#   ggplot(aes(x=pattern,y=Latency,group=type,fill=type)) +
#   stat_summary(fun = "mean", geom = "area",
#                fill = "#EB5286",
#                alpha = .5) +
#   stat_summary(fun = "mean", geom = "point",
#                color = "#6F213F") +
#   custom_style

if(generate.miniatures){
  finalise_plot(plot_name = latency_mean,
                save_filepath = sprintf("%s/%s/summary_latency_mean.pdf", directory, dir),
                width_pixels = width,
                height_pixels = height)
  finalise_plot(plot_name = latency_median,
                save_filepath = sprintf("%s/%s/summary_latency_median.pdf", directory, dir),
                width_pixels = width,
                height_pixels = height)
  finalise_plot(plot_name = latency_std_dev,
                save_filepath = sprintf("%s/%s/summary_latency_std_dev.pdf", directory, dir),
                width_pixels = width,
                height_pixels = height)
  finalise_plot(plot_name = latency_std_errors,
                save_filepath = sprintf("%s/%s/summary_latency_std_errors.pdf", directory, dir),
                width_pixels = width,
                height_pixels = height)
  finalise_plot(plot_name = latency_conf_interval,
                save_filepath = sprintf("%s/%s/summary_latency_conf_interval.pdf", directory, dir),
                width_pixels = width,
                height_pixels = height)
  finalise_plot(plot_name = latency_mean_conf_interv,
                save_filepath = sprintf("%s/%s/summary_latency_mean_conf_interv.pdf", directory, dir),
                width_pixels = width,
                height_pixels = height)
}else{
  ggsave(
    sprintf("%s/%s/summary.pdf", directory, dir),
    arrangeGrob(top=textGrob(page_title,gp=gpar(fontsize=18,col="#063376",fontface="bold"),vjust=1),
                latency_mean,latency_median,latency_min_max,latency_std_dev,latency_std_errors,
                latency_conf_interval,latency_mean_conf_interv),
    device=pdf(),
    unit="in",
    width=8.5,
    height=11,
    dpi = 300,
  )
}

