## Overview

`JBuildFS` is a library for implementing [key-value
stores](https://en.wikipedia.org/wiki/Key%E2%80%93value_database) that
supports specific features for building compilers.  For example,
content might be stored on disk or in an archive file and might also
be _versioned_ (or not).  Roughly speaking a content _source_ provides
a way to read content, whilst a _sink_ provides a way of writing
content.  Furthermore, content can be stored in journal-like
structures called _ledgers_.
