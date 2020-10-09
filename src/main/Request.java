package main;

public class Request {
  
  private int success;
  
  private int fail;
  
  public Request() {
    success = 0;
    fail = 0;
  }
  
  public Request(int success, int fail) {
    this.success = success;
    this.fail = fail;
  }
  
  public void incrementSuccess(int numRequest) {
    success += numRequest;
  }
  
  public void incrementFail(int numRequest) {
    fail += numRequest;
  }

  public int getSuccess() {
    return success;
  }

  public void setSuccess(int success) {
    this.success = success;
  }

  public int getFail() {
    return fail;
  }

  public void setFail(int fail) {
    this.fail = fail;
  }

  @Override
  public String toString() {
//    return "Request [success=" + success + ", fail=" + fail + "]";
    return "Request [success=" + success + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + fail;
    result = prime * result + success;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Request other = (Request) obj;
    if (fail != other.fail)
      return false;
    if (success != other.success)
      return false;
    return true;
  }
}
