package com.dremio.support.diagnostics.stress;

/** Represents a sequence configuration used for generating query parameters. */
public class Sequence {
  /** The name of the sequence. */
  private String name;
  /** The starting value of the sequence. */
  private int start;
  /** The ending value of the sequence. */
  private int end;
  /** The step increment for the sequence. */
  private int step;

  /**
   * Gets the name of the sequence.
   *
   * @return the sequence name.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the sequence.
   *
   * @param name the sequence name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the starting value of the sequence.
   *
   * @return the start value.
   */
  public int getStart() {
    return start;
  }

  /**
   * Sets the starting value of the sequence.
   *
   * @param start the start value.
   */
  public void setStart(int start) {
    this.start = start;
  }

  /**
   * Gets the ending value of the sequence.
   *
   * @return the end value.
   */
  public int getEnd() {
    return end;
  }

  /**
   * Sets the ending value of the sequence.
   *
   * @param end the end value.
   */
  public void setEnd(int end) {
    this.end = end;
  }

  /**
   * Gets the step increment for the sequence.
   *
   * @return the step value.
   */
  public int getStep() {
    return step;
  }

  /**
   * Sets the step increment for the sequence.
   *
   * @param step the step value.
   */
  public void setStep(int step) {
    this.step = step;
  }
}
