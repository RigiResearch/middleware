#!/usr/bin/env Rscript
args = commandArgs(trailingOnly=TRUE)

if (length(args)!=3) {
  stop("Three arguments were expected: the root dir, the execution scenario and the architecture variant",
       call.=FALSE)
}

#if (!require("devtools")) install.packages("devtools")
#install.packages("lubridate")
#install.packages("dunn.test")
#install.packages("dplyr") # %>% and group_by
#install.packages("DescTools")
#install.packages("FSA")
#install.packages("PMCMRplus")
#install.packages("qqplotr")
#install.packages("forcats")
#install.packages(tidyverse)

library("lubridate")
library("dunn.test")
library("dplyr")
library("DescTools")
library("FSA")
library("PMCMRplus")
library("ggplot2")
library("qqplotr")
library("forcats")
library("tidyverse")

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

# Read the CSV file
root = args[1] # e.g., /Users/miguel/Desktop/deployments
traffic_type = args[2] # e.g., constant
variant = args[3] # e.g., proxy-cache-3.1
directory = sprintf("%s/%s-scenario-%s", root, traffic_type, variant)
msrmts = read.csv(sprintf("%s/all.csv", directory))

# Alter columns
msrmts$pattern <- factor(msrmts$pattern)
msrmts$start <- ymd_hms(as_datetime(msrmts$timeStamp/1000))

# Subtract the first (date)time to represent the elapsed time from 00:00:00
first = msrmts$start[1]
hour(msrmts$start) <- hour(msrmts$start)-hour(first)
minute(msrmts$start) <- minute(msrmts$start)-minute(first)
second(msrmts$start) <- second(msrmts$start)-second(first)

msrmts$end <- msrmts$start + as.duration(msrmts$Latency/1000)
msrmts$success <- factor(with(msrmts,ifelse(grepl("true",msrmts$success), "success", "error")))
msrmts$type <- factor(with(msrmts,ifelse(grepl("playlist",msrmts$URL), "playlist", "video")))

# Filtered data by request type
videos <- msrmts %>% filter(type=="video")
playlists <- msrmts %>% filter(type=="playlist")

compute_statistics <- function(dataset, root_dir){
  # Separate successful and erroneous requests
  successful <- dataset[grep("success",dataset$success),]
  erroneous <- dataset[grep("error",dataset$success),]
  succ_info <- successful %>% select(pattern, Latency) %>%
    count(pattern)
  erro_info <- erroneous %>% select(pattern, Latency) %>%
    count(pattern)
  print("Successful requests:")
  print(succ_info)
  print("Erroneous requests:")
  print(erro_info)

  # Test normality
  # http://www.psychwiki.com/wiki/How_do_I_determine_whether_my_data_are_normal%3F (method 3)
  print("-> Summary")
  summ <- summary(successful$Latency)
  print(summ)

  print("-> Normality test: ks")
  ks.test(successful$Latency, "pnorm", mean=mean(successful$Latency), sd=sd(successful$Latency), exact=TRUE)

  # Another test for normality and others
  # https://stat-methods.com/home/kruskal-wallis-r/

  #Produce descriptive statistics by group
  print("-> Descriptive statistics")
  descript<- successful %>% mutate(Latency = Latency/1000) %>% # Convert milliseconds to seconds
    select(pattern, Latency) %>% group_by(pattern) %>% 
    summarise(n=n(), 
              mean=mean(Latency, na.rm = TRUE), 
              sd=sd(Latency, na.rm = TRUE),
              stderr=sd/sqrt(n),
              LCL = mean - qt(1 - (0.05 / 2), n - 1) * stderr,
              UCL = mean + qt(1 - (0.05 / 2), n - 1) * stderr,
              median=median(Latency, na.rm = TRUE),
              min=min(Latency, na.rm = TRUE), 
              max=max(Latency, na.rm = TRUE),
              IQR=IQR(Latency, na.rm = TRUE),
              LCLmed = MedianCI(Latency, na.rm=TRUE)[2],
              UCLmed = MedianCI(Latency, na.rm=TRUE)[3])
  print(descript)
  
  # Test for significant difference - Shapiro-Wilk
  print("-> Test for significant difference - Shapiro-Wilk")
  shapir <- successful %>%
    group_by(pattern) %>%
    summarise(W = shapiro.test(Latency)$statistic,
              p.value = shapiro.test(Latency)$p.value)
  print(shapir)
  
  #Perform the Kruskal-Wallis test
  print("-> Kruskal-Wallis")
  m1<-kruskal.test(Latency ~ pattern, data=successful)
  print(m1)

  # Read more: https://stats.stackexchange.com/a/71491
  attach(successful)
  print("-> Dunn test (Hochberg)")
  posthocs2<- dunn.test(Latency, pattern, method="hochberg", list=TRUE)
  print(posthocs2$comparisons)

  #
  # TODO Add this to the other R program
  #
  # Set the default ggplot2 theme
  # theme_set(bbc_style())

  base_size = 11
  half_line <- base_size/2
  custom_style <- theme_minimal() + #bbc_style() +
    theme(legend.position="none") +
    theme(panel.grid.major.y=element_line(color="#eeeeee")) +
    theme(plot.title=element_text(size=18,color="#063376",hjust=0)) +
    theme(plot.subtitle=element_text(size=12,margin=margin(6, 0, 8, 0),hjust=0)) +
    theme(legend.text=element_text(size=12)) +
    theme(axis.text=element_text(size=11)) +
    theme(axis.text.x=element_text(size=11,color="#666666",
                                   margin=margin(b=2*half_line/2),vjust=1.3)) +
    theme(axis.text.y=element_text(margin=margin(r=1.1*half_line/2),hjust=1.3))

  # Helper functions/vars
  format_latency = function(num) sprintf("%.0fs", num/1000)
  breaks_for = function(max){
    if(max < 10000){
      latency_breaks <- 0:160000*1000
    } else {
      latency_breaks = 0:160000*15000
    }
    return(latency_breaks)
  }
  latency_breaks <- breaks_for(max(successful$Latency))

  #Produce Boxplots and visually check for outliers
  boxplot <- successful %>%
    ggplot(aes(x = pattern, y = Latency, fill = pattern)) +
    stat_boxplot(geom ="errorbar", width = 0.5) +
    geom_boxplot(fill="#FF7F0E") + 
    stat_summary(fun=mean, geom="point", shape=10, size=3, color="black") + 
    labs(x = "Variants", y = "Latency") +
    scale_y_continuous(breaks=latency_breaks,labels=format_latency) +
    theme(legend.position="none") +
    custom_style

  #Perform QQ plots by group
  qqplot <- successful %>%
    ggplot(mapping = aes(sample = Latency, color = pattern, fill = pattern)) +
    stat_qq_band(alpha=0.5, conf=0.95, bandType = "pointwise") +
    stat_qq_line() +
    stat_qq_point(col="black") +
    facet_wrap(~ pattern, scales = "free") +
    labs(x = "Theoretical Quantiles", y = "Sample Quantiles") +
    custom_style

  finalise_plot(plot_name = boxplot,
                save_filepath = sprintf("%s/charts/boxplot_%s_%s.pdf",
                                        root_dir, first(successful$type), traffic_type),
                width_pixels = 1400,
                height_pixels = 500)
  finalise_plot(plot_name = qqplot,
                save_filepath = sprintf("%s/charts/qqplot_%s_%s.pdf",
                                        root_dir, first(successful$type), traffic_type),
                width_pixels = 1400,
                height_pixels = 1000)
}

compute_statistics(videos, directory)
compute_statistics(playlists, directory)
