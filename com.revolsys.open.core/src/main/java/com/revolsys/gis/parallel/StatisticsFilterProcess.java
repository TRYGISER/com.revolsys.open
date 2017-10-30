package com.revolsys.gis.parallel;

import com.revolsys.parallel.process.FilterProcess;
import com.revolsys.record.Record;
import com.revolsys.util.count.LabelCountMap;

public class StatisticsFilterProcess extends FilterProcess<Record> {

  private LabelCountMap acceptStatistics;

  private LabelCountMap rejectStatistics;

  @Override
  protected void destroy() {
    if (this.acceptStatistics != null) {
      this.acceptStatistics.disconnect();
    }
    if (this.rejectStatistics != null) {
      this.rejectStatistics.disconnect();
    }
  }

  public LabelCountMap getAcceptStatistics() {
    return this.acceptStatistics;
  }

  public LabelCountMap getRejectStatistics() {
    return this.rejectStatistics;
  }

  @Override
  protected void init() {
    super.init();
    if (this.acceptStatistics != null) {
      this.acceptStatistics.connect();
    }
    if (this.rejectStatistics != null) {
      this.rejectStatistics.connect();
    }
  }

  @Override
  protected void postAccept(final Record object) {
    if (this.acceptStatistics != null) {
      this.acceptStatistics.addCount(object);
    }
  }

  @Override
  protected void postReject(final Record object) {
    if (this.rejectStatistics != null) {
      this.rejectStatistics.addCount(object);
    }
  }

  public void setAcceptStatistics(final LabelCountMap acceptStatistics) {
    this.acceptStatistics = acceptStatistics;
  }

  public void setRejectStatistics(final LabelCountMap rejectStatistics) {
    this.rejectStatistics = rejectStatistics;
  }

}
