package com.opengamma.strata.basics.index;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.named.ExtendedEnum;
import org.junit.jupiter.api.Test;

public class FxIndicesTest {

  @Test
  public void test() {
    final ExtendedEnum<FxIndex> enumLookup = FxIndices.ENUM_LOOKUP;
    final ImmutableMap<String, FxIndex> stringFxIndexImmutableMap = enumLookup.lookupAll();
    stringFxIndexImmutableMap.forEach((s, i) -> System.out.println(s + "--> " + i));
  }
}
