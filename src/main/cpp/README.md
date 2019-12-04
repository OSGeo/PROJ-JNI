DEFINITIONS OF TERMS
--------------------
For all files in this directory, _function_ is a C/C++ function (either PROJ or JNI)
and _method_ is a Java method, including the ones implemented in this directory.


NOTE ON CHARACTER ENCODING
--------------------------
This implementation assumes that the PROJ library expects strings encoded in UTF-8,
regardless the platform encoding. Consequently we use the JNI `StringUTF` functions
directly. It is not completely appropriate because JNI functions use modified UTF-8,
but it should be okay if the strings do not use the null character (0) or the
supplementary characters (the ones encoded on 4 bytes in a UTF-8 string).
