package spice86.main;

import spice86.ui.Spice86Application;

/**
 * This is the entry point that should be used.
 */
public class Main {
  public static void main(String[] args) {
    // Javafx entry point needs to be in another package to avoid error when running a fat jar
    Spice86Application.main(args);
  }
}