package com.dremio.support.diagnostics.stress;

public class Sequence {
  private String name;
  private int start;
  private int end;
  private int step;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public int getStep() {
    return step;
  }

  public void setStep(int step) {
    this.step = step;
  }
}
