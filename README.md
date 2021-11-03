## Overview

`JBuildFS` is a library for implementing [key-value
stores](https://en.wikipedia.org/wiki/Key%E2%80%93value_database) that
supports specific features for building compilers.  For example,
content might be stored on disk or in an archive file and might also
be _versioned_ (or not).  Roughly speaking a content _source_ provides
a way to read content, whilst a _sink_ provides a way of writing
content.  Furthermore, content can be stored in journal-like
structures called _ledgers_.

### Content

All managed content implements the `Content` interface, and is
associated with an instance of `Content.Type`.  In particular, given
an `InputStream` and a `Content.Type` one can attempt to instantiate
an instance of `Content` (though this might fail in the `InputStream`
is corrupted, etc).  The following illustrates a minimal example:

```Java
class Point implements Content {
  public static Content.Type<Point> ContentType = new Content.Type<Point>() {

    @Override
    public String getSuffix() {
      return "pt";
    }

    @Override
    public Point read(Trie id, InputStream input, Registry registry) throws IOException {
      try (ObjectInputStream ois = new ObjectInputStream(input)) {
        return new Point(ois.readInt(), ois.readInt());
      }
    }

    @Override
    public void write(OutputStream output, Point value) throws IOException {
      try (ObjectOutputStream ois = new ObjectOutputStream(output)) {
        ois.writeInt(value.x);
        ois.writeInt(value.y);
      }
    }

  };

  public final int x;
  public final int y;

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public Type<?> getContentType() {
    return ContentType;
  }
}
```

This defines a class of structured content, `Point`, which is
associated with `Point.ContentType`.  Through this instance of
`Content.Type` we can serialise / deserialise our structure content
(e.g. read it from disk, or write it back).  

### Sources

### Sinks
