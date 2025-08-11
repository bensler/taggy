package com.bensler.taggy.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;


class TagTest {

  @Test
  void setTest() {
    final Tag tag1 = new Tag(1, null, "bla", Map.of(), Set.of());
    final Tag tag2 = new Tag(1, null, "blub", Map.of(), Set.of());
    final Set<Tag> tags = new HashSet<>();

    tags.add(tag1);
    tags.add(tag2);

    assertEquals(1, tags.size());
  }

  @Test
  void mapTest() {
    final Tag tag1 = new Tag(1, null, "bla", Map.of(), Set.of());
    final Tag tag2 = new Tag(1, null, "blub", Map.of(), Set.of());
    final String value = "value";
    final Map<Tag, String> tagsMap = new HashMap<>();

    tagsMap.put(tag1, value);

    assertEquals(value, tagsMap.get(tag2));
  }

}
