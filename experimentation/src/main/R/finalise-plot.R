save_plot <- function (plot_grid, width, height, save_filepath) {
  grid::grid.draw(plot_grid)
  #save it
  ggplot2::ggsave(filename = save_filepath,
                  plot=plot_grid, width=(width/72), height=(height/72),  bg="white")
}

#Left align text
left_align <- function(plot_name, pieces){
  grob <- ggplot2::ggplotGrob(plot_name)
  n <- length(pieces)
  grob$layout$l[grob$layout$name %in% pieces] <- 2
  return(grob)
}

#' Arrange alignment and save BBC ggplot chart
#'
#' Running this function will save your plot with the correct guidelines for publication for a BBC News graphic.
#' It will left align your title, and subtitle, add the BBC blocks at the bottom right and save it to your specified location.
#' @param plot_name The variable name of the plot you have created that you want to format and save
#' @param save_filepath Exact filepath that you want the plot to be saved to
#' @param width_pixels Width in pixels that you want to save your chart to - defaults to 640
#' @param height_pixels Height in pixels that you want to save your chart to - defaults to 450
#' @return (Invisibly) an updated ggplot object.

#' @keywords finalise_plot
#' @examples
#' finalise_plot(plot_name = myplot,
#' save_filepath = "filename_that_my_plot_should_be_saved_to-nc.png",
#' width_pixels = 640,
#' height_pixels = 450
#' )
#'
#' @export
finalise_plot <- function(plot_name,
                          save_filepath=file.path(Sys.getenv("TMPDIR"), "tmp-nc.png"),
                          width_pixels=640,
                          height_pixels=450) {

  #Draw your left-aligned grid
  plot_left_aligned <- plot_name # left_align(plot_name, c("subtitle", "title", "caption"))
  plot_grid <- ggpubr::ggarrange(plot_left_aligned, ncol = 1, nrow = 1,
                                 heights = c(1, 0.045/(height_pixels/450)))
  ## print(paste("Saving to", save_filepath))
  save_plot(plot_grid, width_pixels, height_pixels, save_filepath)
  ## Return (invisibly) a copy of the graph. Can be assigned to a
  ## variable or silently ignored.
  invisible(plot_grid)
}
