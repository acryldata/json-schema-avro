package com.github.fge.avro;

import java.io.IOException;
import org.testng.annotations.Test;


public final class NullDefaultTest
    extends AvroTranslationsTest
{
  public NullDefaultTest()
      throws IOException
  {
    super("nullDefault");
  }


  @Test
  public void noOpTest() {
  }
}
