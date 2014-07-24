package com.revolsys.gis.parallel;

import java.util.Map;

import com.revolsys.data.record.Record;
import com.revolsys.data.types.DataType;
import com.revolsys.data.validator.AttributeValueValidator;
import com.revolsys.data.validator.RecordValidator;
import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.process.BaseInOutProcess;

public class RecordValidationProcess extends
  BaseInOutProcess<Record, Record> {
  private final RecordValidator validator = new RecordValidator();

  @Override
  protected void process(final Channel<Record> in,
    final Channel<Record> out, final Record object) {
    validator.isValid(object);
    out.write(object);
  }

  public void setValidators(
    final Map<DataType, AttributeValueValidator> validators) {
    validator.addValidators(validators);
  }
}